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

import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pool.
 * <p>
 * Thread safe work pool. Null values not permitted.
 *
 * @author Ahseya
 * @param <T> type
 */
@ThreadSafe
public final class Pool<T> {

    private static final Logger logger = LoggerFactory.getLogger(Pool.class);

    public static <T> Pool<T> newInstance(SimpleQueue<T> queue) {
        logger.trace("<< newInstance()");

        if (queue.contains(null)) {
            throw new IllegalArgumentException("Null entries not permitted");
        }

        ReentrantLock lock = new ReentrantLock(false);
        Pool<T> items = new Pool<>(lock, queue);

        logger.trace(">> newInstance()");
        return items;
    }

    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final SimpleQueue<T> queue;
    private int pending;
    private volatile boolean isShutdown;

    Pool(ReentrantLock lock, Condition notEmpty, SimpleQueue<T> queue, int pending, boolean isShutdown) {
        this.lock = Objects.requireNonNull(lock);
        this.notEmpty = Objects.requireNonNull(notEmpty);
        this.queue = Objects.requireNonNull(queue);
        this.pending = pending;
        this.isShutdown = isShutdown;
    }

    Pool(ReentrantLock lock, SimpleQueue<T> queue) {
        this(lock, lock.newCondition(), queue, 0, false);
    }

    boolean operate(UnaryOperator<T> operator) throws InterruptedException {
        T item = acquire();

        if (item == null) {
            return true;
        }

        T requeue = null;

        try {
            requeue = operator.apply(item);
        } finally {
            release(requeue);
        }

        return false;
    }

    T acquire() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            logger.trace("<< acquire()");

            while (queue.isEmpty()) {
                notEmpty.await();
            }

            T item = queue.remove();
            if (item == null) {
                // Poisoned
                logger.debug("-- acquire() > poisoned");
                queue.add(null);
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

    void shutdown() {
        if (isShutdown) {
            return;
        }

        lock.lock();
        try {
            logger.trace("<< shutdown()");

            isShutdown = true;
            testIsEmpty();

            logger.trace(">> shutdown()");
        } finally {
            lock.unlock();
        }
    }

    void release(T item) {
        lock.lock();
        try {
            logger.trace("<< release() < item: {}", item);

            pending--;
            if (item == null) {
                testIsEmpty();
            } else {
                queue.add(item);
                notEmpty.signal();
            }

            logger.trace(">> release()");
        } finally {
            lock.unlock();
        }
    }

    void testIsEmpty() {
        lock.lock();
        try {
            if (pending == 0 && isShutdown && queue.isEmpty()) {
                // Poison pill
                logger.debug("-- testIsEmpty() > empty, poisoning");
                queue.add(null);
                notEmpty.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}
