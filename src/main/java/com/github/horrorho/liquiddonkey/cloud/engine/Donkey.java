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
package com.github.horrorho.liquiddonkey.cloud.engine;

import com.github.horrorho.liquiddonkey.cloud.HttpAgent;
import com.github.horrorho.liquiddonkey.cloud.Outcome;
import com.github.horrorho.liquiddonkey.cloud.SignatureManager;
import com.github.horrorho.liquiddonkey.cloud.client.ChunksClient;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.store.DataWriter;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
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
class Donkey {

    private static final Logger logger = LoggerFactory.getLogger(Donkey.class);

    private final HttpAgent agent;
    private final ChunksClient chunksClient;
    private final StoreManager storeManager;
    private final SignatureManager signatureManager;
    private final int retryCount;
    private final AtomicReference<HttpUriRequest> request;

    Donkey(
            HttpAgent agent,
            ChunksClient chunksClient,
            StoreManager storeManager,
            SignatureManager signatureManager,
            int retryCount,
            AtomicReference<HttpUriRequest> request) {

        this.agent = agent;
        this.chunksClient = chunksClient;
        this.storeManager = storeManager;
        this.signatureManager = signatureManager;
        this.retryCount = retryCount;
        this.request = request;
    }

    Donkey(
            HttpAgent agent,
            ChunksClient chunksClient,
            StoreManager storeManager,
            SignatureManager signatureManager,
            int retryCount) {

        this(agent, chunksClient, storeManager, signatureManager, retryCount, new AtomicReference());
    }

    Map<ICloud.MBSFile, Outcome> process(ChunkServer.StorageHostChunkList chunkList)
            throws InterruptedException, IOException {

        logger.trace("<< process() < chunk list: {}", chunkList.getHostInfo().getUri());

        request.set(chunksClient.get(chunkList));
        int count = 0;

        while (true) {
            Map<ByteString, DataWriter> writers;

            if (request.get() == null || Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Interrupted");
            }

            try {
                byte[] data = agent.execute(client -> client.execute(request.get(), chunksClient.responseHandler()));
                writers = storeManager.put(chunkList, data);

            } catch (HttpResponseException ex) {
                if (ex.getStatusCode() == 401) {
                    fail(ex, chunkList);
                    throw ex;
                }
                if (++count >= retryCount) {
                    // TODO consider aggression option
                    return fail(ex, chunkList);
                }
                logger.warn("-- process() > count: {} exception: {}", count, ex);
                continue;

            } catch (BadDataException ex) {
                if (++count >= retryCount) {
                    return fail(ex, chunkList);
                }
                logger.warn("-- process() > count: {} exception: {}", count, ex);
                continue;
            }

            Map<ICloud.MBSFile, Outcome> outcomes;

            outcomes = write(chunkList, writers);
            logger.trace(">> process() >  outcomes: {}", outcomes.size());
            return outcomes;
        }
    }

    Map<ICloud.MBSFile, Outcome> write(ChunkServer.StorageHostChunkList chunkList, Map<ByteString, DataWriter> writers)
            throws InterruptedException, IOException {

        try {
            return signatureManager.write(writers);

        } catch (IOException ex) {
            logger.error("-- writer() > exception: ", ex);
            fail(ex, chunkList);
            throw ex;

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

    Map<ICloud.MBSFile, Outcome> fail(Exception ex, ChunkServer.StorageHostChunkList chunkList) {
        logger.warn("-- fail() > chunkList: {} exception: {}", chunkList.getHostInfo().getUri(), ex);
        Set<ByteString> failedSignatures = storeManager.fail(chunkList);
        return signatureManager.fail(failedSignatures);
    }

    void kill() {
        HttpUriRequest local = request.getAndSet(null);

        if (local != null) {
            logger.debug("-- kill() > killing");
            try {
                local.abort();
            } catch (UnsupportedOperationException ex) {
                logger.warn("-- kill() > exception: {}", ex);
            }
        } else {
            logger.debug("-- kill() > already killed");
        }
    }
}
// TODO aggression
