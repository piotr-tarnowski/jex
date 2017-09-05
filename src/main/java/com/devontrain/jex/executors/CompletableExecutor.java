package com.devontrain.jex.executors;

import java.util.List;
import java.util.concurrent.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 31.07.17.
 */
public class CompletableExecutor extends AbstractExecutorService {

    final ExecutorService service;

    public CompletableExecutor(ExecutorService service) {
        this.service = service;
    }

    @Override
    public void execute(Runnable command) {
        service.execute(command);
    }

    @Override
    public CompletableFuture<?> submit(Runnable task) {
        tasks.RunnableTask<Boolean> future = new tasks.RunnableTask<>(task, Boolean.TRUE);
        execute(future);
        return future;
    }

    @Override
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        tasks.CallableTask<T> future = new tasks.CallableTask<>(task);
        execute(future);
        return future;
    }

    @Override
    public <T> CompletableFuture<T> submit(Runnable task, T result) {
        tasks.RunnableTask<T> future = new tasks.RunnableTask<>(task, result);
        execute(future);
        return future;
    }

    @Override
    public void shutdown() {
        service.shutdown();
    }

    @Override
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
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return service.awaitTermination(timeout, unit);
    }
}
