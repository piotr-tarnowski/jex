package com.devontrain.jex.executors;

import com.devontrain.jex.common.Holder;
import com.devontrain.jex.executors.tasks.TriConsumer;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;


/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 15.12.17.
 */
@SuppressWarnings("unchecked")
public enum TaskExecutionStrategy {

    NO_INTERRUPTION((executor, context, holder) -> handle(() ->
            handleTask(executor, context, holder), holder)),
    DEFINED_PREDICATE((executor, context, holder) -> handle(() ->
            handleInterruption(executor, context, holder)
            || handleTask(executor, context, holder), holder));

    final TriConsumer<ExecutorBase, Context, Holder<? extends Consumer>> strategy;

    TaskExecutionStrategy(TriConsumer<ExecutorBase, Context, Holder<? extends Consumer>> strategy) {
        this.strategy = strategy;
    }

    private static void handle(BooleanSupplier supplier,
                               Holder holder) {
        while (holder.get() != null) {
            if (supplier.getAsBoolean()) {
                break;
            }
        }
    }

    private static boolean handleInterruption(ExecutorBase executor,
                                              Context context,
                                              Holder<? extends Consumer> holder) {
        boolean interrupt = context.wasInterrupted
                = !context.wasInterrupted && (Thread.interrupted() || executor.interruptionPredicate.test(context));
        if (interrupt) {
            executor.execute(() -> executor.run(context, holder));
            return true;
        }
        return false;
    }

    private static boolean handleTask(ExecutorBase executor,
                                      Context context,
                                      Holder<? extends Consumer> holder) {
        holder.reset().accept(context);
        executor.contextClosingStrategy.accept(context.key, holder);
        return context.paused || /*ADDED*/ context.tasks.isEmpty();
    }
}
