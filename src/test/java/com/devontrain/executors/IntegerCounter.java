package com.devontrain.executors;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class IntegerCounter {

    private int counter;

    public IntegerCounter() {

    }

    public IntegerCounter(int counter) {
        this.counter = counter;
    }


    public int incrementAndGet() {
        return ++counter;
    }

    public int getAndIncrement() {
        int tmp = counter;
        counter++;
        return tmp;
    }

    public int get() {
        return counter;
    }

    public int getAndDecrement() {
        int tmp = counter;
        counter--;
        return tmp;
    }
}
