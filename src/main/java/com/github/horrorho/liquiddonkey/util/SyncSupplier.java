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
package com.github.horrorho.liquiddonkey.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * SyncSupplier.
 *
 * @author Ahseya
 * @param <T> item type
 */
@ThreadSafe
public final class SyncSupplier<T> {

    public static <T> SyncSupplier<T> from(Collection<T> collection) {
        return from(collection, false);
    }

    public static <T> SyncSupplier<T> from(Collection<T> collection, boolean fair) {
        return new SyncSupplier(new ReentrantLock(fair), new ArrayList<>(collection), collection.size());
    }

    private final Lock lock;
    private final ArrayList<T> items;

    SyncSupplier(Lock lock, ArrayList<T> items, int index) {
        this.lock = lock;
        this.items = items;
        this.index = index;
    }

    @GuardedBy("lock")
    private int index;

    public T get() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            if (index == 0) {
                return null;
            }

            index--;
            T item = items.get(index);
            items.set(index, null);
            return item;
        } finally {
            lock.unlock();
        }
    }
}
