/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.liquiddonkey.util.pool;

import java.util.NoSuchElementException;
import java.util.Objects;
import net.jcip.annotations.NotThreadSafe;

/**
 * FIFO queue.
 *
 * @author Ahseya
 * @param <E> element type
 */
@NotThreadSafe
public class FifoArrayQueue<E> implements SimpleQueue<E> {

    private final Object[] items;
    private int putIndex;
    private int takeIndex;
    private int count;

    FifoArrayQueue(Object[] items, int putIndex, int takeIndex, int count) {
        this.items = Objects.requireNonNull(items);
        this.putIndex = putIndex;
        this.takeIndex = takeIndex;
        this.count = count;
    }

    public FifoArrayQueue(int capacity) {
        this(new Object[capacity], 0, 0, 0);
    }

    @Override
    public FifoArrayQueue add(E e) {
        if (count == items.length) {
            throw new IllegalStateException("Full");
        }

        items[putIndex] = e;
        if (++putIndex == items.length) {
            putIndex = 0;
        }
        count++;
        return this;
    }

    @Override
    public E remove() {
        if (count == 0) {
            throw new NoSuchElementException("Empty queue");
        }

        E element = (E) items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length) {
            takeIndex = 0;
        }
        count--;
        return element;
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean isFull() {
        return count == items.length;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }
}
