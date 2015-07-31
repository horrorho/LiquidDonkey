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
import com.github.horrorho.liquiddonkey.cloud.store.DataWriter;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.util.SyncSupplier;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.jcip.annotations.NotThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Donkey.
 *
 * @author Ahseya
 */
@NotThreadSafe
class Donkey implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Donkey.class);

    private final HttpAgent agent;
    private final ChunksClient chunksClient;
    private final SyncSupplier<ChunkServer.StorageHostChunkList> chunks;
    private final StoreManager storeManager;
    private final SignatureManager signatureManager;
    private final Consumer<Map<ICloud.MBSFile, Outcome>> outcomesConsumer;
    private final AtomicReference<Exception> fatal;
    private volatile boolean isAlive;
    private volatile HttpUriRequest request;

    Donkey(
            HttpAgent agent,
            ChunksClient chunksClient,
            SyncSupplier<ChunkServer.StorageHostChunkList> chunks,
            StoreManager storeManager,
            SignatureManager signatureManager,
            Consumer<Map<ICloud.MBSFile, Outcome>> outcomesConsumer,
            AtomicReference<Exception> fatal,
            boolean isAlive,
            HttpUriRequest request) {

        this.agent = Objects.requireNonNull(agent);
        this.chunksClient = Objects.requireNonNull(chunksClient);
        this.chunks = Objects.requireNonNull(chunks);
        this.storeManager = Objects.requireNonNull(storeManager);
        this.signatureManager = Objects.requireNonNull(signatureManager);
        this.outcomesConsumer = Objects.requireNonNull(outcomesConsumer);
        this.fatal = Objects.requireNonNull(fatal);
        this.isAlive = isAlive;
        this.request = Objects.requireNonNull(request);
    }

    Donkey(
            HttpAgent ioExecutor,
            ChunksClient chunksClient,
            SyncSupplier<ChunkServer.StorageHostChunkList> chunks,
            StoreManager storeManager,
            SignatureManager signatureManager,
            Consumer<Map<ICloud.MBSFile, Outcome>> outcomesConsumer,
            int retryCount,
            long retryDelayMs,
            AtomicReference<Exception> fatal) {

        this(
                ioExecutor,
                chunksClient,
                chunks,
                storeManager,
                signatureManager,
                outcomesConsumer,
                fatal,
                true,
                null);
    }

    @Override
    public void run() {
        logger.trace("<< run()");

        try {
            while (isAlive) {
                if (Thread.currentThread().isInterrupted()) {
                    logger.warn("-- run() > interrupt flag set");
                    throw new InterruptedException("Interrupted");
                }

                Exception ex = fatal.get();
                if (ex != null) {
                    logger.warn("-- run() > fatal: {}", ex);
                    break;
                }

                ChunkServer.StorageHostChunkList chunkList = chunks.get();

                if (chunkList == null) {
                    logger.debug("-- run() > depleted");
                    break;
                }

                process(chunkList);
            }
        } catch (IOException | InterruptedException | RuntimeException ex) {
            fatal.compareAndSet(null, ex);
            logger.warn("-- run() > exception: ", ex);
        }

        logger.trace(">> run() > fatal: {} isAlive: {}", fatal == null ? null : fatal.get().getMessage(), isAlive);
    }

    Map<ICloud.MBSFile, Outcome> process(ChunkServer.StorageHostChunkList chunkList)
            throws IOException, InterruptedException {

        logger.trace("<< process() < chunk list: {}", chunkList.getHostInfo().getUri());

        Map<ICloud.MBSFile, Outcome> outcomes = doProcess(chunkList);

        if (outcomes != null) {
            outcomesConsumer.accept(outcomes);
        }

        logger.trace(">> process() >  outcomes: {}", outcomes == null ? null : outcomes.size());
        return outcomes;
    }

    Map<ICloud.MBSFile, Outcome> doProcess(ChunkServer.StorageHostChunkList chunkList)
            throws IOException, InterruptedException {

        while (isAlive) {
            request = chunksClient.get(chunkList);
            byte[] data = fetch();

            if (data == null || !isAlive) { 
                break;
            }

            Map<ByteString, DataWriter> writers = decode(chunkList, data);

            if (writers == null) { 
                break;
            }

            return write(writers);
        }        
        
        Set<ByteString> failedSignatures = storeManager.fail(chunkList);
        return signatureManager.fail(failedSignatures);
    }

    byte[] fetch() throws IOException {
        logger.trace("<< fetch() < request: {}", request.getURI());

        byte[] data;
        try {
            data = agent.execute(client -> client.execute(request, chunksClient.responseHandler()));

        } catch (UnknownHostException ex) {
            logger.warn("-- fetch() > exception: {}", ex);
            data = null;

        } catch (HttpResponseException ex) {
            logger.warn("-- fetch() > exception: {}", ex);

            if (ex.getStatusCode() == 401) {
                // Unauthorized.
                throw ex;
            }
            data = null;
        }

        logger.trace(">> fetch() > data: {}", data == null ? null : data.length);
        return data;
    }

    Map<ByteString, DataWriter> decode(ChunkServer.StorageHostChunkList chunkList, byte[] data)
            throws IOException, InterruptedException {

        logger.trace("<< decode() < chunk list: {} data: {}", chunkList.getHostInfo().getUri(), data.length);

        Map<ByteString, DataWriter> writers;
        try {
            writers = storeManager.put(chunkList, data);

        } catch (BadDataException ex) {
            logger.warn("-- decodeWrite() > exception: ", ex);
            writers = null;
        }

        logger.trace(">> decode() > outcomes: {}", writers);
        return writers;
    }

    Map<ICloud.MBSFile, Outcome> write(Map<ByteString, DataWriter> writers) throws IOException, InterruptedException {
        try {
            return signatureManager.write(writers);
        } finally {
            writers.values().forEach(writer -> {
                try {
                    writer.close();
                } catch (IOException ex) {
                    logger.warn("-- writer() > exception on close: {}", ex);
                }
            });
        }
    }

    void kill() {
        isAlive = false;
        HttpUriRequest local = request;
        if (local != null) {
            try {
                local.abort();
            } catch (UnsupportedOperationException ex) {
                logger.warn("-- kill() > exception: {}", ex);
            }
        }
    }
}
