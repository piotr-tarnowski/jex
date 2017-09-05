package com.devontrain.jex.common;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 08.06.16.
 */
public class Holder<T> implements Supplier<T>, Consumer<T> {
    private T value;

    public Holder() {
    }

    public Holder(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void accept(T t) {
        this.value = t;
    }

    public void consume(Consumer<T> consumer) {
        if (value == null) return;
        consumer.accept(value);
        value = null;
    }

    public T reset() {
        T t = this.value;
        this.value = null;
        return t;
    }
}
