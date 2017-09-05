package com.devontrain.experimental;

import java.util.concurrent.Future;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 04.05.16.
 */
public interface Promise<T> extends Thenable<Future<T>> {

}
