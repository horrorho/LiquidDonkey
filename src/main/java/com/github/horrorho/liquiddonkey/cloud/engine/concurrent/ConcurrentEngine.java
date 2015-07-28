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
package com.github.horrorho.liquiddonkey.cloud.engine.concurrent;

import com.github.horrorho.liquiddonkey.cloud.store.DataWriter;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.iofunction.IOConsumer;
import com.github.horrorho.liquiddonkey.settings.config.EngineConfig;
import com.github.horrorho.liquiddonkey.util.pool.WorkPools;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConcurrentEngine.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public class ConcurrentEngine {

    public static ConcurrentEngine from(EngineConfig config) {
        return from(
                config.threadCount(),
                config.threadStaggerDelayMs(),
                config.retryCount(),
                180000 // TODO
        );
    }

    public static ConcurrentEngine from(int threads, int staggerMs, int retryCount, long executorTimeoutMs) {
        return new ConcurrentEngine(threads, staggerMs, retryCount, executorTimeoutMs);
    }

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentEngine.class);

    private final int threads;
    private final int staggerMs;
    private final int retryCount;
    private final long executorTimeoutMs;

    ConcurrentEngine(int threads, int staggerMs, int retryCount, long executorTimeoutMs) {
        this.threads = threads;
        this.staggerMs = staggerMs;
        this.retryCount = retryCount;
        this.executorTimeoutMs = executorTimeoutMs;
    }

    public boolean execute(
            HttpClient client,
            StoreManager storeManager,
            AtomicReference<Exception> fatal,
            Consumer<Set<ByteString>> failures,
            IOConsumer<Map<ByteString, DataWriter>> completed
    ) throws InterruptedException {

        // Donkey Factory.
        DonkeyFactory factory = DonkeyFactory.from(client, storeManager, retryCount, fatal, failures, completed);

        // Populate WorkPools with FetchDonkeys on the FETCH track.
        Map<Track, List<Donkey>> donkies = storeManager.chunkListList().stream()
                .map(factory::fetchDonkey)
                .collect(Collectors.groupingBy(list -> Track.FETCH));
        WorkPools<Track, Donkey> pools = WorkPools.from(Track.class, donkies);

        return execute(pools, fatal);
    }

    boolean execute(WorkPools<Track, Donkey> pools, AtomicReference<Exception> fatal) throws InterruptedException {
        logger.trace("<< execute()");

        boolean isTimedOut;
        List<Future<?>> futuresFetch = new ArrayList<>();
        List<Future<?>> futuresDecodeWrite = new ArrayList<>();

        ExecutorService executor = Executors.newCachedThreadPool();
        logger.debug("-- execute() > executor created");

        try {
            for (int i = 0; i < threads; i++) {
                futuresFetch.add(executor.submit(Runner.newInstance(pools, Track.FETCH)));
                futuresDecodeWrite.add(executor.submit(Runner.newInstance(pools, Track.DECODE_WRITE)));
                logger.debug("-- execute() > thread submitted: {}", i);
                TimeUnit.MILLISECONDS.sleep(staggerMs);
            }

            logger.debug("-- execute() > runners running: {}", threads);
            executor.shutdown();

            logger.debug("-- execute() > awaiting termination, timeout (ms): {}", executorTimeoutMs);
            isTimedOut = !executor.awaitTermination(executorTimeoutMs, TimeUnit.MILLISECONDS);
            
            if (isTimedOut) {
                logger.warn("-- execute() > timed out");
            } else {
                logger.debug("-- execute() > completed");
            }
            
            logger.trace(">> execute() > timed out: {}", isTimedOut);
            return isTimedOut;

        } catch (InterruptedException ex) {
            logger.warn("-- execute() > interrupted: {}", ex);
            fatal.compareAndSet(null, ex); 
            throw (ex);

        } finally {
            logger.debug("-- execute() > shutting down");
            executor.shutdownNow();

            logger.debug("-- execute() > fetch futures: {}", futuresFetch);
            logger.debug("-- execute() > decodeWriter futures: {}", futuresDecodeWrite);
            logger.debug("-- execute() > has shut down");
        }
    }
}
// TODO trace exception handling
