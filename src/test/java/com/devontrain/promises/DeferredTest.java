package com.devontrain.promises;

import com.devontrain.experimental.AsyncAssertionInspector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 25.05.16.
 */
public class DeferredTest {

    ExecutorService executor;

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }


    @Test
    public void testGetPromiseFromDeferred() {
        Deferred<Integer> deferred = new Deferred<>();
        deferred.promise((promise) -> assertNotNull(promise));
    }

    @Test
    public void testResolveDeferredSimple() throws Exception {
        Deferred<Integer> deferred = new Deferred<>();

        Integer result = 123;
        RunnableFuture<Integer> future = deferred.resolve(result);
        future.run();

        assertEquals(result, future.get());
    }

    @Test
    public void testResolveDeferredCallable() throws Exception {
        Deferred<Integer> deferred = new Deferred<>();

        RunnableFuture<Integer> future = deferred.resolve(() -> 123);
        future.run();

        assertEquals(new Integer(123), future.get());
    }

    @Test
    public void testResolveDeferredAsync() throws Exception {
        Deferred<Integer> deferred = new Deferred<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        deferred.promise(promise -> promise.then(f -> {
            countDownLatch.countDown();
        }));
        assertEquals(1, countDownLatch.getCount());

        RunnableFuture<Integer> future = deferred.resolve(() -> 123);
        executor.execute(future);
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

        Deferred<Integer> deferred = new Deferred<>();
        deferred.promise(promise -> promise.then(
                f -> {
                    try {
                        inspector.assertNotNull(f);
                        inspector.assertTrue(f.get() instanceof Integer);
                        return Optional.of(Integer.toString(f.get()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }finally {
                        numberOfLastHandler.incrementAndGet();
                    }
                    return Optional.<String>empty();
                }
                ).then(optional -> {
                    inspector.assertNotNull(optional);
                    inspector.assertTrue(optional.isPresent());
                    inspector.assertTrue(optional.get() instanceof String);
                    numberOfLastHandler.incrementAndGet();
                    return optional.orElse("");
                }).then((s, chain) -> {
                    inspector.assertFalse(chain.isCanceled());
                    numberOfLastHandler.incrementAndGet();
                    return s;
                }).then(currier.apply(numberOfLastHandler))//curried
                        .then(s -> {
                            inspector.assertNotNull(s);
                            inspector.assertEquals("123", s);
                            numberOfLastHandler.incrementAndGet();
                        })
        );


        RunnableFuture<Integer> future = deferred.resolve(() -> 123);
        executor.execute(future);

        inspector.await(10, 100, future);

        assertTrue(future.isDone());
        assertEquals(new Integer(123), future.get());
        assertEquals(5, numberOfLastHandler.get());


    }

    @Test
    public void testResolveDeferredAsyncHandlerWithCancel() throws Exception {

        AtomicInteger numberOfLastHandler = new AtomicInteger();
        AsyncAssertionInspector inspector = new AsyncAssertionInspector();

        Deferred<Integer> deferred = new Deferred<>();
        deferred.promise(promise -> promise.then(
                f -> {
                    try {
                        inspector.assertNotNull(f);
                        inspector.assertTrue(f.get() instanceof Integer);
                        return Optional.of(Integer.toString(f.get()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }finally {
                        numberOfLastHandler.incrementAndGet();
                    }
                    return Optional.<String>empty();
                }
        ).then(optional -> {
            inspector.assertNotNull(optional);
            inspector.assertTrue(optional.isPresent());
            inspector.assertTrue(optional.get() instanceof String);
            numberOfLastHandler.incrementAndGet();
            return optional.orElse("");
        }).then((s, chain) -> {
            inspector.assertFalse(chain.isCanceled());
            chain.cancel();
            numberOfLastHandler.incrementAndGet();
            return s;
        }).then(currier.apply(numberOfLastHandler))//curried
                .then(s -> {
                    inspector.assertNotNull(s);
                    inspector.assertEquals("123", s);
                    numberOfLastHandler.incrementAndGet();
                }));

        RunnableFuture<Integer> future = deferred.resolve(() -> 123);
        executor.execute(future);

        inspector.await(10, 100, future);

        assertTrue(future.isDone());
        assertEquals(new Integer(123), future.get());
        assertEquals(3, numberOfLastHandler.get());

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testResolveDeferredAsyncHandlerWithException() throws Throwable {

        AtomicInteger numberOfLastHandler = new AtomicInteger();
        AsyncAssertionInspector inspector = new AsyncAssertionInspector();

        Deferred<Integer> deferred = new Deferred<>();
        deferred.promise(promise -> promise.then(
            f -> {
                if (true) throw new UnsupportedOperationException("!!!");
            }
        ));

        RunnableFuture<Integer> future = deferred.resolve(() -> 123);
        executor.execute(future);
        inspector.await(10, 100, future);

        assertTrue(future.isDone());
        try {
            assertEquals(new Integer(123), future.get());
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            throw e.getCause();
        }
        assertEquals(0, numberOfLastHandler.get());

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