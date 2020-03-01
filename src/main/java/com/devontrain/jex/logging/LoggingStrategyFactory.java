package com.devontrain.jex.logging;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class LoggingStrategyFactory {

    private final ReentrantReadWriteLock reentrantReadWriteLock;
    private FileLoggingStrategy fileLoggingStrategy;
    private ConsoleLoggingStrategy consoleLoggingStrategy;

    public LoggingStrategyFactory() {
        this.reentrantReadWriteLock = new ReentrantReadWriteLock(true);
    }

    public FileLoggingStrategy createFileLoggingStrategy(String fileName) {
        return assignLoggingStrategyToFieldAndReturnValue(fileLoggingStrategy, () -> new FileLoggingStrategy(fileName));
    }

    public ConsoleLoggingStrategy createDevNullLoggingStrategy() {
        return assignLoggingStrategyToFieldAndReturnValue(consoleLoggingStrategy, ConsoleLoggingStrategy::new);
    }

    private <T> T assignLoggingStrategyToFieldAndReturnValue(T loggingStrategyField, Supplier<T> loggingStrategyConstructor) {
        reentrantReadWriteLock.readLock().lock();
        try {
            if (null == loggingStrategyField) {
                try {
                    reentrantReadWriteLock.writeLock().lock();
                    loggingStrategyField = loggingStrategyConstructor.get();
                } finally {
                    reentrantReadWriteLock.writeLock().unlock();
                }
            }
            return loggingStrategyField;
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }
}