package com.devontrain.experimental;

import javax.annotation.Nonnull;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 17.08.16.
 */
//for blocking policy for non blocking available processor and fixed size would be enough.
class LazyBlockingQueueDelegate<E extends Runnable> extends BlockingQueueDelegate<E> {
    ThreadPoolExecutor executor;

    LazyBlockingQueueDelegate(BlockingQueue<E> origin) {
        super(origin);
    }

    @Override
    public boolean offer(@Nonnull E runnable) {
        synchronized (executor) {
            if (executor.getPoolSize() + 1 < executor.getMaximumPoolSize()) {
                return false;
            }
        }
        return super.offer(runnable);
    }
}
