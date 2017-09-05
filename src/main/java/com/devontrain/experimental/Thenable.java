package com.devontrain.experimental;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 05.05.16.
 */
public interface Thenable<T> {

    <R> Thenable<R> then(BiFunction<T,ThenableChain,R> after);
    <R> Thenable<R> then(Function<T, R> after);
    void then(Consumer<T> consumer);
}
