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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * WorkPoolTest. Concurrent test.
 *
 * @author Ahseya
 */
public class WorkPoolsTest {

    private static final Logger logger = LoggerFactory.getLogger(WorkPoolsTest.class);
    public static final Marker test = MarkerFactory.getMarker("TEST");

    public static enum Pool {

        ONE, TWO, THREE
    }

    private final int threads = 4;
    private final int max = 1000;
    private final long timeoutMs = 30000;

    private final AtomicInteger pending = new AtomicInteger(0);
    private final Map<Pool, Pool> next;

    public WorkPoolsTest() {
        Pool[] values = Pool.values();
        int length = values.length;
        this.next = IntStream.range(0, length)
                .mapToObj(Integer::valueOf)
                .collect(Collectors.toMap(i -> values[i], i -> values[(i + 1) % length]));
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testFromFactoryMethodsEmpty() throws Exception {
        Map<Pool, Collection<Integer>> items = new EnumMap<>(Pool.class);

        WorkPools<Pool, Integer> pools = WorkPools.from(Pool.class, items);
        assertThat(pools.isFair(), is(false));
        assertThat(pools.isDepleted(), is(true));

        WorkPools<Pool, Integer> poolsNotFair = WorkPools.from(Pool.class, items, false);
        assertThat(poolsNotFair.isFair(), is(false));
        assertThat(poolsNotFair.isDepleted(), is(true));

        WorkPools<Pool, Integer> poolsFair = WorkPools.from(Pool.class, items, true);
        assertThat(poolsFair.isFair(), is(false));
        assertThat(poolsFair.isDepleted(), is(true));
    }

    @Test
    public void testFromFactoryMethodNotEmpty() throws Exception {
        Map<Pool, Collection<Integer>> items = new EnumMap<>(Pool.class);
        items.put(Pool.ONE, Arrays.asList(1));
        items.put(Pool.TWO, Arrays.asList(1, 2));
        items.put(Pool.THREE, Arrays.asList(1, 2, 3));

        WorkPools<Pool, Integer> pools = WorkPools.from(Pool.class, items);
        assertThat(pools.acquire(Pool.ONE), is(1));
        assertThat(pools.acquire(Pool.TWO), is(1));
        assertThat(pools.acquire(Pool.TWO), is(2));
        assertThat(pools.acquire(Pool.THREE), is(1));
        assertThat(pools.acquire(Pool.THREE), is(2));
        assertThat(pools.acquire(Pool.THREE), is(3));
        assertThat(pools.isDepleted(), is(false));
    }

    @Test
    public void testFromFactoryMethodNullException() throws Exception {
        Map<Pool, Collection<String>> items = new EnumMap<>(Pool.class);
        List<String> bad = Arrays.asList("one", "two", "three", null);
        items.put(Pool.ONE, bad);

        exception.expect(IllegalArgumentException.class);
        WorkPools<Pool, String> pools = WorkPools.from(Pool.class, items);
    }

    @Test
    public void testAcquireRelease() throws Exception {
        Map<Pool, Collection<Integer>> items = new EnumMap<>(Pool.class);
        items.put(Pool.ONE, Arrays.asList(1, 2));
        items.put(Pool.TWO, Arrays.asList(3, 4));

        WorkPools<Pool, Integer> pools;

        // Remove all.
        pools = WorkPools.from(Pool.class, items);
        assertThat(pools.acquire(Pool.ONE), is(1));
        assertThat(pools.acquire(Pool.TWO), is(3));
        assertThat(pools.acquire(Pool.ONE), is(2));
        assertThat(pools.acquire(Pool.TWO), is(4));
        assertThat(pools.isDepleted(), is(false));

        // Requeue reordered
        pools.release(Pool.ONE, 3);
        pools.release(Pool.TWO, 1);
        pools.release(Pool.ONE, 4);
        pools.release(Pool.TWO, 2);
        assertThat(pools.isDepleted(), is(false));

        // Remove all.
        assertThat(pools.acquire(Pool.ONE), is(3));
        assertThat(pools.acquire(Pool.TWO), is(1));
        assertThat(pools.acquire(Pool.ONE), is(4));
        assertThat(pools.acquire(Pool.TWO), is(2));
        assertThat(pools.isDepleted(), is(false));

        // Partial dispose.
        pools.release(Pool.ONE, 1);
        pools.release(Pool.TWO, 2);
        pools.release(Pool.ONE, null);
        pools.release(Pool.TWO, null);

        // Remove all.
        assertThat(pools.acquire(Pool.ONE), is(1));
        assertThat(pools.acquire(Pool.TWO), is(2));
        assertThat(pools.isDepleted(), is(false));

        // Dispose all.
        pools.release(Pool.ONE, null);
        pools.release(Pool.TWO, null);
        assertThat(pools.isDepleted(), is(true));

        // Poisoned. Non-blocking null.
        assertThat(pools.acquire(Pool.ONE), is(nullValue()));
        assertThat(pools.acquire(Pool.TWO), is(nullValue()));
        assertThat(pools.acquire(Pool.ONE), is(nullValue()));
        assertThat(pools.acquire(Pool.TWO), is(nullValue()));
        assertThat(pools.isDepleted(), is(true));
    }

    @Test
    public void testProcess() throws Exception {

        Map<Pool, Collection<Integer>> items = new EnumMap<>(Pool.class);
        items.put(Pool.ONE, Arrays.asList(1));

        WorkPools<Pool, Integer> pools;
        boolean isDepleted;

        // dispose()
        pools = WorkPools.from(Pool.class, items);

        isDepleted = pools.process(Pool.ONE, i -> {
            assertThat(i, is(1));
            return Release.requeue(Pool.TWO, i + 1);
        });
        assertThat(isDepleted, is(false));

        isDepleted = pools.process(Pool.TWO, i -> {
            assertThat(i, is(2));
            return Release.dispose();
        });
        assertThat(isDepleted, is(true));

        // requeue(null)
        pools = WorkPools.from(Pool.class, items);

        isDepleted = pools.process(Pool.ONE, i -> {
            assertThat(i, is(1));
            return Release.requeue(Pool.TWO, i + 1);
        });
        assertThat(isDepleted, is(false));

        isDepleted = pools.process(Pool.TWO, i -> {
            assertThat(i, is(2));
            return Release.requeue(null);
        });
        assertThat(isDepleted, is(true));

        // requeue(Pool.THREE, null)
        pools = WorkPools.from(Pool.class, items);

        isDepleted = pools.process(Pool.ONE, i -> {
            assertThat(i, is(1));
            return Release.requeue(Pool.TWO, i + 1);
        });
        assertThat(isDepleted, is(false));

        isDepleted = pools.process(Pool.TWO, i -> {
            assertThat(i, is(2));
            return Release.requeue(Pool.THREE, null);
        });
        assertThat(isDepleted, is(true));

        // null
        pools = WorkPools.from(Pool.class, items);

        isDepleted = pools.process(Pool.ONE, i -> {
            assertThat(i, is(1));
            return Release.requeue(Pool.TWO, i + 1);
        });
        assertThat(isDepleted, is(false));

        isDepleted = pools.process(Pool.TWO, i -> {
            assertThat(i, is(2));
            return null;
        });
        assertThat(isDepleted, is(true));
    }

    @Test
    public void testConcurrentAquireDispose() throws Exception {
        List<Integer> integers = IntStream.range(0, max).mapToObj(Integer::valueOf).collect(Collectors.toList());
        Map<Pool, Collection<Integer>> items = new EnumMap<>(Pool.class);
        Stream.of(Pool.values()).forEach(e -> items.put(e, integers));
        WorkPools<Pool, Integer> pools = WorkPools.from(Pool.class, items);

        Map<Pool, List<Integer>> lists = Stream.of(Pool.values())
                .collect(Collectors.toConcurrentMap(Function.identity(), e -> Collections.synchronizedList(new ArrayList<>())));

        ExecutorService executor = Executors.newCachedThreadPool();

        Stream.of(Pool.values())
                .flatMap(e -> Stream.generate(() -> acquireDisposeRunnable(pools, e, lists.get(e))).limit(threads))
                .forEach(executor::submit);

        executor.shutdown();
        executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        executor.shutdownNow();

        assertThat(pending.get(), is(0));
        Stream.of(Pool.values()).forEach(e -> {
            Collections.sort(lists.get(e));
            assertThat(lists.get(e), is(integers));
        });
        assertThat(pools.isDepleted(), is(true));
    }

    public Runnable acquireDisposeRunnable(WorkPools<Pool, Integer> pools, Pool pool, List<Integer> list) {
        return () -> {
            logger.trace(test, "<< runnable() < pool: {}", pool);
            try {
                pending.incrementAndGet();
                Integer i;
                while ((i = pools.acquire(pool)) != null) {
                    list.add(i);
                    pools.release(pool, null);
                }
                logger.trace(test, "-- runnable > done");
            } catch (InterruptedException | RuntimeException ex) {
                logger.warn(">> runnable() > exception: {}", ex);
            } finally {
                pending.decrementAndGet();
                logger.trace(test, ">> runnable() > pool: {}", pool);
            }
        };
    }

    @Test
    public void testConcurrentAquireRequeue() throws Exception {
        List<Integer> integers = IntStream.range(0, max).mapToObj(Integer::valueOf).collect(Collectors.toList());
        Map<Pool, Collection<Integer>> items = new EnumMap<>(Pool.class);
        Stream.of(Pool.values()).forEach(e -> items.put(e, Arrays.asList(max)));
        WorkPools<Pool, Integer> pools = WorkPools.from(Pool.class, items);

        Map<Pool, List<Integer>> lists = Stream.of(Pool.values())
                .collect(Collectors.toConcurrentMap(Function.identity(), e -> Collections.synchronizedList(new ArrayList<>())));

        ExecutorService executor = Executors.newCachedThreadPool();

        Stream.of(Pool.values())
                .flatMap(e -> Stream.generate(() -> acquireRequeueRunnable(pools, e, lists.get(e))).limit(threads))
                .forEach(executor::submit);

        executor.shutdown();
        executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        executor.shutdownNow();

        assertThat(pending.get(), is(0));
        Stream.of(Pool.values()).forEach(e -> {
            Collections.sort(lists.get(e));
            assertThat(lists.get(e), is(integers));
        });
        assertThat(pools.isDepleted(), is(true));
    }

    public Runnable acquireRequeueRunnable(WorkPools<Pool, Integer> pools, Pool pool, List<Integer> list) {
        return () -> {
            logger.trace(test, "<< runnable() < pool: {}", pool);
            try {
                pending.incrementAndGet();
                Integer i;
                while ((i = pools.acquire(pool)) != null) {
                    list.add(--i);
                    pools.release(pool, i > 0 ? i : null);
                }
                logger.trace(test, "-- runnable > done");
            } catch (InterruptedException | RuntimeException ex) {
                logger.warn(">> runnable() > exception: {}", ex);
            } finally {
                pending.decrementAndGet();
                logger.trace(test, ">> runnable() > pool: {}", pool);
            }
        };
    }

    @Test
    public void testConcurrentAquireCrossRequeue() throws Exception {
        List<Integer> integers = IntStream.range(0, max).mapToObj(Integer::valueOf).collect(Collectors.toList());
        Map<Pool, Collection<Integer>> items = new EnumMap<>(Pool.class);
        items.put(Pool.ONE, Arrays.asList(max));
        WorkPools<Pool, Integer> pools = WorkPools.from(Pool.class, items);

        List<Integer> list = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newCachedThreadPool();

        Stream.of(Pool.values())
                .flatMap(e -> Stream.generate(() -> acquireCrossRequeueRunnable(pools, e, list)).limit(threads))
                .forEach(executor::submit);

        executor.shutdown();
        executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        executor.shutdownNow();

        assertThat(pending.get(), is(0));
        Collections.sort(list);
        assertThat(list, is(integers));
        assertThat(pools.isDepleted(), is(true));
    }

    public Runnable acquireCrossRequeueRunnable(WorkPools<Pool, Integer> pools, Pool pool, List<Integer> list) {
        return () -> {
            logger.trace(test, "<< runnable() < pool: {}", pool);
            try {
                pending.incrementAndGet();
                Integer i;
                while ((i = pools.acquire(pool)) != null) {
                    list.add(--i);
                    Pool nextPool = next.get(pool);
                    Integer item = i > 0 ? i : null;
                    logger.debug(test, "-- runnable > release, pool: {} item: {}", nextPool, item);
                    pools.release(nextPool, item);
                }
                logger.trace(test, "-- runnable > done");
            } catch (InterruptedException | RuntimeException ex) {
                logger.warn(">> runnable() > exception: {}", ex);
            } finally {
                pending.decrementAndGet();
                logger.trace(test, ">> runnable() > pool: {}", pool);
            }
        };
    }

    @Test
    public void testConcurrentAquireCrossProcess() throws Exception {
        List<Integer> integers = IntStream.range(0, max).mapToObj(Integer::valueOf).collect(Collectors.toList());
        Map<Pool, Collection<Integer>> items = new EnumMap<>(Pool.class);
        items.put(Pool.ONE, Arrays.asList(max));
        WorkPools<Pool, Integer> pools = WorkPools.from(Pool.class, items);

        List<Integer> list = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newCachedThreadPool();

        Stream.of(Pool.values())
                .flatMap(e -> Stream.generate(() -> acquireCrossProcessRunnable(pools, e, list)).limit(threads))
                .forEach(executor::submit);

        executor.shutdown();
        executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        executor.shutdownNow();

        assertThat(pending.get(), is(0));
        Collections.sort(list);
        assertThat(list, is(integers));
        assertThat(pools.isDepleted(), is(true));
    }

    public Runnable acquireCrossProcessRunnable(WorkPools<Pool, Integer> pools, Pool pool, List<Integer> list) {
        return () -> {
            logger.trace(test, "<< runnable() < pool: {}", pool);
            try {
                pending.incrementAndGet();
                while (!pools.process(pool, i -> {
                    list.add(--i);
                    Pool nextPool = next.get(pool);
                    Integer item = i > 0 ? i : null;
                    if (item == null) {
                        logger.debug(test, "-- runnable > dispose, pool: {} item: {}", nextPool, item);
                        return Release.dispose();
                    } else {
                        logger.debug(test, "-- runnable > requeue, pool: {} item: {}", nextPool, item);
                        return Release.requeue(nextPool, item);
                    }
                })) {
                }
                logger.trace(test, "-- runnable > done");
            } catch (InterruptedException | RuntimeException ex) {
                logger.warn(">> runnable() > exception: {}", ex);
            } finally {
                pending.decrementAndGet();
                logger.trace(test, ">> runnable() > pool: {}", pool);
            }
        };
    }
}
