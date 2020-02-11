package com.devontrain.jex.common;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 31.07.17.
 */
public class BooleanHolder implements BooleanSupplier, Consumer<Boolean>, Supplier<Boolean> {

    private boolean value;

    public BooleanHolder() {
    }

    public BooleanHolder(boolean value) {
        this.value = value;
    }

    @Override
    public boolean getAsBoolean() {
        return value;
    }

    @Override
    public Boolean get() {
        return value;
    }

    public void accept(boolean t) {
        this.value = t;
    }

    @Override
    public void accept(Boolean t) {
        this.value = t;
    }

    public boolean reset() {
        boolean t = this.value;
        this.value = false;
        return t;
    }
}
