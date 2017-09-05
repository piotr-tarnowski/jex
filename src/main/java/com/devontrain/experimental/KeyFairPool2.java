package com.devontrain.experimental;

import com.devontrain.jex.common.Holder;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 25.07.17.
 */
public class KeyFairPool2<K, C> extends ThreadPoolExecutor {

    private final Map<K, KeyContext> contexts = new ConcurrentHashMap<>();
    private final Function<K, C> creator;

    public KeyFairPool2(int threadNumber, Function<K, C> creator) {
        super(threadNumber, threadNumber, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.creator = creator;
    }

    public <T> Future<T> submit(K key, Function<C, T> callable) {
        Task<T> task = new Task<>(callable);
        contexts.compute(key, (k, ctx) -> {
            if (ctx == null) {
                ctx = new KeyContext(key, creator.apply(k));
            }
            ctx.execute(task);
            return ctx;
        });
        return task;
    }

    class KeyContext {
        final K key;
        final C ctx;
        final Queue<Task<?>> tasks;

        KeyContext(K key, C ctx) {
            this.key = key;
            this.ctx = ctx;
            this.tasks = new LinkedList<>();
        }

        void execute(Task<?> task) {
            if (tasks.isEmpty()) {
                KeyFairPool2.this.execute(() -> task.run(this));
            }
            tasks.add(task);
        }
    }

    class Task<T> extends CompletableFuture<T> {
        final Function<C, T> callable;

        Task(Function<C, T> callable) {
            this.callable = callable;
        }

        @SuppressWarnings("unchecked")
        public void run(KeyContext context) {
            Holder<Task<T>> next = new Holder<>(this);
            Task<T> task;
            while ((task = next.reset()) != null) {
                try {
                    task.complete(task.callable.apply(context.ctx));
                } catch (Exception e) {
                    completeExceptionally(e);
                }
                contexts.computeIfPresent(context.key, (k, ctx) -> {
                    ctx.tasks.poll();
                    if (ctx.tasks.isEmpty()) {
                        return null;
                    }
                    Task<T> t = (Task<T>) ctx.tasks.peek();
                    next.accept(t);
                    return ctx;
                });
            }
        }
    }
}
