package com.devontrain.jex.executors;

import com.devontrain.jex.executors.tasks.CompletableTask;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 11.08.17.
 */
@SuppressWarnings("unchecked")
enum ContextExecutionStrategy {

    CONTEXT(ContextExecutionStrategy::contextResolvingStrategy),
    ASSOCIATE(ContextExecutionStrategy::associateResolvingStrategy);

    final Function<ExecutorBase, BiFunction> strategy;

    ContextExecutionStrategy(Function<ExecutorBase, BiFunction> strategy) {
        this.strategy = strategy;
    }

    private static <K, T> BiFunction<K, CompletableTask<T>, CompletableFuture<T>> contextResolvingStrategy(ExecutorBase executor) {
        return (key, task) -> {
            executor.contextResolvingStrategy.accept(key, task);
            return task;
        };
    }

    private static <K, T> BiFunction<K, CompletableTask<T>, CompletableFuture<T>> associateResolvingStrategy(ExecutorBase executor) {
        return (key, task) -> {
            executor.associateResolvingStrategy.accept(key, executor.association, task);
            return task;
        };
    }
}
