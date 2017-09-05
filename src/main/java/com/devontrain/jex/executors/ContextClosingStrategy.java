package com.devontrain.jex.executors;



import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 28.07.17.
 */
public enum ContextClosingStrategy {

    LEAVE_CONTEXT_AFTER_LAST_TASK(ContextClosingStrategy::leaveContextAfterLastTask),
    REMOVE_CONTEXT_AFTER_LAST_TASK(ContextClosingStrategy::removeContextAfterLastTask);

    final Function<ExecutorBase, BiConsumer> strategy;

    ContextClosingStrategy(Function<ExecutorBase, BiConsumer> strategy) {
        this.strategy = strategy;
    }

    private static <K, C extends Context<K>> BiConsumer<K, Consumer<Consumer<Context<K>>>> leaveContextAfterLastTask(ExecutorBase<K, C> pool) {
        return (key, consumer) ->
                pool.contexts.computeIfPresent(key, (k, ctx) -> {
                    ctx.tasks.poll();
                    consumer.accept(ctx.tasks.peek());
                    return ctx;
                });
    }

    private static <K, C extends Context<K>> BiConsumer<K, Consumer<Consumer<Context<K>>>> removeContextAfterLastTask(ExecutorBase<K, C> pool) {
        return (key, consumer) ->
                pool.contexts.computeIfPresent(key, (k, ctx) -> {
                    ctx.tasks.poll();
                    if (ctx.tasks.isEmpty()) {
                        return null;
                    }
                    consumer.accept(ctx.tasks.peek());
                    return ctx;
                });
    }

}
