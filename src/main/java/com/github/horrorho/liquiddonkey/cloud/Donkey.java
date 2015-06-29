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
import com.github.horrorho.liquiddonkey.cloud.store.ChunkListStore;
import com.github.horrorho.liquiddonkey.cloud.store.MemoryStore;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import net.jcip.annotations.NotThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BatchDownloader.
 *
 * @author ahseya
 */
@NotThreadSafe
public final class Donkey implements Callable<Boolean> {

    public static Donkey newInstance(
            Http http,
            Snapshot snapshot,
            Iterator<Map<ByteString, Set<ICloud.MBSFile>>> iterator,
            ConcurrentMap<Boolean, ConcurrentMap<ByteString, Set<ICloud.MBSFile>>> results,
            ChunkDecrypter decrypter,
            LocalFileWriter writer,
            boolean isAggressive,
            int attempts) {

        return new Donkey(
                http,
                snapshot.backup().account().client(),
                snapshot.backup().udid(),
                snapshot.id(),
                iterator,
                results,
                decrypter,
                writer,
                isAggressive,
                attempts);
    }

    public static Donkey newInstance(
            Http http,
            Client client,
            ByteString backupUdid,
            int snapshot,
            Iterator<Map<ByteString, Set<ICloud.MBSFile>>> iterator,
            ConcurrentMap<Boolean, ConcurrentMap<ByteString, Set<ICloud.MBSFile>>> results,
            ChunkDecrypter decrypter,
            LocalFileWriter writer,
            boolean isAggressive,
            int attempts) {

        return new Donkey(
                http,
                client,
                backupUdid,
                snapshot,
                iterator,
                results,
                decrypter,
                writer,
                isAggressive,
                attempts);
    }

    private static final Logger logger = LoggerFactory.getLogger(Donkey.class);

    private final Http http;
    private final Client client;
    private final ByteString backupUdid;
    private final int snapshot;
    private final Iterator<Map<ByteString, Set<ICloud.MBSFile>>> iterator;
    private final ConcurrentMap<Boolean, ConcurrentMap<ByteString, Set<ICloud.MBSFile>>> results;
    private final ChunkDecrypter decrypter;
    private final LocalFileWriter writer;
    private final boolean isAggressive;
    private final int attempts;

    Donkey(
            Http http,
            Client client,
            ByteString backupUdid,
            int snapshot,
            Iterator<Map<ByteString, Set<ICloud.MBSFile>>> iterator,
            ConcurrentMap<Boolean, ConcurrentMap<ByteString, Set<ICloud.MBSFile>>> results,
            ChunkDecrypter decrypter,
            LocalFileWriter writer,
            boolean isAggressive,
            int attempts) {

        this.http = Objects.requireNonNull(http);
        this.client = Objects.requireNonNull(client);
        this.backupUdid = Objects.requireNonNull(backupUdid);
        this.snapshot = snapshot;
        this.iterator = Objects.requireNonNull(iterator);
        this.results = Objects.requireNonNull(results);
        this.decrypter = Objects.requireNonNull(decrypter);
        this.writer = Objects.requireNonNull(writer);
        this.isAggressive = isAggressive;
        this.attempts = attempts;
    }

    @Override
    public Boolean call() throws Exception {
        logger.trace("<< call() < {}");

        while (iterator.hasNext()) {
            Map<ByteString, Set<ICloud.MBSFile>> signatures = iterator.next();

            try {
                if (signatures.isEmpty()) {
                    logger.warn("-- call() > empty signature list");
                } else {
                    download(signatures);
                    addAll(true, signatures);
                }
            } catch (IOException ex) {
                // TODO work through
                logger.warn("-- call() > exception: ", ex);
                addAll(false, signatures);

                Throwable cause = ex.getCause();
                if (cause instanceof HttpResponseException) {
                    if (((HttpResponseException) cause).getStatusCode() == 401) {
                        throw new AuthenticationException(ex);
                    }
                }

                if (!isAggressive) {
                    throw new UncheckedIOException(ex);
                }
            }
        }
        logger.trace(">> call()");
        return true;
    }

    void addAll(boolean key, Map<ByteString, Set<ICloud.MBSFile>> signatures) {
        results.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).putAll(signatures);
    }

    public void download(Map<ByteString, Set<ICloud.MBSFile>> signatures) throws IOException {
        logger.trace("<< download() < {}", signatures.size());

        List<ICloud.MBSFile> files = signatures.entrySet().stream()
                .map(Map.Entry::getValue)
                .flatMap(Set::stream)
                .collect(Collectors.toList());

        FileGroups fileGroups = client.getFileGroups(http, backupUdid, snapshot, files);
        downloadFileGroups(fileGroups, signatures);

        logger.trace(">> download()");
    }

    private void downloadFileGroups(
            ChunkServer.FileGroups fileGroups,
            Map<ByteString, Set<ICloud.MBSFile>> signatureToFileSet
    ) throws IOException {

        for (ChunkServer.FileChecksumStorageHostChunkLists group : fileGroups.getFileGroupsList()) {
            ChunkListStore store = download(group);

            group.getFileChecksumChunkReferencesList().stream().forEach((fileChecksumChunkReference) -> {
                // Files with identical signatures/ hash.
                Set<ICloud.MBSFile> files = signatureToFileSet.get(fileChecksumChunkReference.getFileChecksum());

                // Reassemble the files from the chunk store via the file-chunk references. 
                files.stream().forEach(file
                        -> writer.write(
                                snapshot,
                                file,
                                output -> store.write(fileChecksumChunkReference.getChunkReferencesList(), output)));
                // TODO iTunes flat style.
            });
        }
    }

    public ChunkListStore download(ChunkServer.FileChecksumStorageHostChunkLists group) throws IOException {
        logger.trace("<< download() < group count : {}", group.getStorageHostChunkListCount());

        // TODO memory or disk based depending on size
        MemoryStore.Builder builder = MemoryStore.builder();
        for (ChunkServer.StorageHostChunkList chunkList : group.getStorageHostChunkListList()) {
            builder.add(download(chunkList));
        }
        ChunkListStore storage = builder.build();

        logger.trace(">> download() > container count : {}", storage.size());
        return storage;
    }

    List<byte[]> download(ChunkServer.StorageHostChunkList chunkList) throws IOException {
        // Recursive.
        return chunkList.getChunkInfoCount() == 0
                ? new ArrayList<>()
                : download(chunkList, 0);
    }

    List<byte[]> download(ChunkServer.StorageHostChunkList chunkList, int attempt) throws IOException {
        // Recursive.
        List<byte[]> decrypted = attempt++ == attempts
                ? new ArrayList<>()
                : decrypter.decrypt(chunkList, client.chunks(http, chunkList));

        return decrypted == null
                ? download(chunkList, attempt)
                : decrypted;
    }

    @Override
    public String toString() {
        return "Donkey{" + "http=" + http + ", client=" + client + ", backupUdid=" + backupUdid + ", snapshot="
                + snapshot + ", iterator=" + iterator + ", decrypter=" + decrypter + ", writer=" + writer
                + ", isAggressive=" + isAggressive + ", attempts=" + attempts + '}';
    }
}
