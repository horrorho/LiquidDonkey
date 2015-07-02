/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation list (the "Software"), to deal
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

import com.github.horrorho.liquiddonkey.cloud.file.Directory;
import com.github.horrorho.liquiddonkey.cloud.file.LocalFileWriter;
import com.github.horrorho.liquiddonkey.cloud.file.LocalFileFilter;
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBagTools;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.settings.config.EngineConfig;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import com.google.protobuf.ByteString;
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
 * Donkeys are {@link CallableFunction} threaded instances that manage from downloads.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class DonkeyFactory {

    public static DonkeyFactory newInstance(EngineConfig engineConfig, FileConfig fileConfig, Printer printer) {
        return new DonkeyFactory(engineConfig, fileConfig, printer);
    }

    private static final Logger logger = LoggerFactory.getLogger(DonkeyFactory.class);

    private final EngineConfig engineConfig;
    private final FileConfig fileConfig;
    private final Printer printer;

    DonkeyFactory(EngineConfig engineConfig, FileConfig fileConfig, Printer printer) {
        this.engineConfig = engineConfig;
        this.fileConfig = fileConfig;
        this.printer = printer;
    }

    Donkey from(
            Http http,
            Snapshot snapshot,
            ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatureToFileMap,
            ConcurrentMap<Boolean, ConcurrentMap<ByteString, Set<ICloud.MBSFile>>> results) {

        logger.trace("<< from()");

        Backup backup = snapshot.backup();

        Directory directory = Directory.newInstance(backup.udid(), fileConfig);

        Predicate<ICloud.MBSFile> localFileFilter = engineConfig.toForceOverwrite()
                ? (file) -> false
                : LocalFileFilter.newInstance(
                        directory,
                        snapshot.id(),
                        engineConfig.toSetLastModifiedTimestamp()).negate();

        Bundler bundler = Bundler.wrap(signatureToFileMap, localFileFilter, engineConfig.batchSizeMinimumBytes());

        LocalFileWriter writer = LocalFileWriter.newInstance(
                KeyBagTools.newInstance(backup.keybag()),
                directory,
                printer,
                engineConfig.toSetLastModifiedTimestamp());

        Donkey donkey = new Donkey(
                http,
                snapshot.backup().account().client(),
                snapshot.backup().udid(),
                snapshot.id(),
                bundler,
                results,
                ChunkDecrypter.newInstance(),
                writer,
                engineConfig.isAggressive(),
                engineConfig.retryCount());

        logger.trace(">> from()");
        return donkey;
    }
}
