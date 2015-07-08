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
import com.github.horrorho.liquiddonkey.cloud.file.SignatureWriter;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.printer.Printer;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;
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
@NotThreadSafe
public final class SnapshotDownloader {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotDownloader.class);

    private final Http http;
    private final Client client;
    private final ChunkDataFetcher chunkDataFetcher;
    private final SignatureWriter signatureWriter;
    private final Printer printer;
    private final int threads = 4;
    private final int retryCount = 3;
    private final boolean isAggressive = false;

    public SnapshotDownloader(Http http, Client client, ChunkDataFetcher chunkDataFetcher, SignatureWriter signatureWriter, Printer printer) {
        this.http = http;
        this.client = client;
        this.chunkDataFetcher = chunkDataFetcher;
        this.signatureWriter = signatureWriter;
        this.printer = printer;
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

                logger.info("-- fetchFileGroups() > fileChunkErrorList: {}", fileGroups.getFileChunkErrorList());
                logger.info("-- fetchFileGroups() > fileErrorList: {}", fileGroups.getFileErrorList());
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
        for (ChunkServer.FileChecksumStorageHostChunkLists fileGroup : fileGroups.getFileGroupsList()) {

            FileGroupDownloader downloader
                    = FileGroupDownloader.from(fileGroups, chunkDataFetcher, signatureWriter, threads, printer);
            downloader.download();

        }
        logger.trace(">> downloadFileGroups()");
    }
}
// TODO split downloads?
