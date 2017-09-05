package com.devontrain.promises;

import com.devontrain.experimental.PromiseBase;
import com.devontrain.experimental.Thenable;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.function.Consumer;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 25.05.16.
 */
public class Deferred<V> {

    private volatile FutureTask<V> task;
    private final PromiseBase<V> promise = new PromiseBase<V>() {
    };

    private class InternalFutureTask extends FutureTask<V> {

        public InternalFutureTask(Callable<V> callable) {
            super(callable);
        }

        @Override
        protected void set(V v) {
            int state = UNSAFE.getInt(this, stateOffset);
            super.set(v);
            try {
                promise.accept(this);
            } catch (Throwable t) {
                UNSAFE.putInt( this, stateOffset, state);
                setException(t);
            }
        }

    }

    public RunnableFuture<V> resolve(V result) {
        return task = new InternalFutureTask(() -> result);
    }

    public RunnableFuture<V> resolve(Callable<V> callable) {
        return task = new InternalFutureTask(callable);
    }

    public RunnableFuture<V> resolve(Runnable runnable, V result) {
        return task = new InternalFutureTask(() -> {
            runnable.run();
            return result;
        });
    }

    public void promise(Consumer<Thenable<Future<V>>> consumer) {
        consumer.accept(this.promise);
    }

    @SuppressWarnings("restriction")
    private static Unsafe getUnsafe() {
        try {

            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);

        } catch (Exception e) {
            throw new Error(e);
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;

    static {
        try {
            UNSAFE = getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
