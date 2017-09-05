package com.devontrain.experimental;

import com.devontrain.jex.common.Holder;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 29.04.16.
 */
public class KeyInterruptibleHungrySynchronizedFairPool<K, C> extends ThreadPoolExecutor {

    private final Map<K, ChainedFutureTask> callables = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ChainedFutureTask> tasks = new ConcurrentLinkedQueue<>();
    private final int threadNumber;

    private final BiFunction<K, C, Boolean> interrupt;
    private final Function<K, C> ctxCreator;

//    public KeyInterruptibleHungrySynchronizedFairPool() {
//        this(Runtime.getRuntime().availableProcessors());
//    }

    public KeyInterruptibleHungrySynchronizedFairPool(int threadNumber, final Function<K, C> ctxCreator, final BiFunction<K, C, Boolean> interrupt) {
        super(threadNumber, threadNumber, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.interrupt = interrupt;
        this.threadNumber = threadNumber;
        this.ctxCreator = ctxCreator;
    }

    private class RootChainedFutureTask<T> extends ChainedFutureTask<T> {

        final K key;
        Function<C, T> callable;
        C ctx;

        RootChainedFutureTask(K key, Function<C, T> callable) {
            this.key = key;
            this.callable = callable;
        }

        @Override
        RootChainedFutureTask<T> getRoot() {
            return this;
        }

        @Override
        public void run() {
            ctx = ctxCreator.apply(key);
            run(callable);
            callable = null;
        }
    }

    private class ChildFutureTask<T> extends ChainedFutureTask<T> {
        final Function<C, T> callable;

        private ChildFutureTask(ChainedFutureTask<T> parent, Function<C, T> callable) {
            super(parent);
            this.callable = callable;
        }

        @Override
        public void run() {
            run(callable);
        }
    }

    private abstract class ChainedFutureTask<T> extends CompletableFuture<T> implements Runnable {

        volatile ChainedFutureTask<T> next;
        final RootChainedFutureTask<T> root;

        ChainedFutureTask() {
            this.root = getRoot();
        }

        ChainedFutureTask(ChainedFutureTask<T> parent) {
            this.root = parent.root;
            parent.next = this;
        }

        RootChainedFutureTask<T> getRoot() {
            return this.root;
        }

        void run(Function<C, T> callable) {
            complete(callable.apply(root.ctx));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(final K key, final Function<C, T> callable) {
        return callables.compute(key, (k, task) -> {
            if (task == null) {
                task = new RootChainedFutureTask<>(key, callable);
                if (callables.size() < threadNumber) {
                    executeTask(task);
                } else {
                    tasks.add(task);
                }
            } else {
                task = new ChildFutureTask<>(task, callable);
            }
            return task;
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void executeTask(@Nonnull ChainedFutureTask<T> t) {
        execute(() -> {
            ChainedFutureTask<T> next = t;
            while (next != null) {
                next.run();
                ChainedFutureTask<T> task = next;
                Holder<ChainedFutureTask<T>> holder = new Holder<>();
                callables.compute(t.root.key, (k, current) -> {
                    ChainedFutureTask<T> upcoming = task.next;
                    holder.accept(upcoming);
                    task.next = null;
                    if (upcoming == null) {
                        if (task == current) {
                            ChainedFutureTask<T> futureTask = tasks.poll();
                            holder.accept(futureTask);//executeTask(futureTask);
                            return null;
                        } else if (current != null) {
                            holder.accept(current.getRoot());//executeTask(current.getRoot());
                        }
                    }
                    return current;
                });
                next = holder.get();
            }
        });
    }

}
