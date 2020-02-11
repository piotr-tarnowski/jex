package com.devontrain.jex.executors;

import com.devontrain.jex.executors.tasks.RunnableTask;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import static com.devontrain.jex.executors.tasks.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 09.08.17.
 */
class internals {

    private internals() {

    }


    @SuppressWarnings("unchecked")
    interface Submiter<K, C> {

        default CompletableFuture<?> submit(K key, Runnable runnable) {
            return ((ExecutorBase) this).execute(key, new FutureTask(new RunnableTask<>(runnable, Boolean.TRUE)));
        }

        default CompletableFuture<?> submit(K key, Consumer<C> consumer) {
            return ((ExecutorBase) this).execute(key, new FutureTask(consumer));
        }

        default <T> CompletableFuture<T> submit(K key, Function<C, T> callable) {
            return ((ExecutorBase) this).execute(key, new FutureTask(new ContextCallableTask<>(callable)));
        }
    }

    interface Reporter<K, C extends Context<K>> {

        @SuppressWarnings("unchecked")
        default <A> CompletableFuture<?> report(K key, Function<C, A> association, Consumer<A> consumer) {
            return ((ExecutorBase) this).execute(key, association, new FutureTask(consumer));
        }

        @SuppressWarnings("unchecked")
        default <A, T> CompletableFuture<T> report(K key, Function<C, A> association, Function<A, T> callable) {
            return ((ExecutorBase) this).execute(key, association, new ContextCallableTask<>(callable));
        }

    }

    static <T> void complete(CompletableFuture<T> result, T t, Throwable ex) {
        try {
            if (ex == null) {
                result.complete(t);
            } else {
                result.completeExceptionally(ex);
            }
        } catch (Throwable e) {
            result.obtrudeException(e);
        }
    }
}
