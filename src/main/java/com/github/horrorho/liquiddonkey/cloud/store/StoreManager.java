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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer.ChunkReference;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import static com.github.horrorho.liquiddonkey.settings.Markers.STORE;
import com.google.protobuf.ByteString;
import java.io.OutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StoreManager.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class StoreManager {

    public static StoreManager from(ChunkServer.FileChecksumStorageHostChunkLists fileGroup) {
        logger.trace("<< from()");
        logger.debug(STORE, "-- from() > fileGroup: {}", fileGroup);

        ConcurrentMap<ByteString, List<ChunkReference>> signatureToChunkReferences = new ConcurrentHashMap<>();
        ConcurrentMap<ByteString, Set<Long>> signatureToContainers = new ConcurrentHashMap<>(); // Concurrent Map/ Set
        ConcurrentMap<Long, Set<ByteString>> containerToSignatures = new ConcurrentHashMap<>(); // Concurrent Map/ Set

        fileGroup.getFileChecksumChunkReferencesList().parallelStream().forEach(references -> {
            ByteString signature = references.getFileChecksum();
            List<ChunkReference> list = references.getChunkReferencesList();

            signatureToChunkReferences.put(signature, list);

            list.stream().forEach(reference -> {
                long containerIndex = reference.getContainerIndex();

                signatureToContainers
                        .computeIfAbsent(signature,
                                s -> Collections.<Long>newSetFromMap(new ConcurrentHashMap<>()))
                        .add(containerIndex);

                containerToSignatures
                        .computeIfAbsent(containerIndex,
                                i -> Collections.<ByteString>newSetFromMap(new ConcurrentHashMap<>()))
                        .add(signature);

            });
        });

        logger.debug(STORE, "-- from() > signatureToChunkReferences: {}", signatureToChunkReferences);
        logger.debug(STORE, "-- from() > signatureToContainers: {}", signatureToContainers);
        logger.debug(STORE, "-- from() > containerToSignatures: {}", containerToSignatures);

        StoreManager chunkManager = new StoreManager(
                MemoryStore.newInstance(),
                containerToSignatures,
                signatureToContainers,
                signatureToChunkReferences);

        logger.trace(">> from()");
        return chunkManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(StoreManager.class);

    private final Store store;
    private final ConcurrentMap<Long, Set<ByteString>> containerToSignatures;   // Requires concurrent Set
    private final ConcurrentMap<ByteString, Set<Long>> signatureToContainers;   // Requires concurrent Set
    private final ConcurrentMap<ByteString, List<ChunkReference>> signatureToChunkReferences;

    StoreManager(
            Store store,
            ConcurrentMap<Long, Set<ByteString>> containerToSignatures,
            ConcurrentMap<ByteString, Set<Long>> signatureToContainers,
            ConcurrentMap<ByteString, List<ChunkReference>> signatureToChunkReferences) {

        this.store = Objects.requireNonNull(store);
        this.containerToSignatures = Objects.requireNonNull(containerToSignatures);
        this.signatureToContainers = Objects.requireNonNull(signatureToContainers);
        this.signatureToChunkReferences = Objects.requireNonNull(signatureToChunkReferences);
    }

    public Map<ByteString, IOFunction<OutputStream, Long>>
            put(long groupIndex, long containerIndex, List<byte[]> data) {

        logger.trace("<< put() < containerIndex: {} chunkDataSize: {}", containerIndex, data.size());

        if (!store.put(groupIndex, containerIndex, data)) {
            logger.warn("-- put() > overwritten store container: {}", containerIndex);
        }

        Map<ByteString, IOFunction<OutputStream, Long>> writers = process(groupIndex, containerIndex);
        if (writers != null) {
            clear(groupIndex, writers.keySet());
        }

        logger.trace(">> put() > writers: {}", writers);
        return writers;
    }

    Map<ByteString, IOFunction<OutputStream, Long>> process(long groupIndex, long containerIndex) {

        Map<ByteString, IOFunction<OutputStream, Long>> writers = containerToSignatures.get(containerIndex).stream()
                .map(signature -> new SimpleEntry<>(signature, process(groupIndex, signature)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return writers;
    }

    IOFunction<OutputStream, Long> process(long groupIndex, ByteString signature) {
        List<ChunkReference> references = signatureToChunkReferences.get(signature);

        // Exit if any chunks are missing.
        if (!store.contains(groupIndex, references)) {
            return null;
        }

        // We have all the chunks, remove reference. Null if another thread beat us to it.
        if (signatureToChunkReferences.remove(signature) == null) {
            return null;
        }

        // Writer.
        return store.writer(groupIndex, references);
    }

    void clear(long groupIndex, Set<ByteString> signatures) {
        signatures.forEach(signature -> {
            signatureToContainers.get(signature).stream().forEach(index -> {
                containerToSignatures.get(index).remove(signature);

                if (containerToSignatures.get(index).isEmpty()) {
                    containerToSignatures.remove(index);
                    store.remove(groupIndex, index);
                }
            });
            signatureToContainers.remove(signature);
        });
    }
}
