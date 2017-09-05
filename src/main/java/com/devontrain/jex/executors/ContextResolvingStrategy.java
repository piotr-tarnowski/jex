package com.devontrain.jex.executors;

import com.devontrain.jex.common.BooleanHolder;
import com.devontrain.jex.common.Holder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.devontrain.jex.executors.tasks.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 28.07.17.
 */
public enum ContextResolvingStrategy {

    RESOLVE_CONTEXT_IN_CALLER_THREAD(
            ContextResolvingStrategy::resolveContextInCallerThread,
            ContextResolvingStrategy::resolveAssociateInCallerThread),
    RESOLVE_CONTEXT_IN_TASK_THREAD(
            ContextResolvingStrategy::resolveContextInTaskThread,
            ContextResolvingStrategy::resolveAssociateInTaskThread);

    final Function<ExecutorBase, BiConsumer> contextStrategy;
    final Function<ExecutorBase, TriConsumer> associationStrategy;

    ContextResolvingStrategy(Function<ExecutorBase, BiConsumer> contextStrategy, Function<ExecutorBase, TriConsumer> associationStrategy) {
        this.contextStrategy = contextStrategy;
        this.associationStrategy = associationStrategy;
    }


    private static <K, C extends Context<K>> BiConsumer<K, CompletableTask> resolveContextInCallerThread(ExecutorBase<K, C> pool) {
        return (key, task) -> {
            BooleanHolder holder = new BooleanHolder();
            C context = resolveContext(pool, key, ctx -> task, holder);
            invokeInCallerThread(pool, task, holder, context);
        };
    }

    private static <K, C extends Context<K>> BiConsumer<K, CompletableTask> resolveContextInTaskThread(ExecutorBase<K, C> pool) {
        return (key, task) -> {
            Function<Context<K>, Consumer<Context<K>>> supplier = ctx -> task;
            invokeInTaskThread(pool, key, task, supplier);
        };
    }

    private static <K, C extends Context<K>> TriConsumer<K, Function<Context<K>, Object>, CompletableTask> resolveAssociateInCallerThread(ExecutorBase<K, C> pool) {
        return (key, association, task) -> {
            BooleanHolder holder = new BooleanHolder();
            C context = resolveContext(pool, key, ctx -> {
                Object associate = association.apply(ctx);
                return c -> task.accept(associate);
            }, holder);
            if (holder.getAsBoolean()) {
                pool.execute(() -> {
                    context.processor = Thread.currentThread();
                    task.run(pool, context);
                });
            }
        };
    }

    private static <K, C extends Context<K>> TriConsumer<K, Function<Context<K>, Object>, CompletableTask> resolveAssociateInTaskThread(ExecutorBase<K, C> pool) {
        return (key, association, task) -> {
            Holder<Function<Context<K>, Consumer<Context<K>>>> supplier = new Holder<>();
            pool.contexts.compute(key, (k, ctx) -> {
                if (ctx == null) {
                    supplier.accept(context -> c -> task.accept(association.apply(c)));
                } else {
                    Object associate = association.apply(ctx);
                    supplier.accept(context -> c -> task.accept(associate));
                }
                return ctx;
            });
            invokeInTaskThread(pool, key, task, supplier.get());
        };
    }

    private static <K, C extends Context<K>> void invokeInCallerThread(ExecutorBase<K, C> pool, CompletableTask task, BooleanHolder holder, C context) {
        if (holder.getAsBoolean()) {
            pool.execute(() -> {
                context.processor = Thread.currentThread();
                task.run(pool, context);
            });
        }
    }

    private static <K, C extends Context<K>> void invokeInTaskThread(ExecutorBase<K, C> pool, K key, CompletableTask task, Function<Context<K>, Consumer<Context<K>>> supplier) {
        pool.execute(() -> {
            BooleanHolder holder = new BooleanHolder();
            C context = resolveContext(pool, key, supplier, holder);
            if (holder.getAsBoolean()) {
                context.processor = Thread.currentThread();
                task.run(pool, context);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <K, C extends Context<K>> C resolveContext(ExecutorBase<K, C> pool, K key, Function<Context<K>, ? extends Consumer> supplier, BooleanHolder holder) {
        return pool.contexts.compute(key, (k, ctx) -> {
            if (ctx == null) {
                ctx = (C) pool.resolver.apply(pool, k);
            }
            holder.accept(ctx.tasks.isEmpty());
            ctx.tasks.add((Consumer<Context<K>>) supplier.apply(ctx));
            return ctx;
        });
    }

}
