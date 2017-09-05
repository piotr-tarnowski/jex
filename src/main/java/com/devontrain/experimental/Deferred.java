package com.devontrain.experimental;

import java.util.concurrent.Callable;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 04.05.16.
 */
public interface Deferred<T> {
    PromiseImpl<T> getPromise();

    void resolve(T result);

    void resolve(Callable<T> callable);
}
