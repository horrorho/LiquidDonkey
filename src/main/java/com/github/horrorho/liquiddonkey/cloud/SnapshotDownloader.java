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
import com.github.horrorho.liquiddonkey.cloud.donkey.Donkey;
import com.github.horrorho.liquiddonkey.cloud.donkey.DonkeyFactory;
import com.github.horrorho.liquiddonkey.cloud.donkey.Track;
import com.github.horrorho.liquiddonkey.cloud.file.SignatureWriter;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import com.github.horrorho.liquiddonkey.util.pool.WorkPools;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
public class SnapshotDownloader {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotDownloader.class);

    private final FileConfig fileConfig;
    private final Printer printer;

    private final int threads = 2;
    private final int retryCount = 3;
    private final int staggerMs = 100;

    private final long executorTimeoutSeconds = 1800;

    public SnapshotDownloader(FileConfig fileConfig, Printer printer) {
        this.fileConfig = fileConfig;
        this.printer = printer;
    }

    void download(Http http, Client client, Snapshot snapshot) throws AuthenticationException, BadDataException, IOException, InterruptedException {

        ChunkServer.FileGroups fileGroups = fetchFileGroups(http, client, snapshot);

        SignatureWriter writer = SignatureWriter.from(snapshot, fileConfig);
        StoreManager manager = StoreManager.from(fileGroups, writer, printer);
        DonkeyFactory factory = DonkeyFactory.from(http, client, manager, retryCount);

        Map<Track, List<Donkey>> donkies = manager.chunkListList().stream().map(factory::fetchDonkey)
                .collect(Collectors.groupingBy(list -> Track.FETCH));
        WorkPools<Track, Donkey> pools = WorkPools.from(Track.class, donkies);

        execute(pools);

    }

    void execute(WorkPools<Track, Donkey> pools) throws InterruptedException {
        logger.trace("<< download()");

        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = 0; i < threads; i++) {
            executor.submit(Runner.newInstance(pools, Track.FETCH));
            executor.submit(Runner.newInstance(pools, Track.DECODE_WRITE));
            TimeUnit.MILLISECONDS.sleep(staggerMs);
        }

        logger.trace("-- download() > runners running: {}", threads);
        executor.shutdown();
        logger.trace("-- download() > awaiting termination");
        executor.awaitTermination(executorTimeoutSeconds, TimeUnit.SECONDS); // TODO 30 min timeout? missing files from signature writer?
        executor.shutdownNow();
        logger.trace(">> download()");
    }

    ChunkServer.FileGroups fetchFileGroups(Http http, Client client, Snapshot snapshot)
            throws AuthenticationException, BadDataException, IOException {

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
