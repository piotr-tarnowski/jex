package com.devontrain.experimental;

import java.util.concurrent.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 04.05.16.
 */
public class Q extends ThreadPoolExecutor{

    public Q(){
        super(Runtime.getRuntime().availableProcessors(),Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public <T> Deferred<T> defer(){
        return new DeferredImpl<>();
    }

    private class DeferredImpl<T> implements Deferred<T>{

        private final PromiseImpl<T> promise = new PromiseImpl<T>();

        @Override
        public PromiseImpl<T> getPromise() {
            return promise;
        }

        @Override
        public void resolve(T result) {
            promise.set( result );
        }

        @Override
        public void resolve(Callable<T> callable) {
            promise.set(callable);
            execute( promise.asFuture() );
        }
    }
}
