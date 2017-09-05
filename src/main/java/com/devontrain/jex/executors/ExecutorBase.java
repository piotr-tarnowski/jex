package com.devontrain.jex.executors;



import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.devontrain.jex.executors.tasks.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 07.08.17.
 */
public abstract class ExecutorBase<K, C> extends CompletableExecutor {


    final Map<K, C> contexts;
    final BiFunction<ExecutorBase, K, Context> resolver;
    final Function<C, ?> association;
    final BiConsumer<K, Consumer<CompletableTask>> contextClosingStrategy;
    final BiFunction<K, CompletableTask, CompletableFuture> contextExecutionStrategy;
    final BiConsumer<K, CompletableTask> contextResolvingStrategy;
    final TriConsumer<K, Function<C, Object>, CompletableTask> associateResolvingStrategy;
    final int tasksLimit;
    final int joinTimeOut;

    @SuppressWarnings("unchecked")
    ExecutorBase(ExecutorService executor,
                 Map<K, C> contexts,
                 BiFunction<? extends ExecutorBase<K, C>, K, Context> resolver,
                 Function<C, ?> association, ContextClosingStrategy contextClosingStrategy,
                 ContextExecutionStrategy contextExecutionStrategy,
                 ContextResolvingStrategy contextResolvingStrategy, int tasksLimit, int joinTimeOut) {
        super(executor);
        this.contexts = contexts;
        this.resolver = (BiFunction) resolver;
        this.association = association;
        this.contextClosingStrategy = contextClosingStrategy.strategy.apply(this);
        this.contextExecutionStrategy = contextExecutionStrategy.strategy.apply(this);
        this.contextResolvingStrategy = contextResolvingStrategy.contextStrategy.apply(this);
        this.associateResolvingStrategy = contextResolvingStrategy.associationStrategy.apply(this);
        this.tasksLimit = tasksLimit;
        this.joinTimeOut = joinTimeOut;
    }

    final <T> CompletableFuture<T> execute(K key, CompletableTask<T> task) {
        return contextExecutionStrategy.apply(key, task);
    }

    @SuppressWarnings("unchecked")
    final <A, T> CompletableFuture<T> execute(K key, Function<C, A> association, CompletableTask<T> task) {
        associateResolvingStrategy.accept(key, (Function) association, task);
        return task;
    }
}
