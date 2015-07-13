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

import com.github.horrorho.liquiddonkey.settings.Markers;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Stream;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * WorkPool.
 * <p>
 * Lightweight thread safe work pools. Null values not permitted.
 *
 * @author Ahseya
 * @param <E> enum pool type
 * @param <T> item type
 */
@ThreadSafe
public final class WorkPools<E extends Enum<E>, T> {

    private static final Logger logger = LoggerFactory.getLogger(WorkPools.class);
    public static final Marker marker = MarkerFactory.getMarker(Markers.POOL);

    public static <E extends Enum<E>, T> WorkPools<E, T> from(Class<E> poolKeyType, Map<E, List<T>> items) {
        return WorkPools.from(poolKeyType, items, false);
    }

    public static <E extends Enum<E>, T> WorkPools<E, T>
            from(Class<E> poolKeyType, Map<E, List<T>> items, boolean fairness) {

        Objects.requireNonNull(poolKeyType);
        Objects.requireNonNull(items);

        logger.trace(marker, "<< from() < key: {} item count: {} fairness: {}",
                poolKeyType.getSimpleName(), items.size(), fairness);

        ReentrantLock lock = new ReentrantLock();
        Map<E, Condition> notEmptyConditions = new EnumMap<>(poolKeyType);
        Map<E, SimpleQueue<T>> pools = new EnumMap<>(poolKeyType);

        Stream.of(poolKeyType.getEnumConstants()).forEach(key -> {
            notEmptyConditions.put(key, lock.newCondition());
            pools.put(key, new FifoLinkedListQueue());
        });

        int total = items.entrySet().stream()
                .mapToInt(entry -> {
                    if (entry.getValue().contains(null)) {
                        throw new IllegalArgumentException("Null items not permitted");
                    }
                    pools.get(entry.getKey()).addAll(entry.getValue());
                    return entry.getValue().size();
                })
                .sum();

        if (total == 0) {
            logger.debug(marker, "-- from() > empty collection, poisoning pools");
            pools.values().stream().forEach(q -> q.add(null));
        }

        WorkPools<E, T> worksPool = new WorkPools<>(lock, notEmptyConditions, pools, 0, total == 0);

        logger.trace(marker, ">> from() > key: {} fairness: {} total: {}", poolKeyType.getSimpleName(), fairness, total);
        return worksPool;
    }

    private final ReentrantLock lock;
    private final Map<E, Condition> notEmptyConditions;
    private final Map<E, SimpleQueue<T>> pools;
    private volatile int pending;
    private volatile boolean isDepleted;

    WorkPools(
            ReentrantLock lock, Map<E, Condition> notEmptyConditions,
            Map<E, SimpleQueue<T>> pools,
            int pending,
            boolean isDepleted) {

        this.lock = Objects.requireNonNull(lock);
        this.notEmptyConditions = Objects.requireNonNull(notEmptyConditions);
        this.pools = Objects.requireNonNull(pools);
        this.pending = pending;
        this.isDepleted = isDepleted;
    }

    public boolean process(E pool, Function<T, Release<E, T>> function) throws InterruptedException {
        T item = acquire(Objects.requireNonNull(pool));

        if (item == null) {
            return true;
        }

        Release<E, T> action = null;

        try {
            action = function.apply(item);
        } finally {
            if (action == null) {
                release(pool, null);
            } else {
                release(action.pool() == null ? pool : action.pool(), action.item());
            }
        }
        return isDepleted;
    }

    T acquire(E pool) throws InterruptedException {
        logger.trace(marker, "<< acquire() < pool: {}", pool);
        if (isDepleted) {
            logger.trace(marker, ">> acquire() > depleted, pool: {} item: null", pool);
            return null;
        }
        lock.lockInterruptibly();
        try {
            SimpleQueue<T> queue = pools.get(pool);
            Condition notEmpty = notEmptyConditions.get(pool);

            while (queue.isEmpty()) {
                notEmpty.await();
            }

            T item = queue.remove();

            if (item == null) {
                logger.debug(marker, "-- acquire() > poisoned pool: {}", pool);
                queue.add(null);
                notEmpty.signal();
            } else {
                pending++;
            }
            logger.trace(marker, ">> acquire() > pool: {} item: {}", pool, item);
            return item;
        } finally {
            lock.unlock();
        }
    }

    void release(E pool, T item) {
        logger.trace(marker, "<< release() < pool: {} item: {}", pool, item);
        lock.lock();
        try {
            pending--;
            SimpleQueue<T> queue = pools.get(pool);
            Condition notEmpty = notEmptyConditions.get(pool);

            if (item == null) {
                logger.debug(marker, "-- release() > disposed, pool: {} item: {}", pool, item);

                if (pending == 0 && queue.isEmpty() && pools.values().stream().allMatch(SimpleQueue::isEmpty)) {
                    logger.debug(marker, "-- release() > depleted, poisoning pools");
                    pools.values().stream().forEach(q -> q.add(null));
                    isDepleted = true;
                    notEmptyConditions.values().forEach(Condition::signal);
                }
            } else {
                logger.debug(marker, "-- release() > pool: {} item: {}", pool, item);
                queue.add(item);
                notEmpty.signal();
            }
            logger.trace(marker, ">> release() > requeued, pool: {} item: {}", pool, item);
        } finally {
            lock.unlock();
        }
    }

    public boolean isFair() {
        return lock.isFair();
    }

    public boolean isDepleted() {
        return isDepleted;
    }
}
// TODO each pool must have a worker or it will remain blocked
// TODO POOL logger marker
