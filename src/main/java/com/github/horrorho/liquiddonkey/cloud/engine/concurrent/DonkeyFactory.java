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
import com.github.horrorho.liquiddonkey.cloud.store.DataWriter;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.iofunction.IOConsumer;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;

/**
 * DonkeyFactory.
 *
 * @author Ahseya
 */
@ThreadSafe
class DonkeyFactory {

    public static DonkeyFactory from(
            HttpClient client,
            StoreManager storeManager,
            int retryCount,
            AtomicReference<Exception> fatal,
            Consumer<Set<ByteString>> failures,
            IOConsumer<Map<ByteString, DataWriter>> signaturesWriter) {

        return new DonkeyFactory(
                client,
                ChunksClient.create(),
                storeManager,
                retryCount,
                fatal,
                failures,
                signaturesWriter);
    }

    private final HttpClient client;
    private final ChunksClient chunksClient;
    private final StoreManager storeManager;
    private final int retryCount;
    private final AtomicReference<Exception> fatal;
    private final Consumer<Set<ByteString>> failures;
    private final IOConsumer<Map<ByteString, DataWriter>> signaturesWriter;
    private final Map<Donkey, ChunkServer.StorageHostChunkList> donkies;  // Requires a synchronized weak hash map.

    DonkeyFactory(
            HttpClient client,
            ChunksClient chunksClient,
            StoreManager storeManager,
            int retryCount,
            AtomicReference<Exception> fatal,
            Consumer<Set<ByteString>> failures,
            IOConsumer<Map<ByteString, DataWriter>> signaturesWriter,
            WeakHashMap<Donkey, ChunkServer.StorageHostChunkList> donkies) {

        this.client = Objects.requireNonNull(client);
        this.chunksClient = Objects.requireNonNull(chunksClient);
        this.storeManager = Objects.requireNonNull(storeManager);
        this.retryCount = retryCount;
        this.fatal = Objects.requireNonNull(fatal);
        this.failures = Objects.requireNonNull(failures);
        this.signaturesWriter = Objects.requireNonNull(signaturesWriter);
        this.donkies = Collections.synchronizedMap(donkies);
    }

    DonkeyFactory(
            HttpClient client,
            ChunksClient chunksClient,
            StoreManager storeManager,
            int retryCount,
            AtomicReference<Exception> fatal,
            Consumer<Set<ByteString>> failures,
            IOConsumer<Map<ByteString, DataWriter>> signaturesWriter) {

        this(client, chunksClient, storeManager, retryCount, fatal, failures, signaturesWriter, new WeakHashMap<>());
    }

    public FetchDonkey fetchDonkey(ChunkServer.StorageHostChunkList chunkList) {
        FetchDonkey instance = new FetchDonkey(
                client,
                chunksClient,
                this::writerDonkey,
                storeManager,
                chunkList,
                new ArrayList<>(),
                retryCount,
                fatal,
                failures);

        donkies.put(instance, chunkList);
        return instance;
    }

    FetchDonkey fetchDonkey(WriterDonkey donkey) {
        FetchDonkey instance = new FetchDonkey(
                client,
                chunksClient,
                this::writerDonkey,
                storeManager,
                donkey.chunkList(),
                donkey.exceptions(),
                retryCount,
                fatal,
                failures);

        donkies.put(instance, donkey.chunkList());
        return instance;
    }

    WriterDonkey writerDonkey(FetchDonkey donkey, byte[] data) {
        WriterDonkey instance = new WriterDonkey(
                this::fetchDonkey,
                signaturesWriter,
                data,
                storeManager,
                donkey.chunkList(),
                donkey.exceptions(),
                retryCount,
                fatal,
                failures);

        donkies.put(instance, donkey.chunkList());
        return instance;
    }

    long killDonkies() {
        return donkies.keySet()
                .stream()
                .filter(Objects::nonNull)
                .peek(Donkey::kill)
                .count();
    }
}
