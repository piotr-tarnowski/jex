package com.devontrain.jex.executors;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.devontrain.jex.executors.ContextClosingStrategy.REMOVE_CONTEXT_AFTER_LAST_TASK;
import static com.devontrain.jex.executors.ContextExecutionStrategy.ASSOCIATE;
import static com.devontrain.jex.executors.ContextExecutionStrategy.CONTEXT;
import static com.devontrain.jex.executors.ContextResolvingStrategy.RESOLVE_CONTEXT_IN_CALLER_THREAD;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 07.08.17.
 */
@SuppressWarnings("unchecked")
public class ContextualExecutorBuilder<K, C extends Context<K>> {

    private static final Function<Object, Object> NO_ASSOCIATION = Function.identity();

    private ExecutorService executor;
    private Map<K, C> contexts;
    private BiFunction<? extends ExecutorBase<K, C>, K, Context> resolver;
    private ContextClosingStrategy contextClosingStrategy;
    private ContextExecutionStrategy contextExecutionStrategy;
    private ContextResolvingStrategy contextResolvingStrategy;
    private int tasksLimit = -1;
    private int joinTimeOut = -1;

    public final ContextualExecutorBuilder<K, C> executor(ExecutorService executor) {
        this.executor = executor;
        return this;
    }

    public final ContextualExecutorBuilder<K, C> contexts(Map<K, C> contexts) {
        this.contexts = contexts;
        return this;
    }

    public ContextualExecutorBuilder<K, C> contextResolver(BiFunction<ExecutorBase, K, C> resolver) {
        this.resolver = (BiFunction) resolver;
        return this;
    }

    public final ContextualExecutorBuilder<K, C> contextRemovingStrategy(ContextClosingStrategy taskHandler) {
        this.contextClosingStrategy = taskHandler;
        return this;
    }

    public final ContextualExecutorBuilder<K, C> contextResolvingStrategy(ContextResolvingStrategy taskExecutor) {
        this.contextResolvingStrategy = taskExecutor;
        return this;
    }

    public final ContextualExecutorBuilder<K, C> taskLimit(int tasksLimit) {
        this.tasksLimit = tasksLimit;
        return this;
    }

    public final ContextualExecutorBuilder<K, C> joinTimeOut(int joinTimeOut) {
        this.joinTimeOut = joinTimeOut;
        return this;
    }

    void initialize(Function association) {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        if (contexts == null) {
            contexts = new ConcurrentHashMap<>();
        }
        if (resolver == null) {
            resolver = Context::new;
        }
        if (contextClosingStrategy == null) {
            contextClosingStrategy = REMOVE_CONTEXT_AFTER_LAST_TASK;
        }
        if (association == NO_ASSOCIATION) {
            contextExecutionStrategy = CONTEXT;
        } else {
            contextExecutionStrategy = ASSOCIATE;
        }
        if (contextResolvingStrategy == null) {
            contextResolvingStrategy = RESOLVE_CONTEXT_IN_CALLER_THREAD;
        }
        if (tasksLimit < 0) {
            tasksLimit = 100;
        }
        if (joinTimeOut < 0) {
            joinTimeOut = 250;
        }
    }

    public final ContextualExecutor<K, C> build() {
        initialize(NO_ASSOCIATION);
        return new ContextualExecutor<K, C>(
                executor,
                contexts,
                resolver,
                (Function) NO_ASSOCIATION,
                contextClosingStrategy,
                contextExecutionStrategy,
                contextResolvingStrategy,
                tasksLimit,
                joinTimeOut
        );
    }

    public final <A extends Associate<T>, T extends Context<K>> AssociateableExecutor<K, T, A> build(Class<A> clazz, Function<T, A> association) {
        initialize(association);
        return new AssociateableExecutor<K, T, A>(
                executor,
                (Map) contexts,
                (BiFunction) resolver,
                association,
                contextClosingStrategy,
                contextExecutionStrategy,
                contextResolvingStrategy,
                tasksLimit,
                joinTimeOut
        );
    }
}
