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

import com.github.horrorho.liquiddonkey.cloud.client.ChunksClient;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.util.pool.ToDo;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.jcip.annotations.NotThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FetchDonkey.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class FetchDonkey extends Donkey {

    private static final Logger log = LoggerFactory.getLogger(FetchDonkey.class);

    private final HttpClient client;
    private final HttpUriRequest request;
    private final ResponseHandler<byte[]> responseHandler;
    private final BiFunction<FetchDonkey, byte[], WriterDonkey> writerDonkeys;

    public FetchDonkey(
            HttpClient client,
            ChunksClient chunksClient,
            BiFunction<FetchDonkey, byte[], WriterDonkey> writerDonkeys,
            StoreManager manager,
            ChunkServer.StorageHostChunkList chunkList,
            List<Exception> exceptions,
            int retryCount,
            AtomicReference<Exception> fatal,
            Consumer<Set<ByteString>> failures) {

        super(manager, chunkList, exceptions, retryCount, fatal, failures);

        this.client = Objects.requireNonNull(client);
        this.request = chunksClient.get(chunkList);
        this.responseHandler = chunksClient.responseHandler();
        this.writerDonkeys = Objects.requireNonNull(writerDonkeys);
    }

    @Override
    protected ToDo<Track, Donkey> toProcess() throws IOException {
        log.trace("<< toProcess()");

        ToDo<Track, Donkey> toDo;
        try {
            byte[] data = client.execute(request, responseHandler);
            toDo = requeue(Track.DECODE_WRITE, writerDonkeys.apply(this, data));

        } catch (UnknownHostException ex) {
            log.warn("-- toProcess() > exception: ", ex);
            toDo = retry(ex);

        } catch (HttpResponseException ex) {
            log.warn("-- toProcess() > exception: ", ex);
            toDo = ex.getStatusCode() == 401
                    ? abort(ex)
                    : retry(ex);

        } catch (IOException | RuntimeException ex) {
            throw ex;
        }

        log.trace(">> toProcess() > release: {}", toDo);
        return toDo;
    }

    @Override
    void kill() {
        super.kill();
        try {
            request.abort();
        } catch (UnsupportedOperationException ex) {
            log.warn("-- kill() > exception: {}", ex);
        }
    }
}
