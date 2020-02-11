package com.devontrain.jex.executors;


import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 25.07.17.
 */
@SuppressWarnings("unchecked")
public final class ContextualExecutor<K, C extends Context<K>> extends ExecutorBase<K, C> implements internals.Submiter<K, C>, internals.Reporter<K, C> {

    ContextualExecutor(LoggingStrategy loggingStrategy,
                       ExecutorService executor,
                       Map contexts,
                       BiFunction resolver,
                       Function<C, ?> association,
                       ContextClosingStrategy contextClosingStrategy,
                       ContextExecutionStrategy contextExecutionStrategy,
                       ContextResolvingStrategy contextResolvingStrategy,
                       Predicate<C> interruptionStrategy,
                       int tasksLimit,
                       int joinTimeOut) {
        super(loggingStrategy, executor, contexts, resolver, association, contextClosingStrategy, contextExecutionStrategy, contextResolvingStrategy, interruptionStrategy, tasksLimit, joinTimeOut);
    }
}
