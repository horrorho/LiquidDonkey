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
package com.github.horrorho.liquiddonkey.cloud;

import com.github.horrorho.liquiddonkey.util.Fifo;
import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ItemPool.
 * <p>
 * Thread safe work pool.
 *
 * @author Ahseya
 * @param <T> type
 */
@ThreadSafe
public class ItemPool<T> {

    private static final Logger logger = LoggerFactory.getLogger(ItemPool.class);

    public static <T> ItemPool<T> newInstance(Collection<T> collection) {
        logger.trace("<< newInstance() < item count: {}", collection.size());

        T[] array = (T[]) collection.toArray(new Object[collection.size()]);

        ReentrantLock lock = new ReentrantLock(false);
        Condition notEmpty = lock.newCondition();
        Fifo<T> buffer = new Fifo<>(array.length, array);

        ItemPool<T> items = new ItemPool<>(lock, notEmpty, buffer, 0);

        logger.trace(">> newInstance() > item count: {}", buffer.size());
        return items;
    }

    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final Fifo<T> buffer;
    private int pending;

    ItemPool(ReentrantLock lock, Condition notEmpty, Fifo<T> buffer, int pending) {
        this.lock = lock;
        this.notEmpty = notEmpty;
        this.buffer = buffer;
        this.pending = pending;
    }

    public boolean function(UnaryOperator<T> operator) throws InterruptedException {
        logger.trace("<< function()");

        T item = acquire();

        boolean isEmpty;
        if (item == null) {
            isEmpty = true;
        } else {
            try {
                release(operator.apply(item));
                isEmpty = false;
            } catch (Throwable th) {
                logger.warn("-- function() > item: {} throwable: {}", item, th);
                // Do not retry
                release(null);
                throw th;
            }
        }

        logger.trace(">> function() > isEmpty: {}", isEmpty);
        return isEmpty;
    }

    T acquire() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            logger.trace("<< acquire()");

            while (buffer.isEmpty()) {
                notEmpty.await();
            }

            T item = buffer.pull();

            if (item == null) {
                // Poisoned
                logger.debug("-- acquire() > poisoned");
                buffer.push(null);
                notEmpty.signal();
            } else {
                pending++;
            }

            logger.trace(">> acquire() > item: {}", item);
            return item;
        } finally {
            lock.unlock();
        }
    }

    void release(T item) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            logger.trace("<< release() < item: {}", item);

            pending--;
            if (item == null) {
                if (pending == 0 && buffer.isEmpty()) {
                    // Poison pill
                    logger.debug("-- release() > to poison");
                    buffer.push(null);
                    notEmpty.signal();
                }
            } else {
                buffer.push(item);
                notEmpty.signal();
            }

            logger.trace(">> release()");
        } finally {
            lock.unlock();
        }
    }
}
