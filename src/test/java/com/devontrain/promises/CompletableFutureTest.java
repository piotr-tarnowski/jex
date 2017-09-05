package com.devontrain.promises;

import com.devontrain.experimental.AsyncAssertionInspector;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 25.05.16.
 */
public class CompletableFutureTest {

    @Test
    public void testGetPromiseFromDeferred() {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture(123);
        future.thenAccept( i -> assertNotNull(i) );
        assertTrue(future.isDone());
    }

    @Test
    public void testResolveDeferredSimple() throws Exception {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        Integer result = 123;
        future.complete(result);

        assertEquals(result, future.get());
    }

    @Test
    public void testResolveDeferredAsync() throws Exception {

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(()->123);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        future.thenAccept( i -> {
            countDownLatch.countDown();
        });

        try {
            countDownLatch.await(10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //NOOP
        }
        assertTrue(future.isDone());
        assertEquals(new Integer(123), future.get());

    }


    @Test
    public void testResolveDeferredAsyncHandler() throws Exception {

        AtomicInteger numberOfLastHandler = new AtomicInteger();
        AsyncAssertionInspector inspector = new AsyncAssertionInspector();

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync( () -> 123 );
        future.thenApply(
                f -> {
                        inspector.assertNotNull(f);
                        inspector.assertTrue(f instanceof Integer);
                        numberOfLastHandler.incrementAndGet();
                        return Optional.of(Integer.toString(f));
                }
                ).thenApply(optional -> {
                    inspector.assertNotNull(optional);
                    inspector.assertTrue(optional.isPresent());
                    inspector.assertTrue(optional.get() instanceof String);
                    numberOfLastHandler.incrementAndGet();
                    return optional.orElse("");
                }).thenApply(/*(*/s/*, chain)*/ -> {
//                    inspector.assertFalse(chain.isCanceled());
                    numberOfLastHandler.incrementAndGet();
                    return s;
                }).thenApply(currier.apply(numberOfLastHandler))//curried
                        .thenAccept(s -> {
                            inspector.assertNotNull(s);
                            inspector.assertEquals("123", s);
                            numberOfLastHandler.incrementAndGet();
                        }
        );


        inspector.await(10, 100, future);

        assertTrue(future.isDone());
        assertEquals(new Integer(123), future.get());
        assertEquals(5, numberOfLastHandler.get());
    }

    @Test
    public void testResolveDeferredAsyncHandlerWithCancel() throws Exception {

        AtomicInteger numberOfLastHandler = new AtomicInteger();
        AsyncAssertionInspector inspector = new AsyncAssertionInspector();

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 123);

        future.thenApply(
                f -> {
                        inspector.assertNotNull(f);
                        inspector.assertTrue(f instanceof Integer);
                        numberOfLastHandler.incrementAndGet();
                        return Optional.of(Integer.toString(f));
                }
        ).thenApply(optional -> {
            inspector.assertNotNull(optional);
            inspector.assertTrue(optional.isPresent());
            inspector.assertTrue(optional.get() instanceof String);
            numberOfLastHandler.incrementAndGet();
            return optional.orElse("");
        }).thenApply(/*(*/s/*, chain)*/ -> {
//            inspector.assertFalse(chain.isCanceled());
//            chain.cancel();
            numberOfLastHandler.incrementAndGet();
            if(true)
            throw new IllegalStateException();
            return s;
        }).thenApply(currier.apply(numberOfLastHandler))//curried
                .thenAccept(s -> {
                    inspector.assertNotNull(s);
                    inspector.assertEquals("123", s);
                    numberOfLastHandler.incrementAndGet();
                });

        inspector.await(10, 100, future);

        assertTrue(future.isDone());
        assertEquals(new Integer(123), future.get());
        assertEquals(3, numberOfLastHandler.get());

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testResolveDeferredAsyncHandlerWithException() throws Throwable {

        AtomicInteger numberOfLastHandler = new AtomicInteger();
        AsyncAssertionInspector inspector = new AsyncAssertionInspector();

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 123);

        CompletableFuture<Void> completableFuture = future.thenAccept(
                f -> {
                    if (true) throw new UnsupportedOperationException("!!!");
                }
        ).exceptionally(throwable -> {
            inspector.assertNotNull(throwable);
            numberOfLastHandler.incrementAndGet();
            throw (UnsupportedOperationException) throwable.getCause();
        });

        inspector.await(10, 100, future);

        assertTrue(future.isDone());
        try {
            assertEquals(new Integer(123), completableFuture.get());
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            throw e.getCause();
        }
        assertEquals(1, numberOfLastHandler.get());

    }

    //currying
    private static final Function<AtomicInteger, Function<String, String>> currier = i -> s -> process(s, i);

    private static String process(String s, AtomicInteger numberOfLastHandler) {
        numberOfLastHandler.incrementAndGet();
        return s + "";
    }

    //currying is always returning function
    //partial application is always returning result (each time at least one argument have to be fixed/bind)
    //example of binding: currier.apply(numberOfLastHandler)
    //the function returned from above is a partial application example its invocation will return result not another function :-)
    //Function(a) = Bifunction(a, "FIXED")

}