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
package com.github.horrorho.liquiddonkey.cloud.donkey;

import com.github.horrorho.liquiddonkey.cloud.clients.ChunksClient;
import com.github.horrorho.liquiddonkey.cloud.file.SignatureWriter;
import com.github.horrorho.liquiddonkey.cloud.file.WriterResult;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import org.apache.http.client.HttpClient;

/**
 * DonkeyFactory.
 *
 * @author Ahseya
 */
public class DonkeyFactory {
 
    public static DonkeyFactory from(
            HttpClient client,
            BiConsumer<ICloud.MBSFile, WriterResult> results,
            SignatureWriter signatureWriter,
            StoreManager storeManager,
            int retryCount) {

        return new DonkeyFactory(
                client,
                results,
                signatureWriter,
                storeManager,
                retryCount,
                new AtomicReference<>(null));
    }

    private final HttpClient client;
    private final BiConsumer<ICloud.MBSFile, WriterResult> results;
    private final SignatureWriter signatureWriter;
    private final StoreManager storeManager;
    private final int retryCount;
    private final AtomicReference<Exception> fatal;

    DonkeyFactory(
            HttpClient client,
            BiConsumer<ICloud.MBSFile, WriterResult> out,
            SignatureWriter signatureWriter,
            StoreManager storeManager,
            int retryCount,
            AtomicReference<Exception> fatal) {

        this.client = Objects.requireNonNull(client);
        this.results = Objects.requireNonNull(out);
        this.signatureWriter = Objects.requireNonNull(signatureWriter);
        this.storeManager = storeManager;
        this.retryCount = retryCount;
        this.fatal = Objects.requireNonNull(fatal);
    }

    public FetchDonkey fetchDonkey(ChunkServer.StorageHostChunkList chunkList) {
        return new FetchDonkey(
                client,
                ChunksClient.create(),
                this::writerDonkey,
                chunkList,
                new ArrayList<>(),
                retryCount,
                fatal);
    }

    FetchDonkey fetchDonkey(WriterDonkey donkey) {
        return new FetchDonkey(
                client,
                ChunksClient.create(),
                this::writerDonkey,
                donkey.chunkList(),
                donkey.exceptions(),
                donkey.retryCount(),
                fatal);
    }

    WriterDonkey writerDonkey(FetchDonkey donkey, byte[] data) {
        return new WriterDonkey(
                storeManager,
                results,
                this::fetchDonkey,
                writers -> new DonkeyWriter(signatureWriter, writers),
                data,
                donkey.chunkList(),
                donkey.exceptions(),
                donkey.retryCount(),
                fatal);
    }
}
