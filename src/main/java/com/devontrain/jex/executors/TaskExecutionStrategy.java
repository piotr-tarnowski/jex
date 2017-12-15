package com.devontrain.jex.executors;

import com.devontrain.jex.common.Holder;
import com.devontrain.jex.executors.tasks.TriConsumer;

import java.util.function.Consumer;

import static com.devontrain.jex.executors.tasks.run;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 15.12.17.
 */
@SuppressWarnings("unchecked")
public enum TaskExecutionStrategy {

    NO_INTERRUPTION((executor, context, holder) -> {
        while (holder.get() != null) {
            holder.reset().accept(context);
            executor.contextClosingStrategy.accept(context.key, holder);
            if (context.paused) {
                break;
            }
        }
        context.processor = null;
    }),
    DEFINED_PREDICATE((executor, context, holder) -> {
        while (holder.get() != null) {
            boolean interrupt = Thread.interrupted() || executor.interruptionStrategy.test(context);
            if (interrupt) {
                executor.execute(() -> run(executor, context, holder));
                break;
            } else {
                holder.reset().accept(context);
                executor.contextClosingStrategy.accept(context.key, holder);
                if (context.paused) {
                    break;
                }
            }
        }
        context.processor = null;
    });

    final TriConsumer<ExecutorBase, Context, Holder<Consumer>> strategy;

    TaskExecutionStrategy(TriConsumer<ExecutorBase, Context, Holder<Consumer>> strategy) {
        this.strategy = strategy;
    }
}
