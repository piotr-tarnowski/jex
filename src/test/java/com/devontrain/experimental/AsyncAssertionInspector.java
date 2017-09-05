package com.devontrain.experimental;

import org.junit.Assert;

import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 05.05.16.
 */
public class AsyncAssertionInspector {

    private Optional<Throwable> exception = Optional.empty();
    //inCatchBlock( () -> );

    public void assertNotNull(Object o) {
        inCatchBlock(() -> Assert.assertNotNull(o));
    }

    public void assertTrue(boolean b) {
        inCatchBlock(() -> Assert.assertTrue(b));

    }

    public void assertFalse(boolean b) {
        inCatchBlock(() -> Assert.assertFalse(b));
    }

    public void assertEquals(Object o1, Object o2) {
        inCatchBlock(() -> Assert.assertEquals(o1, o2));
    }

    private final void inCatchBlock(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException re) {
            this.exception = Optional.of(re);
        } catch (Throwable e) {
            this.exception = Optional.of(e);
        }
    }

    public void await(int tries, int time, Future... future) {
        int i = 0;
        while (!future[0].isDone() && i < tries) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                //NOOP
                e.printStackTrace();
            }
            i++;
        }

        exception.ifPresent(e -> {
            if(e instanceof Error){
                throw (Error)e;
            }else {
                throw new AssertionError(e);
            }
        });

    }

}
