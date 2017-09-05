package com.devontrain.experimental;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 29.04.16.
 *
 */
public class KeySynchronizedFairPool<K> extends ThreadPoolExecutor {

    private final Map<K, ChainedFutureTask> callables = Collections.synchronizedMap(new HashMap<>());

    public KeySynchronizedFairPool(){
        this(Runtime.getRuntime().availableProcessors());
    }

    public KeySynchronizedFairPool(int threadNumber) {
        super(threadNumber,threadNumber,0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public KeySynchronizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public KeySynchronizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public KeySynchronizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public KeySynchronizedFairPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    private class ChainedFutureTask<T> extends FutureTask<T> {

        K key;
        ChainedFutureTask next;

        ChainedFutureTask(Callable<T> callable) {
            super(callable);
        }

        ChainedFutureTask(Runnable runnable, T result) {
            super(runnable, result);
        }


        @Override
        protected void done() {
            synchronized ( callables ){
                if(next == null) {
                    ChainedFutureTask task = callables.get(key);
                    if(task == this) {
                        callables.remove(key);
                    }
                }else{
                    execute(next);
                    next = null;
                }
                key = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(final K key, final Callable<T> callable) {
        ChainedFutureTask task;
        synchronized (callables) {
            task = callables.get(key);
            if (task == null) {
                task = newTaskFor(callable);
                task.key = key;
                execute(task);
            } else {
                task = task.next = newTaskFor(callable);
                task.key = key;
            }
            callables.put(key, task);
        }
        return task;
    }

    @Override
    protected <T> ChainedFutureTask newTaskFor(Callable<T> callable) {
        return new ChainedFutureTask<>(callable);
    }

    @Override
    protected <T> ChainedFutureTask newTaskFor(Runnable runnable, T value) {
        return new ChainedFutureTask<>(runnable, value);
    }
}
