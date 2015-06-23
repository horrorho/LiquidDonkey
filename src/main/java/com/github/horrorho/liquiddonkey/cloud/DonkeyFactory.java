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
import com.github.horrorho.liquiddonkey.cloud.file.Directory;
import com.github.horrorho.liquiddonkey.cloud.file.LocalFileWriter;
import com.github.horrorho.liquiddonkey.cloud.file.LocalFileFilter;
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBag;
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBagTools;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.pipe.ArgumentExceptionPair;
import com.github.horrorho.liquiddonkey.pipe.Piper;
import com.github.horrorho.liquiddonkey.settings.config.DirectoryConfig;
import com.github.horrorho.liquiddonkey.settings.config.DonkeyFactoryConfig;
import com.github.horrorho.liquiddonkey.util.CallableFunction;
import com.google.protobuf.ByteString;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Donkey factory.
 * <p>
 * Donkeys are {@link CallableFunction} threaded instances that manage snapshot downloads.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class DonkeyFactory {

    private static final Logger logger = LoggerFactory.getLogger(DonkeyFactory.class);

    /**
     * Returns a new instance.
     *
     * @param config not null
     * @param directoryConfig not null
     * @param printer not null
     * @return a new instance, not null
     */
    public static DonkeyFactory newInstance(DonkeyFactoryConfig config, DirectoryConfig directoryConfig, Printer printer) {

        return new DonkeyFactory(
                printer,
                directoryConfig.base(),
                config.batchSizeBytes(),
                directoryConfig.isFlat(),
                directoryConfig.isCombined(),
                config.isAggressive(),
                config.toForceOverwrite(),
                config.toSetLastModifiedTime());
    }

    private final Path backupFolder;
    private final Printer printer;
    private final long batchSizeBytes;
    private final boolean isFlat;
    private final boolean isCombined;
    private final boolean isAggressive;
    private final boolean toForceOverwrite;
    private final boolean toSetLastModifiedTime;

    DonkeyFactory(
            Printer printer,
            Path backupFolder,
            long batchSizeBytes,
            boolean isFlat,
            boolean isCombined,
            boolean isAggressive,
            boolean toForceOverwrite,
            boolean toSetLastModifiedTime) {

        this.backupFolder = Objects.requireNonNull(backupFolder);
        this.printer = Objects.requireNonNull(printer);
        this.batchSizeBytes = batchSizeBytes;
        this.isFlat = isFlat;
        this.isCombined = isCombined;
        this.isAggressive = isAggressive;
        this.toForceOverwrite = toForceOverwrite;
        this.toSetLastModifiedTime = toSetLastModifiedTime;
    }

    /**
     * Returns a new instance.
     *
     * @param client not null
     * @param backup not null
     * @param keyBag not null
     * @param snapshot the required snapshot
     * @param signatureToFileMap the required files, not null
     * @return a new instance, not null
     */
    public
            CallableFunction<Iterator<Map<ByteString, Set<ICloud.MBSFile>>>, List<ArgumentExceptionPair<Map<ByteString, Set<ICloud.MBSFile>>>>>
            from(
                    Client client,
                    Backup backup,
                    KeyBag keyBag,
                    int snapshot,
                    ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatureToFileMap) {

                logger.trace("<< newInstance()");

                Directory directory = Directory.newInstance(
                        backupFolder.resolve(backup.udidString()),
                        isFlat,
                        isCombined);

                Predicate<ICloud.MBSFile> localFileFilter = toForceOverwrite
                        ? (file) -> false
                        : LocalFileFilter.newInstance(
                                directory,
                                snapshot,
                                toSetLastModifiedTime);

                Batcher<ByteString, Set<ICloud.MBSFile>> batcher
                        = Batcher.<ByteString, Set<ICloud.MBSFile>>newInstance(
                                signatureToFileMap,
                                files -> files.stream().mapToLong(ICloud.MBSFile::getSize).findAny().orElse(0),
                                files -> !files.stream().allMatch(localFileFilter),
                                batchSizeBytes);

                LocalFileWriter writer = LocalFileWriter.newInstance(
                        KeyBagTools.newInstance(
                                keyBag),
                        directory,
                        printer,
                        toSetLastModifiedTime);

                BatchDownloader downloader = BatchDownloader.newInstance(client,
                        backup.udid(),
                        snapshot,
                        writer,
                        ChunkListDownloader.newInstance(
                                client,
                                isAggressive));

                Piper<Map<ByteString, Set<ICloud.MBSFile>>> pipe
                        = Piper.<Map<ByteString, Set<ICloud.MBSFile>>>newInstance(downloader, isAggressive);

                logger.trace(">> newInstance()");
                return CallableFunction.newInstance(batcher, pipe);
            }
}
