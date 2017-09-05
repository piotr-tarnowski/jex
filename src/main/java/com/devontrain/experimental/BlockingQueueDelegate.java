package com.devontrain.experimental;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 15.06.16.
 */
public abstract class BlockingQueueDelegate<E> implements BlockingQueue<E> {

    private final BlockingQueue<E> origin;

    public BlockingQueueDelegate(BlockingQueue origin) {
        this.origin = origin;
    }

    @Override
    public boolean add(E e) {
        return origin.add(e);
    }

    @Override
    public boolean offer(E e) {
        return origin.offer(e);
    }

    @Override
    public E remove() {
        return origin.remove();
    }

    @Override
    public E poll() {
        return origin.poll();
    }

    @Override
    public E element() {
        return origin.element();
    }

    @Override
    public E peek() {
        return origin.peek();
    }

    @Override
    public void put(E e) throws InterruptedException {
        origin.put(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return origin.offer(e);
    }

    @Override
    public E take() throws InterruptedException {
        return origin.take();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return origin.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return origin.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return origin.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return origin.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return origin.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return origin.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return origin.retainAll(c);
    }

    @Override
    public void clear() {
        origin.clear();
    }

    @Override
    public int size() {
        return origin.size();
    }

    @Override
    public boolean isEmpty() {
        return origin.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return origin.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return origin.iterator();
    }

    @Override
    public Object[] toArray() {
        return origin.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return origin.toArray(a);
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return origin.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return origin.drainTo(c, maxElements);
    }
}
