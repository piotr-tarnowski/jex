package com.devontrain.experimental;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 29.04.16.
 */
public class KeyPrioritizedFairPool<K, C> extends ThreadPoolExecutor {

    private final Map<K, TaskListDef> callables = new ConcurrentHashMap<>();

    public KeyPrioritizedFairPool() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public KeyPrioritizedFairPool(int threadNumber) {
        super(threadNumber, threadNumber, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public KeyPrioritizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public KeyPrioritizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public KeyPrioritizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public KeyPrioritizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    //TODO: maxSize to thread pool corelation as builder
    public void registerTaskCollectionSize(K key, int maxSize) {
        callables.put(key, new TaskListDef(maxSize));
    }

    private class TaskListDef {

        final int maxSize;
        final AtomicInteger inRun = new AtomicInteger(0);
        final Queue<ChainedFutureTask> tasks;

        private TaskListDef(int maxSize) {
            this.maxSize = maxSize + 1;
            this.tasks = new ConcurrentLinkedQueue<>();
        }

        class ChainedFutureTask<T> extends FutureTask<T> {

            ChainedFutureTask(Callable<T> callable) {
                super(callable);
            }

            ChainedFutureTask(Runnable runnable, T result) {
                super(runnable, result);
            }

            @Override
            protected void set(T t) {
                super.set(t);
                execute(tasks.poll());
            }
        }
    }


    @Override
    public void execute(Runnable command) {
        if (command == null) {
            return;
        }
        super.execute(command);
    }

    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(final K key, final Function<C, T> callable) {
        TaskListDef taskListDef = callables.computeIfAbsent(key, k -> new TaskListDef(1));
        TaskListDef.ChainedFutureTask<T> task = taskListDef.new ChainedFutureTask<>(() -> callable.apply(null));
        taskListDef.tasks.offer(task);

        if (taskListDef.inRun.incrementAndGet() < taskListDef.maxSize) {
            execute(() -> {
                try {
                    TaskListDef.ChainedFutureTask<T> next = taskListDef.tasks.poll();
                    if (next == null) {
                        return;
                    }
                    next.run();
                } finally {
                    taskListDef.inRun.decrementAndGet();
                }
            });
        } else {
            taskListDef.inRun.decrementAndGet();
        }
        return task;
    }

}
