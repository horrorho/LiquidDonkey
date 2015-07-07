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

import com.github.horrorho.liquiddonkey.cloud.file.SignatureWriter;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
public class FileGroupDownloader {

    private static final Logger logger = LoggerFactory.getLogger(FileGroupDownloader.class);

    public static FileGroupDownloader from(
            ChunkServer.FileChecksumStorageHostChunkLists fileGroup,
            ChunkDataFetcher chunkDataFetcher,
            SignatureWriter signatureWriter,
            int threads) {

        StoreManager storeManager = StoreManager.from(fileGroup);
        HostManager hostManager = HostManager.from(fileGroup);

        return new FileGroupDownloader(
                signatureWriter,
                chunkDataFetcher,
                hostManager,
                storeManager,
                threads,
                new AtomicBoolean(false));
    }

    private final SignatureWriter signatureWriter;
    private final ChunkDataFetcher chunkDataFetcher;
    private final HostManager hostManager;
    private final StoreManager storeManager;
    private final int threads;
    private final AtomicBoolean isFatality;
    private volatile Thread executorThread = null;

    FileGroupDownloader(
            SignatureWriter signatureWriter,
            ChunkDataFetcher chunkDataFetcher,
            HostManager hostManager,
            StoreManager storeManager,
            int threads,
            AtomicBoolean isFatality) {

        this.signatureWriter = signatureWriter;
        this.chunkDataFetcher = chunkDataFetcher;
        this.hostManager = hostManager;
        this.storeManager = storeManager;
        this.threads = threads;
        this.isFatality = isFatality;
    }

    void download() throws AuthenticationException, BadDataException, IOException, InterruptedException {

        logger.trace("<< downloadFileGroup()");

        executorThread = Thread.currentThread();
        ExecutorService fetchExecutor = Executors.newFixedThreadPool(threads);
        ExecutorService writerExecutor = Executors.newFixedThreadPool(threads);
        Iterator<Long> iterator = hostManager.iterator();

        logger.trace("-- downloadFileGroup() > iterate");
        while (iterator.hasNext()) {
            async(iterator.next(), fetchExecutor, writerExecutor);
        }

        fetchExecutor.shutdown();
        logger.trace("-- downloadFileGroup() > await termination");
        fetchExecutor.awaitTermination(30, TimeUnit.MINUTES); // TODO 30 min timeout and reacquire files?
        writerExecutor.shutdown();
        writerExecutor.awaitTermination(30, TimeUnit.SECONDS); // TODO figure
        logger.trace("-- downloadFileGroup() > terminated");

        // clean up interrupted exceptions?
        logger.trace(">> downloadFileGroup()");

        // multiple interrupts? uncleared?
    }

    CompletableFuture<Void> async(Long container, Executor fetchExecutor, Executor writerExecutor) {
        logger.trace("<< async() < container: {}", container);

        if (container == null) {
            logger.warn("<< async() < null container");
        }
        ChunkServer.StorageHostChunkList chunkList = hostManager.storageHostChunkList(container);

        CompletableFuture<Void> async
                = CompletableFuture.<byte[]>supplyAsync(
                        () -> fetch(chunkList, container), fetchExecutor)
                .<byte[]>thenAcceptAsync(
                        chunkData -> process(chunkList, chunkData, container), writerExecutor)
                .exceptionally(throwable -> {
                    //TODO rework
                    logger.warn("<< main() < throwable : {}", throwable.getLocalizedMessage());
                    return null;
                });

        logger.trace(">> async()");
        return async;
    }

    byte[] fetch(ChunkServer.StorageHostChunkList chunkList, Long container) {
        try {
            logger.trace("<< fetch() < container: {}", container);

            byte[] chunkData = chunkDataFetcher.apply(chunkList);

            logger.trace(">> fetch() > chunk data size: {}", chunkData.length);
            return chunkData;

        } catch (UncheckedIOException ex) {
            hostManager.failed(container, ex);
            ioError(ex.getCause());

            logger.trace(">> fetch() > exception: {}", ex);
            return null;
        }
    }

    void process(
            ChunkServer.StorageHostChunkList chunkList,
            byte[] chunkData,
            Long container) {

        logger.trace("<< process() < chunkData length: {}", chunkData == null ? null : chunkData.length);

        if (chunkData == null) {
            logger.warn("-- process() > null chunkData");
            return;
        }

        ChunkDecrypter chunkDecrypter = ChunkDecrypter.newInstance();

        Map<ByteString, IOFunction<OutputStream, Long>> writers
                = storeManager.put(container, chunkDecrypter.decrypt(chunkList, chunkData));

        if (writers == null) {
            hostManager.success(container);
        } else {
            try {
                for (ByteString signature : writers.keySet()) {
                    signatureWriter.write(signature, writers.get(signature));
                }
                hostManager.success(container);
            } catch (IOException ex) {
                fileIoError(ex);
            }
        }

        logger.trace(">> process() > chunk data size: {}", chunkData.length);
    }

    boolean fileIoError(IOException ex) {
        logger.trace("<< fileIoError() < exception: {}", ex);

        kill();

        logger.trace(">> fileIoError()");
        return true;
    }

    boolean ioError(IOException ex) {
        logger.trace("<< ioError() < exception: {}", ex);

        kill();

        logger.trace(">> ioError()");
        return true;
    }

    void kill() {
        logger.trace("<< kill()");

        if (executorThread == null) {
            logger.warn("-- kill() > bad state, null executorThread");
        } else {
            // Pull the plug and interrupt all executor tasks.
            executorThread.interrupt();
        }

        logger.trace(">> kill()");
    }
}
// TODO error count before fail?
// Aggression