package com.devontrain.executors;

import com.devontrain.jex.executors.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

import static java.time.Duration.ofSeconds;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ContextualExecutorHeavyLoadTestBase {

    private static final int TEST_TIME_OUT = 1000;
    private static final int TRIES = 100000;
    private static final int CONCURRENCY = 5;
    static ExecutorService executor;
    static ContextualExecutor<Integer, TestContext> kexpool;
    static ConcurrentHashMap<Integer, ContextualExecutorHeavyLoadTestBase.TestContext> contexts;
    static AfterFileLoggingStrategy loggingStrategy = new DevNUllLoggingStrategy();
    List<Future<Integer>> futures = new ArrayList<>();

    void setUp() {
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    void cleanUp() {
        contexts.clear();
        futures.clear();
//        System.err.println(futures + " * " + futures.size());
//        System.err.println(contexts + " * " + contexts.size());
    }

    @AfterAll
    void tearDown() {
        while (!kexpool.isShutdown()) {
            kexpool.shutdownNow();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //NOOP
            }
        }
        while (!executor.isShutdown()) {
            executor.shutdown();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //NOOP
            }
        }
    }

    @Test
    public void allTaskWillBeFinishedInReasonableTime() throws Exception {
        Assertions.assertTimeout(ofSeconds(TEST_TIME_OUT), () -> {
            System.err.println(futures + " x " + futures.size());
            System.err.println(contexts + " x " + contexts.size());
            for (int i = 0; i < TRIES; i++) {
                int tmp = i;
                final int key = i % CONCURRENCY;
                final int increment = i / CONCURRENCY;
                final Future<Integer> future = kexpool.report(key, Function.identity(),
                        ctx -> {
                            IntegerCounter counter = ctx.getCtx();
                            int c = counter.incrementAndGet();
                            if (tmp < 5) {
                                System.out.println(tmp + " @ " + ctx + "#" + counter + " --> " + c);
                            }
                            if (increment != c) {
                                Assertions.fail(new IllegalStateException(counter + " @" + c + "\t" + System.currentTimeMillis() + "\t" + Thread.currentThread().getName() + " --> " + key + " : " + increment));
                                new IllegalStateException(counter + " @" + c + "\t" + System.currentTimeMillis() + "\t" + Thread.currentThread().getName() + " --> " + key + " : " + increment).printStackTrace();
                                System.exit(-1);
                            }
                            return increment;
                        });
                futures.add(future);
            }
            Assertions.assertEquals(TRIES, futures.size());
            for (Future<Integer> future : futures) {
                Assertions.assertNotNull(future.get(TEST_TIME_OUT, TimeUnit.SECONDS));
            }
        });
    }

    protected static class TestContext extends ContextWrapper<Integer, IntegerCounter> {

        public TestContext(ExecutorBase contextualExecutor,
                           Integer key) {
            this(contextualExecutor, key, new IntegerCounter(-1));
        }

        private TestContext(ExecutorBase contextualExecutor,
                            Integer key,
                            IntegerCounter ctx) {
            super(contextualExecutor, key);
            this.ctx = ctx;
        }

        @Override
        public IntegerCounter getCtx() {
            return ctx;
        }
    }
}
