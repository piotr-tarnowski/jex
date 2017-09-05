package com.devontrain.experimental;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 29.04.16.
 */
public class KeyHungryPrioritizedFairPool<K, C> extends ThreadPoolExecutor {

    private final Map<K, TaskListDef> callables = new ConcurrentHashMap<>();

    private final Function<K, C> ctxCreator;

    private volatile int reservedThreads = 0;

//    public KeyHungryPrioritizedFairPool() {
//        this(Runtime.getRuntime().availableProcessors());
//    }


    public KeyHungryPrioritizedFairPool(int threadNumber, Function<K, C> ctxCreator) {
//        super(threadNumber, threadNumber, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        super(threadNumber, threadNumber, 60L, TimeUnit.SECONDS, new LazyBlockingQueueDelegate<>(new LinkedBlockingDeque<>()));
        @SuppressWarnings("unchecked")
        LazyBlockingQueueDelegate<Runnable> queue = (LazyBlockingQueueDelegate) getQueue();
        queue.executor = this;
        this.ctxCreator = ctxCreator;
    }


//
//    public KeyHungryPrioritizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
//        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
//    }
//
//    public KeyHungryPrioritizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
//        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
//    }
//
//    public KeyHungryPrioritizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
//        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
//    }
//
//    public KeyHungryPrioritizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
//        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
//    }

    //TODO: maxSize to thread pool correlation as builder
    public void registerTaskCollectionSize(K key, int maxSize) {
        callables.put(key, new TaskListDef(key, maxSize));
    }

    private class TaskListDef<T> {

        final int maxSize;
        final AtomicInteger inRun = new AtomicInteger(0);
        final ConcurrentLinkedQueue<ChainedFutureTask> tasks;
        final K key;

        private TaskListDef(K key, int maxSize) {
            this.maxSize = maxSize;
            this.tasks = new ConcurrentLinkedQueue<>();
            this.key = key;

            //for blocking policy for non blocking available processor and fixed size would be enough.
            synchronized (KeyHungryPrioritizedFairPool.this) {
                int delta = reservedThreads + maxSize - getMaximumPoolSize();
                if (delta > 0) {
                    setMaximumPoolSize(getMaximumPoolSize() + delta);
                }
            }
        }

        private void executeTask() {
            C ctx = ctxCreator.apply(key);
            executeTask(ctx);
        }

        private void executeTask(C ctx) {
            if (tasks.size() > 0) {
                int current;
                while ((current = inRun.get()) < maxSize) {
                    if (inRun.compareAndSet(current, ++current)) {
                        execute(() -> {
                            TaskListDef<T>.ChainedFutureTask next = tasks.poll();
                            if (next != null) {
                                next.run(ctx);
                            }
                            inRun.decrementAndGet();
                            executeTask(ctx);
                        });
                    }
                }
            }
        }

        private class ChainedFutureTask extends CompletableFuture<T> {
            private final Function<C, T> function;

            ChainedFutureTask(Function<C, T> function) {
                this.function = function;
                tasks.offer(this);
                executeTask();
            }

            public void run(C ctx) {
                T t = function.apply(ctx);
                complete(t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(final K key, final Function<C, T> callable) {
        TaskListDef<T> taskListDef = callables.computeIfAbsent(key, k -> new TaskListDef<T>(key, 1));
        return taskListDef.new ChainedFutureTask(callable);
    }

}
