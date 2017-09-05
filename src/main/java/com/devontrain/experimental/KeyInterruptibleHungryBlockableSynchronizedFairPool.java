package com.devontrain.experimental;

import com.devontrain.jex.common.Holder;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 29.04.16.
 */
public class KeyInterruptibleHungryBlockableSynchronizedFairPool<K, C> extends ThreadPoolExecutor {

    private final Map<K, ChainedFutureTask> callables = new ConcurrentHashMap<>();
    private final int threadNumber;

    private final BiFunction<K, C, Boolean> interrupt;
    private final Function<K, C> ctxCreator;

//    public KeyInterruptibleHungrySynchronizedFairPool() {
//        this(Runtime.getRuntime().availableProcessors());
//    }

    public KeyInterruptibleHungryBlockableSynchronizedFairPool(int threadNumber, final Function<K, C> ctxCreator, final BiFunction<K, C, Boolean> interrupt) {
        super(threadNumber, threadNumber, 0L, TimeUnit.MILLISECONDS, new LazyBlockingQueueDelegate<>(new LinkedBlockingQueue<>()));
        this.interrupt = interrupt;
        this.threadNumber = Math.max(Runtime.getRuntime().availableProcessors(), threadNumber);
        this.ctxCreator = ctxCreator;
        LazyBlockingQueueDelegate queue = (LazyBlockingQueueDelegate) getQueue();
        queue.executor = this;
    }

    private class RootChainedFutureTask<T> extends ChainedFutureTask<T> {

        final K key;
        C ctx;

        RootChainedFutureTask(K key, final Function<C, T> callable) {
            super(callable);
            this.key = key;
        }

        @Override
        RootChainedFutureTask getRoot() {
            return this;
        }

        @Override
        public void run() {
            ctx = ctxCreator.apply(key);
            super.run();
        }
    }

    private class ChainedFutureTask<T> extends CompletableFuture<T> implements Runnable {

        volatile ChainedFutureTask<T> next;
        final RootChainedFutureTask<T> root;
        final Function<C, T> callable;

        ChainedFutureTask(final Function<C, T> callable) {
            this.callable = callable;
            this.root = getRoot();
        }

        ChainedFutureTask(ChainedFutureTask<T> parent, final Function<C, T> callable) {
            this.callable = callable;
            parent.next = this;
            this.root = parent.root;
        }

        RootChainedFutureTask<T> getRoot() {
            return this.root;
        }

        @Override
        public void run() {
            complete(callable.apply(root.ctx));
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(final K key, final Function<C, T> callable) {
        return callables.compute(key, (k, task) -> {
            if (task == null) {
                task = new RootChainedFutureTask<>(key, callable);
                alignPollSize();
                executeTask(task);
            } else {
                task = new ChainedFutureTask<>(task, callable);
            }
            return task;
        });
    }

    private <T> void executeTask(@Nonnull final ChainedFutureTask<T> t) {
        execute(() -> {
            ChainedFutureTask<T> prev = null;
            ChainedFutureTask<T> next = t;
            while (next != null) {
                next.run();
                prev = next;
                Holder<ChainedFutureTask<T>> holder = new Holder<>();
                final ChainedFutureTask<T> task = prev;
                callables.compute(t.root.key, (k, current) -> {
                    ChainedFutureTask<T> upcoming = task.next;
                    holder.accept(upcoming);
                    task.next = null;
                    if (upcoming == null) {
                        if (task == current) {
                            alignPollSize();
                            return null;
                        } else if (current != null) {
                            holder.accept(current.getRoot());
                        }
                    }
                    return current;
                });
                next = holder.get();
            }
        });
    }

    private synchronized void alignPollSize() {
        setMaximumPoolSize(Math.max(callables.size(), threadNumber));
    }
}
