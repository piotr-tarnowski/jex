package com.devontrain.jex.executors;


import com.devontrain.jex.executors.tasks.RunnableTask;

import java.util.concurrent.CompletableFuture;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 08.08.17.
 */
@SuppressWarnings("unchecked")
public interface BasicSynchronizer {

    default CompletableFuture<?> runInSync(Runnable runnable) {
        Context ctx = (Context) this;
        return ctx.runInSync(new RunnableTask<>(runnable, Boolean.TRUE));
    }

    default <T> CompletableFuture<T> joinInSync(CompletableFuture<T> future, int timeout) {
        Context ctx = (Context) this;
        return ctx.joinInSync(future, timeout);
    }
}
