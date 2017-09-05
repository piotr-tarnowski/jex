package com.devontrain.experimental;

import java.util.concurrent.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 04.05.16.
 */
class PromiseImpl<T> extends PromiseBase<T> {


    private RunnableFuture<T> future;

    public void set(T t) {
        future = new FutureResulted<>(t);
    }

    public void set(Callable<T> callable) {
        future = new FutureTask<>(() -> {
            T v = callable.call();
            function.apply(new FutureResulted<>(v));
            return v;
        });
    }

    public RunnableFuture<T> asFuture() {
        return future;
    }

    public static class FutureResulted<V> implements RunnableFuture<V> {

        private final V result;

        FutureResulted(V result) {
            this.result = result;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return result;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return result;
        }

        @Override
        public void run() {

        }
    }

}
