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

import com.github.horrorho.liquiddonkey.cloud.data.Core;
import com.github.horrorho.liquiddonkey.cloud.engine.ConcurrentEngine;
import com.github.horrorho.liquiddonkey.cloud.data.FileGroups;
import com.github.horrorho.liquiddonkey.cloud.data.Snapshot;
import com.github.horrorho.liquiddonkey.cloud.data.Snapshots;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.settings.config.EngineConfig;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SnapshotDownloader.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class SnapshotDownloader {

    public static SnapshotDownloader from(
            EngineConfig engineConfig,
            FileConfig fileConfig) {

        ConcurrentEngine engine = ConcurrentEngine.from(engineConfig);
        Function<Snapshot, SignatureManager> signatureManagers = s -> SignatureManager.from(s, fileConfig);

        return new SnapshotDownloader(engine, signatureManagers);
    }

    public static SnapshotDownloader from(
            ConcurrentEngine engine,
            Function<Snapshot, SignatureManager> signatureWriters) {

        return new SnapshotDownloader(engine, signatureWriters);
    }

    private static final Logger logger = LoggerFactory.getLogger(SnapshotDownloader.class);

    private final ConcurrentEngine engine;
    private final Function<Snapshot, SignatureManager> signatureManagers;

    SnapshotDownloader(
            ConcurrentEngine engine,
            Function<Snapshot, SignatureManager> signatureWriters) {

        this.engine = Objects.requireNonNull(engine);
        this.signatureManagers = Objects.requireNonNull(signatureWriters);
    }

    public void download( 
            HttpAgent agent,
            Core core,
            Snapshot snapshot,
            Consumer<Map<ICloud.MBSFile, Outcome>> outcomes
    ) throws BadDataException, IOException, InterruptedException {

        logger.trace("<< download() < dsPrsID: {} udid: {} snapshot: {}",
                snapshot.dsPrsID(), snapshot.backupUDID(), snapshot.snapshotID());

        Exception fatal = null;
        boolean isCompleted = false;

        while (!isCompleted && !snapshot.files().isEmpty() && !agent.authenticatorIsInvalid()) {
            logger.debug("-- download() > loop, files: {}", snapshot.files().size());

            // FilesGroups
            Snapshot get = snapshot;
            ChunkServer.FileGroups fileGroups
                    = agent.execute((client, mmeAuthToken) -> FileGroups.from(client, core, mmeAuthToken, get));

            // Store manager
            StoreManager storeManager = StoreManager.from(fileGroups);

            // Filter snapshots to reflect downloadbles.  
            // ICloud.MBSFiles may be non-downloadable, e.g. directories, empty files.
            Set<ByteString> downloadables = storeManager.remainingSignatures();
            snapshot = Snapshots.from(snapshot, file -> downloadables.contains(file.getSignature()));

            // Signature manager.
            SignatureManager signatureManager = signatureManagers.apply(snapshot);

            // Sanity check.
            logger.debug("-- download() > loaded signatures, StoreManager: {} SignatureManager: {}",
                    storeManager.remainingSignatures().size(), signatureManager.remainingSignatures().size());

            try {
                // Execute.
                fatal = engine.execute(agent, storeManager, signatureManager, outcomes);
                isCompleted = true;
                // TODO rethrow or handle this exception, eg 401 status codes
            } catch (TimeoutException ex) {
                logger.warn("-- download() > exception: {}", ex);
                isCompleted = false;
            }

            // Mismatches are possible.
            // DataWriters may have left the Store but termination occured before the contents were consumed.
            logger.debug("-- download() > remaining signatures, StoreManager: {} SignatureManager: {}",
                    storeManager.remainingSignatures().size(), signatureManager.remainingSignatures().size());

            // Filter out completed files. We use the SignatureManager as our reference.
            Set<ICloud.MBSFile> remaining = signatureManager.remainingFiles();
            snapshot = Snapshots.from(snapshot, file -> remaining.contains(file));

            // TODO checksum/ salvage completed chunks from the StoreManager in the case of timeout downloads.
            logger.debug("-- download() > end loop, is completed: {} fatal: {} remaining files: {}",
                    isCompleted, fatal, snapshot.files().size());
        }

        logger.trace(">> download()");
    }

//    Consumer<Map<ICloud.MBSFile, Outcome>> outcomes(SignatureManager signatureManager) {
//        return outcomes -> {
//            double completed = signatureManager.failedBytes() + signatureManager.outBytes();
//            double total = signatureManager.totalBytes();
//            double percentage = completed / total;
//            percentageOutcomes.accept(percentage, outcomes);
//        };
//    }
}
