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

import com.github.horrorho.liquiddonkey.cloud.data.FileGroups;
import com.github.horrorho.liquiddonkey.cloud.data.Snapshot;
import com.github.horrorho.liquiddonkey.cloud.data.Snapshots;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.store.DataWriter;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.iofunction.IOConsumer;
import com.github.horrorho.liquiddonkey.settings.config.EngineConfig;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConcurrentDownloader.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class ConcurrentDownloader {

    public static ConcurrentDownloader from(
            EngineConfig engineConfig,
            FileConfig fileConfig,
            Consumer<Map<ICloud.MBSFile, FileOutcome>> outcomes) {

        ConcurrentEngine engine = ConcurrentEngine.from(engineConfig);
        Function<Snapshot, SignatureManager> signatureManagers = s -> SignatureManager.from(s, fileConfig);

        return new ConcurrentDownloader(engine, signatureManagers, outcomes);
    }

    public static ConcurrentDownloader from(
            ConcurrentEngine engine,
            Function<Snapshot, SignatureManager> signatureWriters,
            Consumer<Map<ICloud.MBSFile, FileOutcome>> outcomes) {

        return new ConcurrentDownloader(engine, signatureWriters, outcomes);
    }

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentDownloader.class);

    private final ConcurrentEngine engine;
    private final Function<Snapshot, SignatureManager> signatureManagers;
    private final Consumer<Map<ICloud.MBSFile, FileOutcome>> outcomes;

    ConcurrentDownloader(
            ConcurrentEngine engine,
            Function<Snapshot, SignatureManager> signatureWriters,
            Consumer<Map<ICloud.MBSFile, FileOutcome>> outcomes) {

        this.engine = Objects.requireNonNull(engine);
        this.signatureManagers = Objects.requireNonNull(signatureWriters);
        this.outcomes = Objects.requireNonNull(outcomes);
    }

    public void download(HttpClient client, Snapshot snapshot)
            throws BadDataException, IOException, InterruptedException {

        logger.trace("<< download() < dsPrsID: {} udid: {} snapshot: {}",
                snapshot.dsPrsID(), snapshot.backupUDID(), snapshot.snapshotID());

        AtomicReference<Exception> fatal = new AtomicReference(null);
        boolean isTimedOut = false;

        while (!isTimedOut && !snapshot.files().isEmpty()) {
            logger.debug("-- download() > begin: {}", snapshot.files().size());

            // Files to download.
            ChunkServer.FileGroups fileGroups = FileGroups.from(client, snapshot);

            // Create managers.
            SignatureManager signatureManager = signatureManagers.apply(snapshot);
            StoreManager storeManager = StoreManager.from(fileGroups);

            // Create result consumers.
            Consumer<Set<ByteString>> failures = set -> fail(signatureManager, set);
            IOConsumer<Map<ByteString, DataWriter>> completed = writers -> write(signatureManager, writers);

            // Execute.
            isTimedOut = engine.execute(client, storeManager, fatal, failures, completed);

            // Filter out completed files.
            Set<ICloud.MBSFile> remaining = signatureManager.remainingFiles();
            snapshot = Snapshots.from(snapshot, file -> remaining.contains(file));

            // TODO timeout option
            // TODO checksum/ salvage completed chunks from the StoreManager in the case of timeout downloads.
            logger.debug("-- download() > isTimedOut: {} fatal: {} remaining: {}",
                    isTimedOut, fatal, snapshot.files().size());
        }

        logger.trace(">> download()");
    }

    void fail(SignatureManager manager, Set<ByteString> failures) {
        outcomes.accept(manager.fail(failures));
    }

    void write(SignatureManager manager, Map<ByteString, DataWriter> writers) throws IOException {
        try {
            outcomes.accept(manager.write(writers));
        } catch (InterruptedException ex) {
            logger.warn("-- write() > interrupted: {}", ex);
        } finally {
            writers.values().forEach(writer -> {
                try {
                    writer.close();
                } catch (IOException | IllegalStateException ex) {
                    logger.warn("-- write() > exception: ", ex);
                }
            });
        }
    }
}
