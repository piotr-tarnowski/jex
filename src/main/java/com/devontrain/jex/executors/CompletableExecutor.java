package com.devontrain.jex.executors;

import com.devontrain.jex.executors.tasks.CallableTask;
import com.devontrain.jex.executors.tasks.RunnableTask;
import com.sun.istack.internal.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 31.07.17.
 */
public class CompletableExecutor extends AbstractExecutorService {

    private final ExecutorService service;

    public CompletableExecutor(ExecutorService service) {
        this.service = service;
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        service.execute(command);
    }

    @Override
    @Nonnull
    public CompletableFuture<?> submit(@Nonnull Runnable task) {
        RunnableTask<Boolean> future = new RunnableTask<>(task, Boolean.TRUE);
        execute(future);
        return future;
    }

    @Override
    @Nonnull
    public <T> CompletableFuture<T> submit(@Nonnull Callable<T> task) {
        CallableTask<T> future = new CallableTask<>(task);
        execute(future);
        return future;
    }

    @Override
    @Nonnull
    public <T> CompletableFuture<T> submit(@Nonnull Runnable task,
                                           T result) {
        RunnableTask<T> future = new RunnableTask<>(task, result);
        execute(future);
        return future;
    }

    @Override
    public void shutdown() {
        service.shutdown();
    }

    @Override
    @Nonnull
    public List<Runnable> shutdownNow() {
        return service.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return service.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return service.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout,
                                    @Nonnull TimeUnit unit) throws InterruptedException {
        return service.awaitTermination(timeout, unit);
    }
}
