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

import com.github.horrorho.liquiddonkey.cloud.HttpAgent;
import com.github.horrorho.liquiddonkey.cloud.Outcome;
import com.github.horrorho.liquiddonkey.cloud.SignatureManager;
import com.github.horrorho.liquiddonkey.cloud.client.ChunksClient;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.settings.config.EngineConfig;
import com.github.horrorho.liquiddonkey.util.SyncSupplier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import static org.bouncycastle.crypto.tls.ConnectionEnd.client;
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
                1800000 // TODO
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
    private final long retryDelayMs = 1000;
    private final ChunksClient chunksClient = ChunksClient.create();

    ConcurrentEngine(int threads, int staggerMs, int retryCount, long executorTimeoutMs) {
        this.threads = threads;
        this.staggerMs = staggerMs;
        this.retryCount = retryCount;
        this.executorTimeoutMs = executorTimeoutMs;
    }

    public Exception execute(
            HttpAgent agent,
            StoreManager storeManager,
            SignatureManager signatureManager,
            Consumer<Map<ICloud.MBSFile, Outcome>> outcomesConsumer
    ) throws InterruptedException, TimeoutException {

        List<ChunkServer.StorageHostChunkList> chunks = storeManager.chunkListList().stream().collect(Collectors.toList());
        logger.debug("-- execute() > chunks count: {}", chunks.size());

        SyncSupplier<ChunkServer.StorageHostChunkList> syncSupplier = SyncSupplier.from(chunks);
        AtomicReference<Exception> fatal = new AtomicReference(null);
        Supplier<Donkey> donkies = ()
                -> new Donkey(
                        agent,
                        chunksClient,
                        syncSupplier,
                        storeManager,
                        signatureManager,
                        outcomesConsumer,
                        retryCount,
                        retryDelayMs,
                        fatal);

        return execute(donkies, fatal);
    }

    Exception execute(Supplier<Donkey> donkeySupplier, AtomicReference<Exception> fatal)
            throws InterruptedException, TimeoutException {

        logger.trace("<< execute()");

        boolean isTimedOut;
        List<Future<?>> futures = new ArrayList<>();
        List<Donkey> donkies = new ArrayList<>();

        ExecutorService executor = Executors.newCachedThreadPool();
        logger.debug("-- execute() > executor created");

        try {
            for (int i = 0; i < threads; i++) {
                Donkey donkey = donkeySupplier.get();
                donkies.add(donkey);
                futures.add(executor.submit(donkey));

                logger.debug("-- execute() > donkey submitted: {}", i);
                TimeUnit.MILLISECONDS.sleep(staggerMs);
            }

            logger.debug("-- execute() > runners running: {}", threads);
            executor.shutdown();

            logger.debug("-- execute() > awaiting termination, timeout (ms): {}", executorTimeoutMs);
            isTimedOut = !executor.awaitTermination(executorTimeoutMs, TimeUnit.MILLISECONDS);

            if (isTimedOut) {
                logger.warn("-- execute() > timed out");
                throw new TimeoutException("Concurrent engine timed out");
            }

            Exception ex = fatal.get();
            logger.trace(">> execute() > fatal: {}", ex);
            return ex;

        } catch (InterruptedException ex) {
            logger.warn("-- execute() > interrupted: {}", ex);
            throw (ex);

        } finally {
            logger.debug("-- execute() > shutting down");
            executor.shutdownNow();

            // Kill donkies (aborting any http requests in progress).
            donkies.stream().forEach(Donkey::kill);

            long finished = futures.stream().filter(Future::isDone).count();
            long pending = threads - finished;

            logger.debug("-- execute() > runnables, finished: {} pending: {}", finished, pending);
            logger.debug("-- execute() > has shut down");
        }
    }
}
// TODO trace exception handling
// TODO handle java.net.SocketTimeoutException: Read timed out