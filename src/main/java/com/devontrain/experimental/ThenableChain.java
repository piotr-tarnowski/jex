package com.devontrain.experimental;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 06.05.16.
 */
public interface ThenableChain {

    boolean isCanceled();
    void cancel();
}
