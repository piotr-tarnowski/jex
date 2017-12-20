package com.devontrain.jex.executors;

import com.devontrain.jex.common.BooleanHolder;
import com.devontrain.jex.common.Holder;

import java.util.Collection;
import java.util.LinkedList;
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
        return (key, association, task) -> invokeInCallerThread(executor, key, task, ctx -> {
            Object associate = association.apply(ctx);
            return c -> task.accept(associate);
        });
    }

    private static <K, C extends Context<K>> TriConsumer<K, Function<Context<K>, Object>, CompletableTask> resolveAssociateInTaskThread(ExecutorBase<K, C> executor) {
        return (key, association, task) -> {
            Holder<Function<Context<K>, Consumer<Context<K>>>> supplier = new Holder<>();
            executor.contexts.compute(key, (k, ctx) -> {
                Function _association;
                if (ctx == null || ctx.getClass() == Context.class) {
                    _association = association;
                } else {
                    Object associate = association.apply(ctx);
                    _association = c -> associate;
                }
                supplier.accept(context -> c -> task.accept(_association.apply(c)));
                return ctx;
            });
            invokeInTaskThread(executor, key, task, supplier.get());
        };
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
            BooleanHolder empty = new BooleanHolder();
            if (ctx == null) {
                ctx = (C) new Context<>(executor, k);
            }
            empty.accept(ctx.tasks.isEmpty());
            if (Context.class == ctx.getClass()) {
                ctx.tasks.add(supplier);
            } else {
                ctx.tasks.add(supplier.apply(ctx));
            }
            if (empty.getAsBoolean()) {
                executor.execute(() -> {
                    BooleanHolder holder = new BooleanHolder();
                    C context = executor.contexts.compute(k, (k2, ctx2) -> {
                        if (ctx2.getClass() == Context.class) {
                            C newCtx = (C) executor.resolver.apply(executor, k2);
                            for (Function<Context<K>, Consumer<Context<K>>> func : (Collection<Function<Context<K>, Consumer<Context<K>>>>) ctx2.tasks) {
                                Consumer<Context<K>> consumer = func.apply(newCtx);
                                newCtx.tasks.add(consumer);
                            }
                            ctx2 = newCtx;
                        }
                        holder.accept(empty.getAsBoolean());
                        return ctx2;
                    });
//                        .recreateContext(key, empty.getAsBoolean(), supplier, holder);
                    if (holder.getAsBoolean()) {
                        System.err.println(Thread.currentThread().getName() + "!!!!" + empty.getAsBoolean() + "/" + context.tasks.size() + "/" + key + ":" + k);
                        context.processor = Thread.currentThread();
                        task.run(executor, context);
                    }
                });
            }
            return ctx;
        });
    }
}
