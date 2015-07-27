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

/**
 * ToDo.
 *
 * @author Ahseya
 * @param <E> enum key type
 * @param <T> item type
 */
public final class ToDo<E extends Enum<E>, T> {

    public static <E extends Enum<E>, T> ToDo<E, T> dispose() {
        return toDispose;
    }

    public static <E extends Enum<E>, T> ToDo<E, T> requeue(T item) {
        return new ToDo<>(null, item);
    }

    public static <E extends Enum<E>, T> ToDo<E, T> requeue(E pool, T item) {
        return new ToDo<>(pool, item);
    }

    private static final ToDo toDispose = new ToDo<>(null, null);

    private final E pool;
    private final T item;

    ToDo(E pool, T item) {
        this.pool = pool;
        this.item = item;
    }

    public E pool() {
        return pool;
    }

    public T item() {
        return item;
    }

    @Override
    public String toString() {
        return "Release{" + "pool=" + pool + ", item=" + item + '}';
    }
}
