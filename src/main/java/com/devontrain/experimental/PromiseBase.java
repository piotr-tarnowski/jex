package com.devontrain.experimental;

import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 25.05.16.
 */
public abstract class PromiseBase<T> implements Promise<T>, Consumer<Future<T>> {

    protected static final ThenableChainInner IDENTITY = new ThenableChainInner() {
        @Override
        public Object apply(Object o) {
            return o;
        }
    };


    private volatile boolean cancel;
    protected ThenableChainInner function = IDENTITY;

    @FunctionalInterface
    protected interface ThenableChainInner extends Function, ThenableChain {
        default boolean isCanceled() {
            return false;
        }

        default void cancel() {
        }
    }

    private abstract class ThenableChainInnerImpl implements ThenableChainInner {
        protected final ThenableChainInner f;

        protected ThenableChainInnerImpl(ThenableChainInner f) {
            this.f = f;
        }

        public boolean isCanceled() {
            return cancel;
        }

        @Override
        public void cancel() {
            cancel = true;
        }
    }

    private class ThenableChainFunctionImpl extends ThenableChainInnerImpl {

        private final Function a;

        private ThenableChainFunctionImpl(ThenableChainInner f, Function a) {
            super(f);
            this.a = a;
        }

        @Override
        public Object apply(Object o) {
            Object r = f.apply(o);
            if (f.isCanceled()) {
                return null;
            } else {
                return a.apply(r);
            }
        }
    }

    private class ThenableChainConsumerImpl extends ThenableChainInnerImpl {

        private final Consumer a;

        private ThenableChainConsumerImpl(ThenableChainInner f, Consumer a) {
            super(f);
            this.a = a;
        }

        @Override
        public Object apply(Object o) {
            Object r = f.apply(o);
            if (!f.isCanceled()) {
                a.accept(r);
            }
            return null;
        }
    }

    private class ThenableChainBiFunctionImpl extends ThenableChainInnerImpl {

        private final BiFunction a;

        private ThenableChainBiFunctionImpl(ThenableChainInner f, BiFunction a) {
            super(f);
            this.a = a;
        }

        @Override
        public Object apply(Object o) {
            Object r = f.apply(o);
            if (f.isCanceled()) {
                return null;
            } else {
                return a.apply(r, this);
            }
        }
    }

    @Override
    public <R> Thenable<R> then(BiFunction<Future<T>, ThenableChain, R> after) {
        this.function = new ThenableChainBiFunctionImpl(this.function, after);
        return (Thenable<R>) this;
    }

    @Override
    public <R> Thenable<R> then(Function<Future<T>, R> after) {
        this.function = new ThenableChainFunctionImpl(this.function, after);
        return (Thenable<R>) this;
    }

    @Override
    public void then(Consumer<Future<T>> after) {
        this.function = new ThenableChainConsumerImpl(this.function, after);
    }

    @Override
    public void accept(Future<T> future) {
        this.function.apply( future );
    }
}
