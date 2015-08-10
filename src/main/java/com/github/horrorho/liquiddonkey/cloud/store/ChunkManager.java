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
package com.github.horrorho.liquiddonkey.cloud.store;

import com.github.horrorho.liquiddonkey.util.BiMapSet;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.settings.Markers;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * ChunkManager.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class ChunkManager {

    public static ChunkManager from(List<ChunkServer.FileChecksumStorageHostChunkLists> fileGroupsList) {
        logger.trace("<< from()");

        ConcurrentMap<ByteString, List<ByteString>> signatureToChunks = fileGroupsList
                .stream()
                .map(ChunkManager::entries)
                .flatMap(Collection::stream)
                .collect(Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, b) -> {
                                    if (!Objects.equals(a, b)) {
                                        // Improbable.
                                        logger.warn("-- toMap() > signature collision: {} {}", a, b);
                                    }
                                    return a;
                                }));

        logger.debug(marker, "-- from() > signatureToChunks: {}", Bytes.hex(signatureToChunks, Bytes::hex));

        ChunkManager chunkManager = new ChunkManager(
                MemoryStore.create(),
                BiMapSet.from(signatureToChunks),
                signatureToChunks,
                ChunkDecrypter::create);

        logger.trace(">> from()");
        return chunkManager;
    }

    static List<Map.Entry<ByteString, List<ByteString>>> entries(ChunkServer.FileChecksumStorageHostChunkLists fileGroup) {
        List<List<ByteString>> containerToChunkChecksums = containerToChunkChecksums(fileGroup);

        Function<ChunkServer.ChunkReference, ByteString> toFileChecksum
                = reference -> toFileChecksum(containerToChunkChecksums, reference);

        Function<ChunkServer.FileChecksumChunkReferences, Map.Entry<ByteString, List<ByteString>>> toMapEntry
                = references -> toMapEntry(toFileChecksum, references);

        return fileGroup.getFileChecksumChunkReferencesList().stream()
                .map(toMapEntry::apply)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    static List<List<ByteString>> containerToChunkChecksums(ChunkServer.FileChecksumStorageHostChunkLists fileGroup) {
        return fileGroup.getStorageHostChunkListList()
                .stream()
                .map(ChunkServer.StorageHostChunkList::getChunkInfoList)
                .map(chunkInfos -> chunkInfos
                        .stream()
                        .map(chunkInfo -> chunkInfo.getChunkChecksum()).collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    static ByteString toFileChecksum(
            List<List<ByteString>> containerToChunkChecksums,
            ChunkServer.ChunkReference reference) {

        int container = (int) reference.getContainerIndex();
        int index = (int) reference.getChunkIndex();

        if (containerToChunkChecksums.size() <= container) {
            logger.warn("-- toFileChecksum() > container out of bounds: {}", reference);
            return null;
        }
        List<ByteString> chunkChecksums = containerToChunkChecksums.get(container);

        if (chunkChecksums.size() <= index) {
            logger.warn("-- toFileChecksum() > index out of bounds: {}", reference);
            return null;
        }
        return chunkChecksums.get(index);
    }

    static Map.Entry<ByteString, List<ByteString>> toMapEntry(
            Function<ChunkServer.ChunkReference, ByteString> toFileChecksum,
            ChunkServer.FileChecksumChunkReferences references) {

        List<ByteString> chunkChecksums = references.getChunkReferencesList().stream()
                .map(toFileChecksum)
                .collect(Collectors.toList());

        return chunkChecksums.contains(null)
                ? null
                : new SimpleEntry<>(references.getFileChecksum(), chunkChecksums);
    }

    private static final Logger logger = LoggerFactory.getLogger(ChunkManager.class);
    private static final Marker marker = MarkerFactory.getMarker(Markers.STORE);

    // Chunks referenced by their checksums. SHA-256, collision risk negligible.
    private final Store<ByteString> store;
    private final BiMapSet<ByteString, ByteString> signaturesChunks;
    private final ConcurrentMap<ByteString, List<ByteString>> signatureToChunks;
    private final Supplier<ChunkDecrypter> decrypters;

    ChunkManager(
            Store<ByteString> store,
            BiMapSet<ByteString, ByteString> signaturesChunks,
            ConcurrentMap<ByteString, List<ByteString>> signatureToChunks,
            Supplier<ChunkDecrypter> decrypters) {

        this.store = Objects.requireNonNull(store);
        this.signaturesChunks = Objects.requireNonNull(signaturesChunks);
        this.signatureToChunks = Objects.requireNonNull(signatureToChunks);
        this.decrypters = Objects.requireNonNull(decrypters);
    }

    public Map<ByteString, DataWriter> put(List<ChunkServer.ChunkInfo> chunkInfoList, byte[] chunkData)
            throws BadDataException {

        Objects.requireNonNull(chunkInfoList);
        Objects.requireNonNull(chunkData);

        logger.trace("<< put() < chunkInfoList length: {} chunkData length: {}", chunkInfoList.size(), chunkData.length);

        List<byte[]> chunks = decrypters.get().decrypt(chunkInfoList, chunkData);

        for (int i = 0; i < chunkInfoList.size(); i++) {
            if (!store.put(chunkInfoList.get(i).getChunkChecksum(), chunks.get(i))) {
                logger.warn("-- put() > overwritten store container: {}", Bytes.hex(chunkInfoList.get(i).getChunkChecksum()));
            }
        }

        Map<ByteString, DataWriter> writers = process(chunkInfoList);

        logger.trace(">> put() > signatures: {}", Bytes.hex(writers.keySet()));
        return writers;
    }

    Map<ByteString, DataWriter> process(List<ChunkServer.ChunkInfo> chunkInfoList) {
        return signatures(chunkInfoList)
                .stream()
                .map(signature -> new SimpleEntry<>(signature, process(signature)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    DataWriter process(ByteString signature) {
        List<ByteString> chunks = signatureToChunks.get(signature);

        if (!store.contains(chunks) || signatureToChunks.remove(signature) == null) {
            return null;
        }

        List<DataWriter> writers = chunks.stream()
                .map(store::writer)
                .collect(Collectors.toList());

        signaturesChunks.removeKey(signature).forEach(store::remove);

        return CompoundWriter.from(writers);
    }

    public Set<ByteString> fail(List<ChunkServer.ChunkInfo> chunkInfoList) {
        logger.trace("<< fail() < chunkInfoList length: {}", chunkInfoList.size());

        Set<ByteString> failed = signatures(chunkInfoList)
                .stream()
                .filter(signature -> signatureToChunks.remove(signature) != null)
                .collect(Collectors.toSet());

        failed.forEach(signature -> signaturesChunks.removeKey(signature).forEach(store::remove));

        logger.trace(">> fail() > signatures: {}", Bytes.hex(failed));
        return failed;
    }

    Set<ByteString> signatures(List<ChunkServer.ChunkInfo> chunkInfoList) {
        return chunkInfoList.stream()
                .map(ChunkServer.ChunkInfo::getChunkChecksum)
                .map(signaturesChunks::keys)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public Set<ByteString> remainingSignatures() {
        return new HashSet<>(signatureToChunks.keySet());
    }

    public List<ByteString> remainingChunks() {
        return signatureToChunks.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
