package com.devontrain.experimental;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 04.05.16.
 */
public class QTest {

    Q Q;

    @Before
    public void setUp() {
        Q = new Q();
    }

    @After
    public void tearDown() {
        Q.shutdown();
    }

    @Test
    public void testDeferFromQ() {
        Deferred deferred = Q.defer();
        assertNotNull(deferred);
    }

    @Test
    public void testGetPromiseFromDeferred() {
        Deferred deferred = Q.defer();
        assertNotNull(deferred);

        Promise promise = deferred.getPromise();
        assertNotNull(promise);
    }

    @Test
    public void testResolveDeferredSimple() throws Exception {
        Deferred<Integer> deferred = Q.defer();
        assertNotNull(deferred);

        Integer result = 123;
        deferred.resolve(result);

        assertEquals(result, deferred.getPromise().asFuture().get());
    }

    @Test
    public void testResolveDeferredCallable() throws Exception {
        Deferred<Integer> deferred = Q.defer();
        assertNotNull(deferred);

        deferred.resolve(() -> 123);

        assertEquals(new Integer(123), deferred.getPromise().asFuture().get());
    }

    @Test
    public void testResolveDeferredAsync() throws Exception {
        Deferred<Integer> deferred = Q.defer();
        assertNotNull(deferred);


        PromiseImpl<Integer> promise = deferred.getPromise();
        assertNotNull(promise);

        deferred.resolve(() -> 123);

        int i = 0;
        Future<Integer> future = promise.asFuture();
        while (!future.isDone() && i < 10) {
            Thread.sleep(100);
            i++;
        }
        assertTrue(future.isDone());
        assertEquals(new Integer(123), future.get());


    }

    @Test
    public void testResolveDeferredAsyncHandler() throws Exception {

        AsyncAssertionInspector inspector = new AsyncAssertionInspector();

        Deferred<Integer> deferred = Q.defer();
        assertNotNull(deferred);

        PromiseImpl<Integer> promise = deferred.getPromise();
        assertNotNull(promise);
        AtomicInteger numberOfLastHandler = new AtomicInteger();


        promise.then(
                f -> {
                    try {
                        inspector.assertNotNull(f);
                        inspector.assertTrue(f.get() instanceof Integer);
                        return Optional.of(Integer.toString(f.get()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    numberOfLastHandler.incrementAndGet();
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
                });


        deferred.resolve(() -> 123);

        Future<Integer> future = promise.asFuture();
        inspector.await(10, 100, future);

        assertTrue(future.isDone());
        assertEquals(new Integer(123), deferred.getPromise().asFuture().get());
        assertEquals(4, numberOfLastHandler.get());


    }

    @Test
    public void testResolveDeferredAsyncHandlerWithCancel() throws Exception {

        AsyncAssertionInspector inspector = new AsyncAssertionInspector();

        Deferred<Integer> deferred = Q.defer();
        assertNotNull(deferred);

        PromiseImpl<Integer> promise = deferred.getPromise();
        assertNotNull(promise);
        AtomicInteger numberOfLastHandler = new AtomicInteger();

        promise.then(
                f -> {
                    try {
                        inspector.assertNotNull(f);
                        inspector.assertTrue(f.get() instanceof Integer);
                        return Optional.of(Integer.toString(f.get()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    numberOfLastHandler.incrementAndGet();
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
                });


        deferred.resolve(() -> 123);

        Future<Integer> future = promise.asFuture();
        inspector.await(10, 100, future);

        assertTrue(future.isDone());
        assertEquals(new Integer(123), deferred.getPromise().asFuture().get());
        assertEquals(2, numberOfLastHandler.get());

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testResolveDeferredAsyncHandlerWithException() throws Throwable {

        AsyncAssertionInspector inspector = new AsyncAssertionInspector();

        Deferred<Integer> deferred = Q.defer();
        assertNotNull(deferred);

        PromiseImpl<Integer> promise = deferred.getPromise();
        assertNotNull(promise);
        AtomicInteger numberOfLastHandler = new AtomicInteger();
        promise.then(
                f -> {
                    if (true) throw new UnsupportedOperationException("!!!");
                }
        );


        deferred.resolve(() -> 123);

        Future<Integer> future = promise.asFuture();
        inspector.await(10, 100, future);

        assertTrue(future.isDone());
        try {
            assertEquals(new Integer(123), deferred.getPromise().asFuture().get());
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