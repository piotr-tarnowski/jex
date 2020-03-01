package com.devontrain.jex.logging;

import java.io.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 20.12.17.
 */
public class ConsoleLoggingStrategy extends FileLoggingStrategy {

    private static final PrintStream OUT = System.out;
    private static final PrintStream ERR = System.err;

    public ConsoleLoggingStrategy() {
        super("");
    }

    public void info(String message,
                     Object... args) {
        OUT.println(message);
    }

    public void error(String message,
                      Object... args) {
        ERR.println(message);
    }

}
