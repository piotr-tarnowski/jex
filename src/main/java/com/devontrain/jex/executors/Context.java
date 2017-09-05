package com.devontrain.jex.executors;

import com.devontrain.jex.common.Holder;
import com.devontrain.jex.executors.tasks.CompletableTask;
import com.devontrain.jex.executors.tasks.RunnableTask;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.devontrain.jex.executors.internals.complete;
import static com.devontrain.jex.executors.tasks.run;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 09.08.17.
 */
public class Context<K> {

    private static final Logger LOGGER = Logger.getLogger("SCHEDULER");

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    protected final K key;
    final ExecutorBase<K, Context<K>> executor;
    final LinkedList<Consumer<Context<K>>> tasks;
    List<Consumer<Context<K>>> subtasks;
    boolean paused;
    Thread processor;

    //TODO: try to remove this suppress warnings
    @SuppressWarnings("unchecked")
    protected Context(ExecutorBase executor, K key) {
        this.executor = executor;
        this.key = key;
        this.tasks = new LinkedList<>();
    }

    @SuppressWarnings("unchecked")
    <A> A createAssociate() {
        return (A) executor.association.apply(this);
    }

    final CompletableFuture<?> runInSync(CompletableTask task) {
        if (processor == currentThread()) {
            process(subtasks, task);
        } else {
            executor.execute(key, task);
        }
        return task;
    }

    final <T> CompletableFuture<T> joinInSync(CompletableFuture<T> future) {
        return joinInSync(future, executor.joinTimeOut);
    }

    final <T> CompletableFuture<T> joinInSync(CompletableFuture<T> future, int timeout) {
        ensureRunInSync();
        CompletableFuture<T> result = new CompletableFuture<>();
        process(subtasks, () -> {
            paused = true;
            within(future, timeout).whenCompleteAsync((t, ex) -> {
                processor = Thread.currentThread();
                paused = false;
                subtasks = null;
                complete(result, t, ex);
                Holder<CompletableTask> holder = new Holder<>();
                executor.contextClosingStrategy.accept(key, holder);
                run(executor, this, holder);
            }, executor);
        });
        return result;
    }

    final CompletableFuture<?> process(List jobs, CompletableTask task) {
        if (paused) {
            if (jobs == null) {
                jobs = subtasks = tasks.subList(0, 1);
            }
            jobs.add(task);
        } else {
            task.accept(this);
        }
        return task;
    }

    final CompletableFuture<?> process(List jobs, Runnable runnable) {
        return process(jobs, new RunnableTask<>(runnable, Boolean.TRUE));
    }

    final void ensureRunInSync() {
        if (this.processor != currentThread()) {
            throw new IllegalStateException("Method joinInSync can be invoke only in runInSync block.");
        }
    }

    @Nonnull
    <T> CompletableFuture<T> within(CompletableFuture<T> future, long timeout) {
        ScheduledFuture<?> schedule = SCHEDULER.scheduleAtFixedRate(() -> {
            if (tasks.size() < executor.tasksLimit) {
                LOGGER.warning(() -> "Future  " + future + " reached its timeout " + timeout + "[ms]");
            } else {
                future.completeExceptionally(new TimeoutException("Timeout after " + timeout + "[ms]"));
            }
        }, timeout, timeout, MILLISECONDS);
        future.whenComplete((t, throwable) -> schedule.cancel(true));
        return future;
    }
}
