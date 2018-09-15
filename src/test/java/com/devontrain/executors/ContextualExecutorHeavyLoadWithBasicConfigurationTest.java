package com.devontrain.executors;

import com.devontrain.jex.executors.ContextualExecutorBuilder;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.ConcurrentHashMap;

import static com.devontrain.jex.executors.ContextClosingStrategy.LEAVE_CONTEXT_AFTER_LAST_TASK;
import static com.devontrain.jex.executors.ContextResolvingStrategy.RESOLVE_CONTEXT_IN_CALLER_THREAD;

public class ContextualExecutorHeavyLoadWithBasicConfigurationTest extends ContextualExecutorHeavyLoadTestBase {

    @BeforeAll
    void setUp() {
        super.setUp();
        ContextualExecutorBuilder<Integer, TestContext> builder
                = new ContextualExecutorBuilder<>();
        contexts = new ConcurrentHashMap<>();
        builder
                .loggingStrategy(loggingStrategy)
                .contextResolvingStrategy(RESOLVE_CONTEXT_IN_CALLER_THREAD)
                .contextRemovingStrategy(LEAVE_CONTEXT_AFTER_LAST_TASK)
                .contextResolver(ContextualExecutorHeavyLoadTestBase.TestContext::new)
                .contexts(contexts)
                .joinTimeOut(1000)
                .taskLimit(100)
                .executor(executor);
        kexpool = builder.build("kexpool");
    }


//    public static void main(String... args) throws Exception {
//        ContextualExecutorHeavyLoadWithBasicConfigurationTest test = new ContextualExecutorHeavyLoadWithBasicConfigurationTest();
//        for (int i = 0; i < 1000000; i++) {
//            test.setUp();
//            long ms = System.currentTimeMillis();
//            test.allTaskWillBeFinishedInReasonableTime();
//            System.err.println(i + " --> " + (System.currentTimeMillis() - ms) + " ms!");
//            test.cleanUp();
//            test.tearDown();
//        }
//    }
}