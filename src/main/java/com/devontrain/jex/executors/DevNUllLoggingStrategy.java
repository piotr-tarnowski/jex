package com.devontrain.jex.executors;

import java.io.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 20.12.17.
 */
public class DevNUllLoggingStrategy extends LoggingStrategy {
    private PrintStream out = System.out;
    private final PrintStream err = System.err;

    public DevNUllLoggingStrategy() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    void initialzie(String name) {

    }

    private void close() {
    }

    public void info(String message,
                     Object... args) {
        out.println(message);
    }

    public void error(String message,
                      Object... args) {
        err.println(message);
    }

}
