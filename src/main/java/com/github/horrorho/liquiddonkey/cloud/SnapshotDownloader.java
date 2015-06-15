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
import com.github.horrorho.liquiddonkey.cloud.file.LocalFileWriter;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer.FileGroups;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import com.github.horrorho.liquiddonkey.cloud.store.ChunkListStore;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SnapshotDownloader.
 *
 * @author ahseya
 */
@NotThreadSafe
public final class SnapshotDownloader implements Consumer<Map<ByteString, Set<MBSFile>>> {

    /**
     * Returns a new instance.
     *
     * @param client not null
     * @param backupUdid not null
     * @param snapshot not null
     * @param writer not null
     * @param chunkListDownloader not null
     * @return a new instance, not null
     */
    public static SnapshotDownloader newInstance(
            Client client,
            ByteString backupUdid,
            int snapshot,
            LocalFileWriter writer,
            ChunkListDownloader chunkListDownloader) {

        return new SnapshotDownloader(client, backupUdid, snapshot, writer, chunkListDownloader);
    }

    private static final Logger logger = LoggerFactory.getLogger(SnapshotDownloader.class);

    private final Client client;
    private final ByteString backupUdid;
    private final LocalFileWriter writer;
    private final ChunkListDownloader chunkListDownloader;
    private final int snapshot;

    SnapshotDownloader(
            Client client,
            ByteString backupUdid,
            int snapshot,
            LocalFileWriter writer,
            ChunkListDownloader chunkListDownloader) {

        this.client = Objects.requireNonNull(client);
        this.backupUdid = Objects.requireNonNull(backupUdid);
        this.snapshot = Objects.requireNonNull(snapshot);
        this.writer = Objects.requireNonNull(writer);
        this.chunkListDownloader = chunkListDownloader;
    }

    @Override
    public void accept(Map<ByteString, Set<ICloud.MBSFile>> signatureToFileSet) {
        logger.trace("<< accept() < requested: {}", signatureToFileSet.size());

        if (signatureToFileSet.isEmpty()) {
            logger.trace("<< accept() > empty list");
            return;
        }

        try {
            List<ICloud.MBSFile> download = signatureToFileSet.entrySet().stream()
                    .map(Map.Entry::getValue)
                    .flatMap(Set::stream)
                    .collect(Collectors.toList());

            FileGroups fileGroups = client.getFileGroups(backupUdid, snapshot, download);

            downloadFileGroups(fileGroups, signatureToFileSet);

        } catch (IOException ex) {
            logger.trace(">> accept() > exception: ", ex);
            throw new UncheckedIOException(ex);
        }

        logger.trace(">> accept()");
    }

    private void downloadFileGroups(
            ChunkServer.FileGroups fileGroups,
            Map<ByteString, Set<ICloud.MBSFile>> signatureToFileSet
    ) throws IOException {

        for (ChunkServer.FileChecksumStorageHostChunkLists group : fileGroups.getFileGroupsList()) {
            ChunkListStore store = chunkListDownloader.download(group);

            group.getFileChecksumChunkReferencesList().stream().forEach((fileChecksumChunkReference) -> {
                // Files with identical signatures/ hash.
                Set<ICloud.MBSFile> files = signatureToFileSet.get(fileChecksumChunkReference.getFileChecksum());

                // Reassemble the files from the chunk store via the file-chunk references.
                write(snapshot, files, store, fileChecksumChunkReference.getChunkReferencesList());

                // TODO iTunes flat style.
            });
        }
    }

    void write(
            int snapshot,
            Collection<MBSFile> files,
            ChunkListStore storage,
            List<ChunkServer.ChunkReference> chunkReferences) {

        files.stream().forEach(file -> write(snapshot, file, storage, chunkReferences));
    }

    void write(
            int snapshot,
            MBSFile file,
            ChunkListStore storage,
            List<ChunkServer.ChunkReference> chunkReferences) {

        writer.write(snapshot, file, output -> storage.write(chunkReferences, output));
    }
}
