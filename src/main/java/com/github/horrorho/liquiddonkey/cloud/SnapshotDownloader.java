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

import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Snapshot download.
 * <p>
 * Concurrently downloads snapshots via Donkeys.
 *
 * @author Ahseya
 */
public final class SnapshotDownloader {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotDownloader.class);

    private final Http http;
    private final Client client;
    private final ChunkDataFetcher chunkDataFetcher;
    private final int threads = 4;
    private final int retryCount = 3;
    private final boolean isAggressive = false;

    public SnapshotDownloader(Http http, Client client) {
        this.http = http;
        this.client = client;
        chunkDataFetcher = ChunkDataFetcher.newInstance(http, client);
    }

    public void moo(Snapshot snapshot)
            throws AuthenticationException, BadDataException, IOException, InterruptedException {

        ChunkServer.FileGroups fileGroups = fetchFileGroups(snapshot);
        downloadFileGroups(fileGroups);
    }

    ChunkServer.FileGroups fetchFileGroups(Snapshot snapshot)
            throws AuthenticationException, BadDataException, IOException, InterruptedException {

        logger.trace("<< fetchFileGroups() < snapshot: {}", snapshot.id());

        int count = retryCount;
        while (true) {
            try {
                ChunkServer.FileGroups fileGroups
                        = client.getFileGroups(http, snapshot.backup().udid(), snapshot.id(), snapshot.files());

                logger.debug("-- fetchFileGroups() > fileChunkErrorList: {}", fileGroups.getFileChunkErrorList());
                logger.debug("-- fetchFileGroups() > fileErrorList: {}", fileGroups.getFileErrorList());
                logger.trace(">> fetchFileGroups()");
                return fileGroups;

            } catch (BadDataException | HttpResponseException ex) {
                if (count-- > 0) {
                    logger.warn("-- fetchFileGroups() > exception: {}", ex);
                } else {
                    throw ex;
                }
            }
        }
    }

    void downloadFileGroups(ChunkServer.FileGroups fileGroups)
            throws AuthenticationException, BadDataException, IOException, InterruptedException {

        logger.trace("<< downloadFileGroups()");
        int count = retryCount;
        for (ChunkServer.FileChecksumStorageHostChunkLists fileGroup : fileGroups.getFileGroupsList()) {
            while (true) {
                try {
                    downloadFileGroup(fileGroup);
                    break;
                } catch (BadDataException | HttpResponseException ex) {
                    if (count-- > 0) {
                        logger.warn("-- downloadFileGroups() > exception: {}", ex);
                    } else {
                        throw ex;
                    }
                }
            }
        }
        logger.trace(">> downloadFileGroups()");
    }

    void downloadFileGroup(ChunkServer.FileChecksumStorageHostChunkLists fileGroup)
            throws AuthenticationException, BadDataException, IOException, InterruptedException {

        logger.trace("<< downloadFileGroup()");
        StoreManager storeManager = StoreManager.from(fileGroup);
        HostManager hostManager = HostManager.from(fileGroup);

        while (!hostManager.isEmpty()) {
            ExecutorService fetchExecutor = Executors.newFixedThreadPool(threads);
            ExecutorService writerExecutor = Executors.newFixedThreadPool(threads);
            Iterator<Long> iterator = hostManager.iterator();

            logger.trace("-- downloadFileGroup() > iterate");
            while (iterator.hasNext()) {
                async(hostManager, iterator.next(), fetchExecutor, writerExecutor);
            }

            fetchExecutor.shutdown();
            logger.trace("-- downloadFileGroup() > await termination");
            fetchExecutor.awaitTermination(30, TimeUnit.MINUTES); // TODO 30 min timeout and reacquire files?
            logger.trace("-- downloadFileGroup() > terminated");
        }

        // clean up interrupted exceptions?
        logger.trace(">> downloadFileGroup()");
    }

    void async(HostManager hostManager, Long container, Executor fetchExecutor, Executor writerExecutor) {
        logger.trace("<< async() < container: {}", container);

        if (container == null) {
            logger.warn("<< async() < null container");
        }

        CompletableFuture<Void> a = CompletableFuture.<byte[]>supplyAsync(() -> {
            logger.trace("<< async() < container: {}", container);

            ChunkServer.StorageHostChunkList chunksList = hostManager.storageHostChunkList(container);
            byte[] chunkData = chunkDataFetcher.apply(chunksList);

            logger.trace("<< async() > chunk data size: {}", chunkData.length);
            return chunkData;
        }, fetchExecutor).<byte[]>thenAcceptAsync(chunkData -> {
            logger.debug("<< async() < chunk data size: {}", chunkData.length);

            hostManager.success(container);

        }, writerExecutor).exceptionally(throwable -> {
            logger.warn("<< main() < throwable : {}", throwable.getLocalizedMessage());
            return null;
        });

        logger.trace(">> async()");
    }

    
    
    
    
}
// do all filegroups!
