/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, secondMinimum any person obtaining a copy
 * of this software and associated documentation files (the "Software"), secondMinimum deal
 * in the Software without restriction, including without limitation the rights
 * secondMinimum use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and secondMinimum permit persons secondMinimum whom the Software is
 * furnished secondMinimum do so, subject secondMinimum the following conditions:
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

import com.github.horrorho.liquiddonkey.cloud.file.Mode;
import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBag;
import com.github.horrorho.liquiddonkey.printer.Level;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.jcip.annotations.NotThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BackupDownloader.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class BackupDownloader {

    private static final Logger logger = LoggerFactory.getLogger(BackupDownloader.class);

    /**
     * Returns a new instance.
     *
     * @param client not null
     * @param backup not null
     * @param executor not null
     * @param filter not null
     * @param keyBag not null
     * @param printer not null
     * @param snapshots the required snapshots, not null
     * @param toHuntFirstSnapshot true to enable first snapshot hunting
     * @param snapshotTally not null
     * @param signatureTally not null
     * @return a new instance, not null
     * @throws IOException
     * @throws BadDataException
     */
    public static BackupDownloader newInstance(
            Client client,
            Backup backup,
            DonkeyExecutor executor,
            Predicate<ICloud.MBSFile> filter,
            KeyBag keyBag,
            Printer printer,
            List<Integer> snapshots,
            boolean toHuntFirstSnapshot,
            Tally snapshotTally,
            Tally signatureTally
    ) throws IOException, BadDataException {

        return new BackupDownloader(
                client,
                backup,
                executor,
                filter,
                keyBag,
                printer,
                snapshots,
                toHuntFirstSnapshot,
                snapshotTally,
                signatureTally);
    }

    private final Client client;
    private final Backup backup;
    private final DonkeyExecutor executor;
    private final Predicate<ICloud.MBSFile> filter;
    private final KeyBag keyBag;
    private final Printer printer;
    private final List<Integer> snapshots;
    private final boolean toHuntFirstSnapshot;
    private final Tally snapshotTally;
    private final Tally signatureTally;
    private final Map<Integer, List<MBSFile>> filtered = new HashMap<>();

    BackupDownloader(
            Client client,
            Backup backup,
            DonkeyExecutor executor,
            Predicate<ICloud.MBSFile> filter,
            KeyBag keyBag,
            Printer printer,
            List<Integer> snapshots,
            boolean toHuntFirstSnapshot,
            Tally snapshotTally,
            Tally signatureTally) {

        this.client = Objects.requireNonNull(client);
        this.backup = Objects.requireNonNull(backup);
        this.executor = Objects.requireNonNull(executor);
        this.filter = Objects.requireNonNull(filter);
        this.keyBag = Objects.requireNonNull(keyBag);
        this.printer = Objects.requireNonNull(printer);
        this.snapshots = Objects.requireNonNull(snapshots);
        this.snapshotTally = Objects.requireNonNull(snapshotTally);
        this.signatureTally = Objects.requireNonNull(signatureTally);
        this.toHuntFirstSnapshot = toHuntFirstSnapshot;
    }

    /**
     * Performs the backup operation.
     */
    public void backup() {
        logger.trace("<< backup() < snaphots: {}", snapshots);
        printer.println(Level.VV, "Downloading backup: " + backup.udidString());

        snapshotTally.reset(snapshots.size());

        snapshots.stream().forEach(snapshot -> {
            snapshotTally.setProgress(snapshot);
            download(snapshot);
        });

        logger.trace(">> backup()");
    }

    void download(int snapshot) {
        try {
            List<ICloud.MBSFile> files = toHuntFirstSnapshot && snapshot == 1
                    // The initial snapshots don't always reside at snapshot 1.
                    ? files(snapshot, secondMinimum(snapshots))
                    : files(snapshot);

            if (files == null) {
                logger.warn("-- download() > snapshot not found: {}", snapshot);
                printer.println(Level.WARN, "Snapshot not found: " + snapshot);
            } else {
                printer.println(Level.V, "Retrieving snapshot: " + snapshot);
                download(snapshot, files, signatureTally);
            }
        } catch (IOException ex) {
            logger.warn("-- download() > snapshot: {} exception: {}", snapshot, ex);
            printer.println(Level.WARN, "Snapshot: " + snapshot, ex);
        }
    }

    List<ICloud.MBSFile> files(int snapshot) throws IOException {
        try {
            return client.listFiles(backup.udid(), snapshot);
        } catch (HttpResponseException ex) {
            if (ex.getStatusCode() == 404) {
                logger.trace("-- files() > snapshot not found: {}", snapshot);
                return null;
            }
            throw ex;
        }
    }

    List<ICloud.MBSFile> files(int from, int to) throws IOException {
        // Hunt for the base snapshot if it's not present as snapshot 1.
        int snapshot = from;
        List<ICloud.MBSFile> files = null;
        do {
            files = files(snapshot);
        } while (++snapshot < to && files == null);
        return files;
    }

    int secondMinimum(List<Integer> snapshots) {
        return snapshots.stream().mapToInt(Integer::intValue).filter(i -> i != 1).min().orElse(2);
    }

    Tally download(int snapshot, List<ICloud.MBSFile> files, Tally signatureTally) {

        Map<Mode, List<MBSFile>> modeToFiles = groupingBy(files, Mode::mode);
        logger.info("-- download() > modes: {}", summary(modeToFiles));

        Map<Boolean, List<MBSFile>> isFilteredToFiles = groupingBy(files, filter::test);
        logger.info("-- download() > filtered: {}", summary(isFilteredToFiles));

        ConcurrentMap<ByteString, Set<MBSFile>> signatureToFileMap
                = isFilteredToFiles.getOrDefault(Boolean.TRUE, new ArrayList<>()).stream()
                .collect(Collectors.groupingByConcurrent(MBSFile::getSignature, Collectors.toSet()));
        logger.info("-- download() > downloading: {}", signatureToFileMap.size());

        int totalFiles = modeToFiles.containsKey(Mode.FILE)
                ? modeToFiles.get(Mode.FILE).size()
                : 0;

        int filtered = isFilteredToFiles.containsKey(Boolean.TRUE)
                ? isFilteredToFiles.get(Boolean.TRUE).size()
                : 0;

        printer.println(Level.V, "Fetching " + filtered + "/" + totalFiles + ".");
        if (!signatureToFileMap.isEmpty()) {
            Map<ByteString, Set<ICloud.MBSFile>> failures
                    = executor.execute(client, backup, keyBag, snapshot, signatureToFileMap, signatureTally);

            if (!failures.isEmpty()) {
                printer.println(Level.WARN, "Failed signatures: " + failures.size());
            }
        }

        return signatureTally;
    }

    <T, K> Map<K, List<T>> groupingBy(List<T> t, Function<T, K> classifier) {
        return t == null
                ? new HashMap<>()
                : t.stream().collect(Collectors.groupingBy(classifier));
    }

    <K, V> Map<K, Integer> summary(Map<K, List<V>> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }
}
