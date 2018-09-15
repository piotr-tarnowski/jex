package com.devontrain.jex.executors;


import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 11.08.17.
 */
public class AssociateableExecutor<K, C extends Context<K>, A extends Associate<C>> extends ExecutorBase<K, C> implements internals.Submiter<K, A>, internals.Reporter<K, C> {

    AssociateableExecutor(LoggingStrategy loggingStrategy,
                          ExecutorService executor,
                          Map<K, C> contexts,
                          BiFunction<? extends ExecutorBase<K, C>, K, Context> resolver,
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
