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
import net.jcip.annotations.NotThreadSafe;

/**
 * FIFO linked-list queue.
 *
 * @author Ahseya
 * @param <E> element type
 */
@NotThreadSafe
public class FifoLinkedListQueue<E> implements SimpleQueue<E> {

    private int count;
    private Node<E> head;
    private Node<E> tail;

    FifoLinkedListQueue(int count, Node<E> head, Node<E> tail) {
        this.count = count;
        this.head = head;
        this.tail = tail;
    }

    public FifoLinkedListQueue() {
        this(0, null, null);
    }

    @Override
    public FifoLinkedListQueue add(E e) {
        Node node = new Node<>(e, null);

        if (tail == null) {
            tail = node;
            head = node;

        } else {
            tail.previous = node;
            tail = node;
        }
        count++;
        return this;
    }

    @Override
    public E remove() {
        if (count == 0) {
            throw new NoSuchElementException("Empty queue");
        }

        E element = head.element;
        head.element = null;
        head = head.previous;
        if (head == null) {
            tail = null;
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
        return false;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    static final class Node<E> {

        E element;
        Node previous;

        Node(E element, Node previous) {
            this.element = element;
            this.previous = previous;
        }
    }
}
