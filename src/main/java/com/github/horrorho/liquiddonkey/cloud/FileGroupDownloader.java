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
import com.github.horrorho.liquiddonkey.cloud.file.WriterResult;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import com.github.horrorho.liquiddonkey.printer.Level;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
public class FileGroupDownloader {

    private static final Logger logger = LoggerFactory.getLogger(FileGroupDownloader.class);

    public static FileGroupDownloader from(
            ChunkServer.FileGroups fileGroups,
            ChunkDataFetcher chunkDataFetcher,
            SignatureWriter signatureWriter,
            int threads,
            Printer printer) {

        logger.trace("-- from() < groups: {} threads: {}", fileGroups.getFileGroupsCount(), threads);

        List<HostStoragePair> managers = fileGroups.getFileGroupsList().stream()
                .map(HostStoragePair::from)
                .collect(Collectors.toList());

        return new FileGroupDownloader(
                signatureWriter,
                chunkDataFetcher,
                managers,
                threads,
                new AtomicBoolean(false),
                printer);
    }

    private final SignatureWriter signatureWriter;
    private final ChunkDataFetcher chunkDataFetcher;
    private final List<HostStoragePair> managers;
    private final int threads;
    private final AtomicBoolean isFileIOError;
    private final Printer printer;
    private volatile Thread executorThread = null;
    private final long throttleMs = 100;
    private final AtomicReference<Exception> killException = new AtomicReference<>(null);

    FileGroupDownloader(
            SignatureWriter signatureWriter,
            ChunkDataFetcher chunkDataFetcher,
            List<HostStoragePair> managers,
            int threads,
            AtomicBoolean isFileIOError,
            Printer printer) {

        this.signatureWriter = Objects.requireNonNull(signatureWriter);
        this.chunkDataFetcher = Objects.requireNonNull(chunkDataFetcher);
        this.managers = Objects.requireNonNull(managers);
        this.threads = threads;
        this.isFileIOError = Objects.requireNonNull(isFileIOError);
        this.printer = Objects.requireNonNull(printer);
    }

    void download() throws AuthenticationException, BadDataException, IOException, InterruptedException {

        logger.trace("<< download()");

        executorThread = Thread.currentThread();
        ExecutorService fetchExecutor = Executors.newFixedThreadPool(threads);
        ExecutorService writerExecutor = Executors.newFixedThreadPool(threads);

        logger.trace("-- download() > submit futures");

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
            for (HostStoragePair manager : managers) {
                Iterator<Long> containers = manager.hostManager().iterator();
                while (containers.hasNext()) {
                    TimeUnit.MILLISECONDS.sleep(throttleMs);
                    futures.add(async(manager, containers.next(), fetchExecutor, writerExecutor));
                }
            }

        } catch (InterruptedException ex) {
            
        }

        logger.trace("-- download() > futures submitted");

        fetchExecutor.shutdown();
        logger.trace("-- download() > await termination");
        fetchExecutor.awaitTermination(30, TimeUnit.MINUTES); // TODO 30 min timeout and reacquire files?
        logger.trace("-- download() > fetchExecutor terminated.");
        writerExecutor.shutdown();
        writerExecutor.awaitTermination(30, TimeUnit.SECONDS); // TODO figure
        logger.trace("-- download() > writerExecutor terminated.");

        // clean up interrupted exceptions?
        logger.trace(">> download()");

        // multiple interrupts? uncleared?
    }

    CompletableFuture<Void> async(HostStoragePair hostStoragePair, long container, Executor fetchExecutor, Executor writerExecutor) {
        logger.trace("<< async() < container: {}", container);

        ChunkServer.StorageHostChunkList chunkList = hostStoragePair.hostManager().storageHostChunkList(container);

        CompletableFuture<Void> async
                = CompletableFuture.<byte[]>supplyAsync(()
                        -> fetch(chunkList, hostStoragePair.hostManager(), container), fetchExecutor)
                .<byte[]>thenAcceptAsync(chunkData
                        -> process(chunkList, chunkData, hostStoragePair, container), writerExecutor)
                // TODO remove exceptionally?
                .exceptionally(throwable -> exception(hostStoragePair, container, throwable)).toCompletableFuture();

        logger.trace(">> async()");
        return async;
    }

    byte[] fetch(ChunkServer.StorageHostChunkList chunkList, HostManager hostManager, long container) {
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

    void process(ChunkServer.StorageHostChunkList chunkList, byte[] chunkData, HostStoragePair hostStoragePair, long container) {
        logger.trace("<< process() < container: {} chunkData length: {}",
                container, chunkData == null ? null : chunkData.length);

        if (chunkData == null) {
            return;
        }

        ChunkDecrypter chunkDecrypter = ChunkDecrypter.newInstance();
        Map<ByteString, IOFunction<OutputStream, Long>> writers
                = hostStoragePair.storageManager().put(container, chunkDecrypter.decrypt(chunkList, chunkData));

        if (writers == null) {
            hostStoragePair.hostManager().success(container);
        } else {
            try {
                for (ByteString signature : writers.keySet()) {
                    Map<ICloud.MBSFile, WriterResult> map = signatureWriter.write(signature, writers.get(signature));
                    map.keySet().stream().forEach(
                            file -> printer.println(Level.VV,
                                    "\t" + file.getDomain() + " " + file.getRelativePath() + " " + map.get(file)));
                }
                // TEST
                //fileIoError(new IOException("test"));
                hostStoragePair.hostManager().success(container);
            } catch (IOException ex) {
                hostStoragePair.hostManager().failed(container, ex);
                fileIoError(ex);
            }
        }

        logger.trace(">> process() > container: {}", container);
    }

    Void exception(HostStoragePair hostStoragePair, long container, Throwable th) {
        logger.warn("-- throwable() > container: {} throwable: {}", container, th);
        hostStoragePair.hostManager().failed(container, th);
        return null;
    }

    boolean fileIoError(IOException ex) {
        logger.trace("<< fileIoError() < exception: {}", ex);

        isFileIOError.set(true);
        kill(ex);

        logger.trace(">> fileIoError()");
        return true;
    }

    boolean ioError(IOException ex) {
        logger.trace("<< ioError() < exception: {}", ex);

        kill(ex);

        logger.trace(">> ioError()");
        return true;
    }

    void kill(Exception ex) {
        logger.trace("<< kill()");

        killException.compareAndSet(null, ex);
        if (executorThread == null) {
            logger.warn("-- kill() > bad state, null executorThread");
        } else {
            executorThread.interrupt();
        }

        logger.trace(">> kill()");
    }

    private static final class HostStoragePair {

        private final HostManager hostManager;
        private final StoreManager storageManager;

        static HostStoragePair from(ChunkServer.FileChecksumStorageHostChunkLists fileGroup) {
            return new HostStoragePair(
                    HostManager.from(fileGroup),
                    StoreManager.from(fileGroup));
        }

        HostStoragePair(HostManager hostManager, StoreManager storageManager) {
            this.hostManager = hostManager;
            this.storageManager = storageManager;
        }

        public HostManager hostManager() {
            return hostManager;
        }

        public StoreManager storageManager() {
            return storageManager;
        }
    }
}
// TODO error count before fail?
// Aggression
