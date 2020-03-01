package com.devontrain.jex.logging;

public interface LoggingStrategy {
    void info(String message,
              Object... args);

    void error(String message,
               Object... args);
}
