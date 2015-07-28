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

import com.github.horrorho.liquiddonkey.cloud.donkey.Donkey;
import com.github.horrorho.liquiddonkey.cloud.donkey.Track;
import com.github.horrorho.liquiddonkey.util.pool.WorkPools;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
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

    public static ConcurrentEngine from(int threads, int staggerMs, long executorTimeoutMs) {
        return new ConcurrentEngine(threads, staggerMs, executorTimeoutMs);
    }

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentEngine.class);

    private final int threads;
    private final int staggerMs;
    private final long executorTimeoutMs;

    ConcurrentEngine(int threads, int staggerMs, long executorTimeoutMs) {
        this.threads = threads;
        this.staggerMs = staggerMs;
        this.executorTimeoutMs = executorTimeoutMs;
    }

    void execute(WorkPools<Track, Donkey> pools, AtomicReference<Exception> fatal) {
        logger.trace("<< execute()");
        logger.debug("-- execute() - executor fired up");
        ExecutorService executor = Executors.newCachedThreadPool();

        List<Future<?>> futuresFetch = new ArrayList<>();
        List<Future<?>> futuresDecodeWrite = new ArrayList<>();

        try {
            for (int i = 0; i < threads; i++) {
                futuresFetch.add(executor.submit(Runner.newInstance(pools, Track.FETCH)));
                futuresDecodeWrite.add(executor.submit(Runner.newInstance(pools, Track.DECODE_WRITE)));
                TimeUnit.MILLISECONDS.sleep(staggerMs);
            }

            logger.debug("-- execute() > runners running: {}", threads);
            executor.shutdown();

            logger.debug("-- execute() > awaiting termination");
            boolean timedOut = executor.awaitTermination(executorTimeoutMs, TimeUnit.MILLISECONDS);

            if (timedOut) {
                logger.warn("-- execute() > timed out");
            } else {
                logger.debug("-- execute() > completed");
            }
        } catch (InterruptedException ex) {
            logger.warn("-- execute() > interrupted: {}", ex);
            fatal.compareAndSet(null, ex);

        } finally {
            logger.debug("-- execute() > shutting down");
            executor.shutdownNow();

            logger.debug("-- execute() > fetch futures: {}", futuresFetch);
            logger.debug("-- execute() > decodeWriter futures: {}", futuresDecodeWrite);
            logger.debug("-- execute() > has shut down");
        }
        logger.trace(">> execute()");
    }

//    void out(ICloud.MBSFile file, WriterResult result) {
//        std.println("\t" + file.getDomain() + " " + file.getRelativePath() + " " + result);
//
//        WriterResult old = results.put(file, result);
//        if (old != null) {
//            logger.warn("-- out() > overwritten result: {} file: {}", old, file);
//        }
//    }
}
// TODO trace exception handling