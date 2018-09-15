package com.devontrain.jex.executors;

import com.devontrain.jex.common.BooleanHolder;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.devontrain.jex.executors.tasks.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 28.07.17.
 */
@SuppressWarnings("unchecked")
public enum ContextResolvingStrategy {

    RESOLVE_CONTEXT_IN_CALLER_THREAD(
            ContextResolvingStrategy::resolveContextInCallerThread,
            ContextResolvingStrategy::resolveAssociateInCallerThread),
    RESOLVE_CONTEXT_IN_TASK_THREAD(
            ContextResolvingStrategy::resolveContextInTaskThread,
            ContextResolvingStrategy::resolveAssociateInTaskThread);

    final Function<ExecutorBase, BiConsumer> contextStrategy;
    final Function<ExecutorBase, TriConsumer> associationStrategy;

    ContextResolvingStrategy(Function<ExecutorBase, BiConsumer> contextStrategy,
                             Function<ExecutorBase, TriConsumer> associationStrategy) {
        this.contextStrategy = contextStrategy;
        this.associationStrategy = associationStrategy;
    }


    private static <K, C extends Context<K>> BiConsumer<K, CompletableTask> resolveContextInCallerThread(ExecutorBase<K, C> executor) {
        return (key, task) -> invokeInCallerThread(executor, key, task, ctx -> task);
    }

    private static <K, C extends Context<K>> BiConsumer<K, CompletableTask> resolveContextInTaskThread(ExecutorBase<K, C> executor) {
        return (key, task) -> invokeInTaskThread(executor, key, task, ctx -> task);
    }

    private static <K, C extends Context<K>> TriConsumer<K, Function<Context<K>, Object>, CompletableTask> resolveAssociateInCallerThread(ExecutorBase<K, C> executor) {
        return (key, association, task) -> invokeInCallerThread(executor, key, createAssociatedTask(association, task),
                ctx -> {
                    Object associate = association.apply(ctx);
                    return c -> task.accept(associate);
                }
        );
    }

    private static <K, C extends Context<K>> TriConsumer<K, Function<Context<K>, Object>, CompletableTask> resolveAssociateInTaskThread(ExecutorBase<K, C> executor) {
        return (key, association, task) -> invokeInTaskThread(executor, key, createAssociatedTask(association, task),
                ctx -> {
                    Object associate = association.apply(ctx);
                    return c -> task.accept(associate);
                }
        );
    }

    private static <K, C extends Context<K>> void invokeInCallerThread(ExecutorBase<K, C> executor,
                                                                       K key,
                                                                       CompletableTask task,
                                                                       Function<Context<K>, Consumer<Context<K>>> supplier) {
        BooleanHolder holder = new BooleanHolder();
        C context = executor.resolveContext(key, supplier, holder);
        if (holder.getAsBoolean()) {
            executor.execute(() -> {
                context.processor = Thread.currentThread();
                task.run(executor, context);
            });
        }
    }

    private static <K, C extends Context<K>> void invokeInTaskThread(ExecutorBase<K, C> executor,
                                                                     K key,
                                                                     CompletableTask task,
                                                                     Function<Context<K>, Consumer<Context<K>>> supplier) {
        executor.contexts.compute(key, (k, ctx) -> {
            if (ctx == null) {
                ctx = (C) new Context<>(executor, k);
            }
            boolean empty = ctx.tasks.isEmpty();
            if (Context.class == ctx.getClass()) {
                ctx.tasks.add(supplier);
            } else {
                ctx.tasks.add(supplier.apply(ctx));
            }
            if (empty) {
                executor.execute(() -> {
                    C context = executor.resolveContext(k, c -> (Collection<Function>) c.tasks);
                    context.processor = Thread.currentThread();
                    task.run(executor, context);
                });
            }
            return ctx;
        });

    }


    private static <K> FutureTask createAssociatedTask(Function<Context<K>, Object> association,
                                                       CompletableTask task) {
        return new FutureTask(ctx -> task.accept(association.apply((Context<K>) ctx)));
    }
}
