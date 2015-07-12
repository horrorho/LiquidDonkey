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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorkPoolTest. Concurrent test.
 *
 * @author Ahseya
 */
public class WorkPoolTest {

    private static final Logger logger = LoggerFactory.getLogger(WorkPoolTest.class);

    private final long timeoutMs = 30000;
    private final int threads = 4;
    private final int max = 100;

    private ExecutorService executor;
    private List<Integer> list;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        executor = Executors.newCachedThreadPool();
        list = Collections.synchronizedList(new ArrayList<>());
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testOperate() throws Exception {
        List<Integer> integers = IntStream.range(0, max).mapToObj(Integer::valueOf).collect(Collectors.toList());
        WorkPool<Integer> pool = new WorkPool<>(false, new FifoLinkedListQueue().addAll(integers)).shutdown();

        AtomicInteger pending = new AtomicInteger(0);

        Supplier<Runnable> runnables = () -> () -> {
            logger.trace("<< runnable()");
            try {
                pending.incrementAndGet();
                boolean isEnd = false;
                while (!isEnd) {
                    isEnd = pool.operate(t -> {
                        list.add(t);
                        return ++t < max ? t : null;
                    });
                }
                pending.decrementAndGet();
            } catch (InterruptedException ex) {
                logger.warn("-- runnable() > exception: {}", ex);
            }
        };

        Stream.generate(runnables).limit(threads).forEach(executor::submit);
        executor.shutdown();
        executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        executor.shutdownNow();

        assertThat("Interrupted", pending.get(), is(0));
        Collections.sort(list);

        List<Integer> expected = IntStream.range(0, max)
                .flatMap(i -> IntStream.range(i, max))
                .mapToObj(Integer::valueOf)
                .collect(Collectors.toList());
        Collections.sort(expected);
        assertThat(list, is(expected));
    }

    @Test
    public void testAcquireRelease() throws Exception {
        List<Integer> integers = IntStream.range(0, max).mapToObj(Integer::valueOf).collect(Collectors.toList());
        WorkPool<Integer> pool = new WorkPool<>(false, new FifoLinkedListQueue().addAll(integers)).shutdown();

        AtomicInteger pending = new AtomicInteger(0);

        Supplier<Runnable> runnables = () -> () -> {
            logger.trace("<< runnable()");
            try {
                pending.incrementAndGet();
                Integer i;
                while ((i = pool.acquire()) != null) {
                    list.add(i);
                    pool.release(null);
                }
                pending.decrementAndGet();
            } catch (InterruptedException ex) {
                logger.warn("-- runnable() > exception: {}", ex);
            }
        };

        Stream.generate(runnables).limit(threads).forEach(executor::submit);
        executor.shutdown();
        executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        executor.shutdownNow();

        assertThat("Interrupted", pending.get(), is(0));
        Collections.sort(list);
        assertThat(list, is(integers));
    }

    @Test
    public void testAdd() throws Exception {
        List<Integer> integers = IntStream.range(0, max).mapToObj(Integer::valueOf).collect(Collectors.toList());
        WorkPool<Integer> pool = new WorkPool<>(false, new FifoLinkedListQueue());

        AtomicInteger pending = new AtomicInteger(0);

        Supplier<Runnable> runnables = () -> () -> {
            logger.trace("<< runnable()");
            try {
                pending.incrementAndGet();
                Integer i;
                while ((i = pool.acquire()) != null) {
                    list.add(i);
                    pool.release(null);
                }
                pending.decrementAndGet();
            } catch (InterruptedException ex) {
                logger.warn("-- runnable() > exception: {}", ex);
            }
        };

        Stream.generate(runnables).limit(threads).forEach(executor::submit);
        for (Integer integer : integers) {
            pool.add(integer);
        }
        pool.shutdown();

        executor.shutdown();
        executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        executor.shutdownNow();

        assertThat("Interrupted", pending.get(), is(0));
        Collections.sort(list);
        assertThat(list, is(integers));
    }

    @Test
    public void testShutdown() throws Exception {
        List<Integer> integers = Arrays.asList();
        WorkPool<Integer> pool = new WorkPool<>(false, new FifoLinkedListQueue());
 
        AtomicInteger pending = new AtomicInteger(0);

        Supplier<Runnable> runnables = () -> () -> {
            logger.trace("<< runnable()");
            try {
                synchronized (pending) {
                    pending.incrementAndGet();
                    pending.notify();
                }
                Integer i;
                while ((i = pool.acquire()) != null) {
                    list.add(i);
                    pool.release(null);
                }
                pending.decrementAndGet();
            } catch (InterruptedException ex) {
                logger.warn("-- runnable() > exception: {}", ex);
            }
        };

        Stream.generate(runnables).limit(threads).forEach(executor::submit);
        for (Integer integer : integers) {
            pool.add(integer);
        }

        while (true) {
            synchronized(pending) {
                if (pending.get() == threads) {
                    break;
                }
                pending.wait(timeoutMs);
            }
        }
        
        
        pool.shutdown();

        executor.shutdown();
        executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        executor.shutdownNow();

        assertThat("Interrupted", pending.get(), is(0));
        Collections.sort(list);
        assertThat(list, is(integers));
    }

    @Test
    public void testShutdownException() throws Exception {
        List<Integer> integers = IntStream.range(0, max).mapToObj(Integer::valueOf).collect(Collectors.toList());
        WorkPool<Integer> pool = new WorkPool<>(false, new FifoLinkedListQueue().addAll(integers)).shutdown();
        exception.expect(IllegalStateException.class);
        pool.add(0);
    }

    //TODO test shutdown
}
