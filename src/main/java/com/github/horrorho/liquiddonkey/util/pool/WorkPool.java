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
 * WorkPool.
 * <p>
 * Thread safe work pool. Null values not permitted.
 *
 * @author Ahseya
 * @param <T> type
 */
@ThreadSafe
public final class WorkPool<T> {

    private static final Logger logger = LoggerFactory.getLogger(WorkPool.class);

    public static <T> WorkPool<T> newInstance(SimpleQueue<T> queue) {
        return newInstance(false, queue);
    }

    public static <T> WorkPool<T> newInstance(boolean fairness, SimpleQueue<T> queue) {
        logger.trace("<< newInstance()");

        if (queue.contains(null)) {
            throw new IllegalArgumentException("Null entries not permitted");
        }

        WorkPool<T> items = new WorkPool<>(fairness, queue);

        logger.trace(">> newInstance()");
        return items;
    }

    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final SimpleQueue<T> queue;
    private int pending;
    private volatile boolean isShutdown;

    WorkPool(ReentrantLock lock, Condition notEmpty, SimpleQueue<T> queue, int pending, boolean isShutdown) {
        this.lock = Objects.requireNonNull(lock);
        this.notEmpty = Objects.requireNonNull(notEmpty);
        this.queue = Objects.requireNonNull(queue);
        this.pending = pending;
        this.isShutdown = isShutdown;
    }

    WorkPool(ReentrantLock lock, SimpleQueue<T> queue) {
        this(lock, lock.newCondition(), queue, 0, false);
    }

    WorkPool(boolean fairness, SimpleQueue<T> queue) {
        this(new ReentrantLock(fairness), queue);
    }

    public boolean operate(UnaryOperator<T> operator) throws InterruptedException {
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

    public WorkPool add(T item) throws InterruptedException {
        logger.trace("<< add() < {}", item);
        if (isShutdown) {
            throw new IllegalStateException("Shutdown");
        }

        lock.lockInterruptibly();
        try {

            queue.add(item);
            notEmpty.signal();

            logger.trace(">> add()");
            return this;
        } finally {
            lock.unlock();
        }
    }

    T acquire() throws InterruptedException {
        logger.trace("<< acquire()");
        lock.lockInterruptibly();
        try {

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

    WorkPool shutdown() {
        logger.trace("<< shutdown()");
        if (!isShutdown) {
            lock.lock();
            try {

                isShutdown = true;
                testDepleted();

                logger.trace(">> shutdown()");
            } finally {
                lock.unlock();
            }
        }
        return this;
    }

    void release(T item) {
        logger.trace("<< release() < item: {}", item);
        lock.lock();
        try {

            pending--;
            if (item == null) {
                testDepleted();
            } else {
                queue.add(item);
                notEmpty.signal();
            }

            logger.trace(">> release()");
        } finally {
            lock.unlock();
        }
    }

    void testDepleted() {
        lock.lock();
        try {
            if (pending == 0 && isShutdown && queue.isEmpty()) {
                // Poison pill
                logger.debug("-- testDepleted() > depleted, poisoning");
                queue.add(null);
                notEmpty.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}
// TODO external predicate for shutdown?
