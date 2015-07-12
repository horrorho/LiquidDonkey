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

import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.cloud.file.SignatureWriter;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import com.github.horrorho.liquiddonkey.util.pool.WorkPool;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
public class FileGroupDownloader {

    private static final Logger logger = LoggerFactory.getLogger(FileGroupDownloader.class);

    /*
    
     storagehostchunklist
     chunkreferences
     signaturewriter
     http
     client
    
     specified
     threads
     stagger
     error handling
    
    
     */
    private final int threads = 2;
    private final int retryCount = 3;
    private final int staggerMs = 100;

    private final long executorTimeoutSeconds = 1800;

    public void moo(Http http, Client client, Snapshot snapshot, Printer printer, FileConfig fileConfig) throws BadDataException, IOException, AuthenticationException, InterruptedException {

        ChunkServer.FileGroups fileGroups = fetchFileGroups(client, http, snapshot);

        StoreManager manager = StoreManager.from(fileGroups);
        SignatureWriter writer = SignatureWriter.from(snapshot, fileConfig);

        List<Item> items = manager.chunkListList().stream()
                .map(chunkList -> Item.newInstance(chunkList, manager))
                .collect(Collectors.toList());

        //WorkPool<Item> pool = WorkPool.newInstance(items);
        ExecutorService executor = Executors.newFixedThreadPool(threads * 2);

        //Supplier<WorkerFetcher> donkeySupplier = () -> new WorkerFetcher(pool, client, http, executor, writer, printer, new AtomicReference<>(), retryCount);
        // download(donkeySupplier, pool, executor);
    }

    void download(Supplier<WorkerFetcher> donkeySupplier, WorkPool<Item> pool, ExecutorService executor) throws InterruptedException {
        logger.trace("<< download()");

        //ExecutorService executor = Executors.newFixedThreadPool(threads * 2);
        for (int i = 0; i < threads; i++) {
            executor.submit(donkeySupplier.get());
            TimeUnit.MILLISECONDS.sleep(staggerMs);
        }

        logger.trace("-- download() > workers fired up: {}", threads);

        executor.shutdown();
        logger.trace("-- download() > awaiting termination");
        executor.awaitTermination(executorTimeoutSeconds, TimeUnit.SECONDS);
        executor.shutdownNow();
        logger.trace(">> download()");
    }

    ChunkServer.FileGroups fetchFileGroups(Client client, Http http, Snapshot snapshot)
            throws AuthenticationException, BadDataException, IOException, InterruptedException {

        logger.trace("<< fetchFileGroups() < snapshot: {}", snapshot.id());

        int count = retryCount;
        while (true) {
            try {
                ChunkServer.FileGroups fileGroups
                        = client.getFileGroups(http, snapshot.backup().udid(), snapshot.id(), snapshot.files());

                logger.info("-- fetchFileGroups() > fileChunkErrorList: {}", fileGroups.getFileChunkErrorList());
                logger.info("-- fetchFileGroups() > fileErrorList: {}", fileGroups.getFileErrorList());
                logger.trace(">> fetchFileGroups()");
                return fileGroups;

            } catch (AuthenticationException ex) {
                throw ex;
            } catch (BadDataException | HttpResponseException ex) {
                if (count-- > 0) {
                    logger.warn("-- fetchFileGroups() > exception: {}", ex);
                } else {
                    throw ex;
                }
            }
        }
    }
}

// TODO error count before fail?
// Aggression
