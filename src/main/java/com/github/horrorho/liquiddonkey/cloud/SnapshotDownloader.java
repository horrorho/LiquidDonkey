/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies from the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions from the Software.
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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.FileErrorException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.settings.config.EngineConfig;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Snapshot download.
 * <p>
 * Concurrently downloads snapshots via Donkeys.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class SnapshotDownloader {

    /**
     * Returns a new instance.
     *
     * @param factory not null
     * @param config not null
     * @return new instance, not null
     */
    public static SnapshotDownloader newInstance(
            DonkeyFactory factory,
            EngineConfig config) {

        return new SnapshotDownloader(
                factory,
                config.threadCount(),
                config.threadStaggerDelay(),
                config.retryCount());
    }

    private static final Logger logger = LoggerFactory.getLogger(SnapshotDownloader.class);

    private final DonkeyFactory factory;
    private final int threads;
    private final int staggerDelayMs;
    private final int retryCount;

    SnapshotDownloader(DonkeyFactory factory, int threads, int staggerDelayMs, int retryCount) {
        this.factory = Objects.requireNonNull(factory);
        this.threads = threads;
        this.staggerDelayMs = staggerDelayMs;
        this.retryCount = retryCount;
    }

    /**
     * Executes donkeys.
     * <p>
     * Downloads the specified snapshot concurrently. Entries are removed from signatures as the execution proceeds.
     * Results are return as a boolean signature map: true for success, false for fail.
     *
     * @param http, not null
     * @param snapshot, not null
     * @param signatures, not null
     * @param printer, not null
     * @return results, not null
     * @throws AuthenticationException
     * @throws IOException
     */
    public ConcurrentMap<Boolean, ConcurrentMap<ByteString, Set<ICloud.MBSFile>>> execute(
            Http http,
            Snapshot snapshot,
            ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures,
            Printer printer
    ) throws AuthenticationException, IOException {

        logger.trace("<< execute() < snapshot: {} signatures: {}", snapshot, signatures.size());

        ConcurrentMap<Boolean, ConcurrentMap<ByteString, Set<ICloud.MBSFile>>> results = new ConcurrentHashMap<>();
        results.put(Boolean.TRUE, new ConcurrentHashMap<>());
        results.put(Boolean.FALSE, new ConcurrentHashMap<>());
        // TODO empty signatures
        int count = 0;
        while (count++ < retryCount && !signatures.isEmpty()) {
            logger.debug("-- execute() : count: {}/{} signatures: {}", count, retryCount, signatures.size());
            signatures.putAll(results.get(false));
            results.get(false).clear();
            doExecute(http, snapshot, signatures, results, printer);
        }

        logger.trace(">> execute()");
        return results;
    }

    ConcurrentMap<Boolean, ConcurrentMap<ByteString, Set<ICloud.MBSFile>>> doExecute(
            Http http,
            Snapshot snapshot,
            ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures,
            ConcurrentMap<Boolean, ConcurrentMap<ByteString, Set<ICloud.MBSFile>>> results,
            Printer printer
    ) throws AuthenticationException, IOException {

        logger.trace("<< doExecute() < signatures: {}", signatures.size());

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Future<Boolean>> futures
                = Stream.generate(() -> factory.from(http, snapshot, signatures, results, printer))
                .limit(threads)
                .map(executor::submit)
                .peek(this::stagger)
                .collect(Collectors.toList());
        // Threads all fired up.
        executor.shutdown();

        // All done.
        for (Future<Boolean> future : futures) {
            error(future);
        }

        if (!results.get(Boolean.FALSE).isEmpty()) {
            logger.warn("-- doExecute() > failures: {}", results.get(Boolean.FALSE).size());
        }

        logger.trace(">> doExecute()");
        return results;
    }

    <T> T error(Future<T> future) throws AuthenticationException, IOException {
        T t = null;
        // Exception handling.
        try {
            t = future.get();
        } catch (CancellationException ex) {
            logger.warn("-- error() > exception: {}", ex);
            throw new IllegalStateException("Cancelled");
        } catch (InterruptedException ex) {
            logger.warn("-- error() > exception: {}", ex);
            throw new IllegalStateException("Interrupted");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException) {
                if (cause instanceof AuthenticationException) {
                    throw (AuthenticationException) cause;
                }

                if (cause instanceof FileErrorException) {
                    throw (FileErrorException) cause;
                }

                throw (IOException) cause;
            }
            logger.warn("-- error() > suppressed exception: ", ex);
        }
        return t;
    }

    <T> T stagger(T t) {
        try {
            // Stagger to avoid triggering sensitive anti-flood protection with high thread counts,
            // or to disrupt the initial coinciding download/ decrypt phases between threads.
            TimeUnit.MILLISECONDS.sleep(staggerDelayMs);
            return t;
        } catch (InterruptedException ex) {
            throw new IllegalStateException("Interrupted");
        }
    }
}
