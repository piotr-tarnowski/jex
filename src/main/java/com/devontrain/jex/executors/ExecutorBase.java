package com.devontrain.jex.executors;


import com.devontrain.jex.common.BooleanHolder;
import com.devontrain.jex.common.Holder;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.*;

import static com.devontrain.jex.executors.TaskExecutionStrategy.DEFINED_PREDICATE;
import static com.devontrain.jex.executors.TaskExecutionStrategy.NO_INTERRUPTION;
import static com.devontrain.jex.executors.tasks.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 07.08.17.
 */
@SuppressWarnings("unchecked")
public abstract class ExecutorBase<K, C extends Context> extends CompletableExecutor {


    final Map<K, C> contexts;
    final Function<C, ?> association;
    final BiConsumer<K, Consumer> contextClosingStrategy;
    final BiConsumer<K, CompletableTask> contextResolvingStrategy;
    final TriConsumer<K, Function<C, Object>, CompletableTask> associateResolvingStrategy;
    final int tasksLimit;
    final int joinTimeOut;
    final Predicate<C> interruptionPredicate;
    private final BiFunction<ExecutorBase, K, Context> resolver;
    private final BiFunction<K, CompletableTask, CompletableFuture> contextExecutionStrategy;
    private final TaskExecutionStrategy taskExecutionStrategy;

    ExecutorBase(ExecutorService executor,
                 Map<K, C> contexts,
                 BiFunction<? extends ExecutorBase<K, C>, K, Context> resolver,
                 Function<C, ?> association,
                 ContextClosingStrategy contextClosingStrategy,
                 ContextExecutionStrategy contextExecutionStrategy,
                 ContextResolvingStrategy contextResolvingStrategy,
                 Predicate<C> interruptionPredicate,
                 int tasksLimit,
                 int joinTimeOut) {
        super(executor);
        this.contexts = contexts;
        this.resolver = (BiFunction) resolver;
        this.association = association;
        this.contextClosingStrategy = contextClosingStrategy.strategy.apply(this);
        this.contextExecutionStrategy = contextExecutionStrategy.strategy.apply(this);
        this.contextResolvingStrategy = contextResolvingStrategy.contextStrategy.apply(this);
        this.associateResolvingStrategy = contextResolvingStrategy.associationStrategy.apply(this);
        this.interruptionPredicate = interruptionPredicate;
        if (interruptionPredicate == null) {
            this.taskExecutionStrategy = NO_INTERRUPTION;
        } else {
            this.taskExecutionStrategy = DEFINED_PREDICATE;
        }
        this.tasksLimit = tasksLimit;
        this.joinTimeOut = joinTimeOut;
    }

    final <T> CompletableFuture<T> execute(K key,
                                           CompletableTask<T> task) {
        return contextExecutionStrategy.apply(key, task);
    }

    final <A, T> CompletableFuture<T> execute(K key,
                                              Function<C, A> association,
                                              CompletableTask<T> task) {
        associateResolvingStrategy.accept(key, (Function) association, task);
        return task;
    }

    final void run(C context,
                   Holder<? extends Consumer> holder) {
        taskExecutionStrategy.strategy.accept(this, context, holder);
        context.processor = null;
    }


    @SuppressWarnings("unchecked")
    final C resolveContext(K key,
                           Function<Context<K>, ? extends Consumer> supplier,
                           BooleanHolder holder) {
        return contexts.compute(key, (k, ctx) -> {
            if (ctx == null) {
                ctx = (C) resolver.apply(this, k);
            }
            holder.accept(ctx.tasks.isEmpty());
            ctx.tasks.add((Consumer<Context<K>>) supplier.apply(ctx));
            return ctx;
        });
    }
}
