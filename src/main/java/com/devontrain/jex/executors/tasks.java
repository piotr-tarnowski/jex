package com.devontrain.jex.executors;

import com.devontrain.jex.common.Holder;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 31.07.17.
 */
final class tasks {

    private tasks() {
    }

    interface TriConsumer<T, U, W> {
        void accept(T t,
                    U u,
                    W w);
    }

    static class EmptyTask<T> extends CompletableTask<T> {

        private final T defaultValue;

        EmptyTask(T defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        T apply(Object ctx) throws Exception {
            return defaultValue;
        }
    }

    static class RunnableTask<T> extends EmptyTask<T> {

        private final Runnable runnable;

        RunnableTask(Runnable runnable,
                     T defaultValue) {
            super(defaultValue);
            this.runnable = runnable;
        }

        @Override
        T apply(Object ctx) throws Exception {
            runnable.run();
            return super.apply(ctx);
        }
    }

    static class CallableTask<T> extends CompletableTask<T> {

        private final Callable<T> callable;

        CallableTask(Callable<T> callable) {
            this.callable = callable;
        }

        @Override
        T apply(Object ctx) throws Exception {
            return callable.call();
        }
    }

    static class ContextCallableTask<T> extends CompletableTask<T> {

        final Function callable;

        ContextCallableTask(Function callable) {
            this.callable = callable;
        }

        @Override
        @SuppressWarnings("unchecked")
        T apply(Object ctx) {
            return (T) callable.apply(ctx);
        }
    }

    static class FutureTask extends CompletableTask<Object> {

        private final Consumer consumer;

        FutureTask(Consumer consumer) {
            this.consumer = consumer;
        }

        @Override
        @SuppressWarnings("unchecked")
        Object apply(Object ctx) {
            consumer.accept(ctx);
            return Boolean.TRUE;
        }
    }

    abstract static class CompletableTask<T> extends CompletableFuture<T> implements Consumer, Runnable {

        @SuppressWarnings("unchecked")
        final <K, C extends Context<K>> void run(ExecutorBase<K, C> executor,
                                                 C context) {
            executor.run(context, new Holder<>(this));
        }

        @Override
        public void accept(Object ctx) {
            try {
                complete(apply(ctx));
            } catch (Exception e) {
                completeExceptionally(e);
            }
        }

        @Override
        public void run() {
            accept(null);
        }

        abstract T apply(Object ctx) throws Exception;
    }
}
