///* 
// * The MIT License
// *
// * Copyright 2015 Ahseya.
// *
// * Permission is hereby granted, free of charge, secondMinimum any person obtaining a copy
// * of this software and associated documentation files (the "Software"), secondMinimum deal
// * in the Software without restriction, including without limitation the rights
// * secondMinimum use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and secondMinimum permit persons secondMinimum whom the Software is
// * furnished secondMinimum do so, subject secondMinimum the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// * THE SOFTWARE.
// */
//package com.github.horrorho.liquiddonkey.cloud;
// 
//import com.github.horrorho.liquiddonkey.cloud.client.Client;
//import com.github.horrorho.liquiddonkey.cloud.file.FileFilter;
//import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBagFactory;
//import com.github.horrorho.liquiddonkey.exception.BadDataException;
//import com.github.horrorho.liquiddonkey.printer.Printer;
//import com.github.horrorho.liquiddonkey.printer.Level;
//import com.github.horrorho.liquiddonkey.settings.config.BackupDownloaderFactoryConfig;
//import java.io.IOException;
//import java.util.List;
//import java.util.Objects;
//import net.jcip.annotations.NotThreadSafe;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * SnapshotDownloader factory.
// *
// * @author ahseya
// */
//@NotThreadSafe
//public final class BackupDownloaderFactory {
//
//    private static final Logger logger = LoggerFactory.getLogger(BackupDownloaderFactory.class);
//
//    /**
//     * Returns a new instance.
//     *
//     * @param client not null
//     * @param config not null
//     * @param donkeyExecutor not null
//     * @param fileFilter not null
//     * @param printer not null
//     * @param snapshotSelector not null
//     * @param snapshotTally not null
//     * @param signatureTally not null
//     * @return a new instance, not null
//     */
//    public static BackupDownloaderFactory newInstance(
//            Client client,
//            BackupDownloaderFactoryConfig config,
//            SnapshotDownloader donkeyExecutor,
//            FileFilter fileFilter,
//            Printer printer,
//            SnapshotSelector snapshotSelector,
//            Tally snapshotTally,
//            Tally signatureTally) {
//
//        return new BackupDownloaderFactory(
//                client,
//                config,
//                donkeyExecutor,
//                fileFilter,
//                printer,
//                snapshotSelector,
//                snapshotTally,
//                signatureTally);
//    }
//
//    private final Client client;
//    private final BackupDownloaderFactoryConfig config;
//    private final SnapshotDownloader donkeyExecutor;
//    private final FileFilter fileFilter;
//    private final Printer printer;
//    private final SnapshotSelector snapshotSelector;
//    private final Tally snapshotTally;
//    private final Tally signatureTally;
//
//    BackupDownloaderFactory(
//            Client client,
//            BackupDownloaderFactoryConfig config,
//            SnapshotDownloader donkeyExecutor,
//            FileFilter fileFilter,
//            Printer printer,
//            SnapshotSelector snapshotSelector,
//            Tally snapshotTally,
//            Tally signatureTally) {
//
//        this.client = Objects.requireNonNull(client);
//        this.config = Objects.requireNonNull(config);
//        this.donkeyExecutor = Objects.requireNonNull(donkeyExecutor);
//        this.fileFilter = Objects.requireNonNull(fileFilter);
//        this.printer = Objects.requireNonNull(printer);
//        this.snapshotSelector = Objects.requireNonNull(snapshotSelector);
//        this.snapshotTally = Objects.requireNonNull(snapshotTally);
//        this.signatureTally = Objects.requireNonNull(signatureTally);
//    }
//
//    /**
//     * Returns a new instance.
//     *
//     * @param backup not null
//     * @return a new instance, or null if it could not be created
//     */
//    public SnapshotDownloader of(Backup backup) {
//        List<Integer> snapshots = snapshotSelector.apply(backup);
//        String backupUdid = backup.udidString();
//
//        if (snapshots.isEmpty()) {
//            logger.warn("-- newInstance() > no resolved snapshots, backup: {}", backupUdid);
//            return null;
//        }
//
//        try {
//            return SnapshotDownloader.newInstance(
//                    client,
//                    backup,
//                    donkeyExecutor,
//                    fileFilter,
//                    KeyBagFactory.from(
//                            client.getKeys(
//                                    backup.udid())),
//                    printer,
//                    snapshots,
//                    config.toHuntFirstSnapshot(),
//                    snapshotTally,
//                    signatureTally);
//
//        } catch (IOException ex) {
//            logger.warn("-- newInstance() > unable to create instance, backup: {} exception: {}", backupUdid, ex);
//            printer.println(Level.WARN, "Unable to acquire backup: " + backupUdid, ex);
//            return null;
//        } catch (BadDataException ex) {
//            logger.warn("-- newInstance() > unable to acquire keybag, backup: {} exception: {}", backupUdid, ex);
//            printer.println(Level.WARN, "Unable to acquire keybag for backup: " + backupUdid);
//            return null;
//        }
//    }
//}
