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

import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * SimpleQueue.
 *
 * @author Ahseya
 * @param <E> element type
 */
public interface SimpleQueue<E> {

    /**
     * Inserts the specified element into this queue.
     *
     * @param e the element to add, may be null
     * @return this instance, not null
     * @throws IllegalStateException if the element cannot be added at this time due to capacity restrictions
     */
    SimpleQueue add(E e);

    /**
     * Retrieves and removes the next element from this queue.
     *
     * @return the next element, may be null
     * @throws NoSuchElementException if this queue is empty
     */
    E remove();

    /**
     * Returns true if this queue contains no elements.
     *
     * @return true if this queue contains no elements
     */
    boolean isEmpty();

    /**
     * Returns true if this queue is at full capacity and unable to accept more elements.
     *
     * @return true if this queue is at full capacity and unable to accept more elements
     */
    boolean isFull();

    /**
     * Returns the number of elements in this collection.
     *
     * @return the number of elements in this collection
     */
    int size();

    /**
     * Inserts all the elements from the specified collection into this queue.
     *
     * @param collection, not null
     * @return this instance, not null
     * @throws IllegalStateException if the element cannot be added at this time due to capacity restrictions
     */
    default SimpleQueue addAll(Collection<E> collection) {
        collection.stream().forEach(this::add);
        return this;
    }

    /**
     * Returns true if this queue contains the specified element.
     *
     * @param e, may be null
     * @return true if this queue contains the specified element
     */
    default boolean contains(E e) {
        boolean hasElement = false;

        for (int i = 0; i < size(); i++) {
            E element = remove();
            hasElement = hasElement
                    ? true
                    : element == e;
            add(element);
        }

        return hasElement;
    }
}
