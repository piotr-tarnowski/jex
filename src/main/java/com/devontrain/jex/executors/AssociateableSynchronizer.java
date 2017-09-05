package com.devontrain.jex.executors;


import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 09.08.17.
 */
public interface AssociateableSynchronizer<A extends Associate<? extends Context>> {

    @SuppressWarnings("unchecked")
    default CompletableFuture<?> runInSync(Consumer<A> consumer) {
        Context ctx = (Context) this;
        return ctx.runInSync(new tasks.FutureTask(consumer));
    }
}
