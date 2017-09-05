package com.devontrain.jex.executors;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 27.07.17.
 */
public class ContextWrapper<K, C> extends Context<K> {

    protected C ctx;

    protected ContextWrapper(ExecutorBase<K, ?> executor, K key) {
        super(executor, key);
    }

    public C getCtx() {
        return ctx;
    }
}
