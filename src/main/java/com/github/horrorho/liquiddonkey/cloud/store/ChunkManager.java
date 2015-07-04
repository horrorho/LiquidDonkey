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
import static com.github.horrorho.liquiddonkey.settings.Markers.CLOUD;
import com.google.protobuf.ByteString;
import java.io.OutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
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
 *
 * @author Ahseya
 */
@ThreadSafe
public final class ChunkManager {

    public static ChunkManager from(ChunkServer.FileChecksumStorageHostChunkLists fileGroup) {
        logger.trace("<< from()");
        logger.debug(CLOUD, "-- from() > fileGroup: {}", fileGroup);

        ConcurrentMap<ByteString, List<ChunkReference>> signatureToChunkReferences = new ConcurrentHashMap<>();
        ConcurrentMap<ByteString, Set<Long>> signatureToContainers = new ConcurrentHashMap<>(); // Concurrent Set
        ConcurrentMap<Long, Set<ByteString>> containerToSignatures = new ConcurrentHashMap<>(); // Concurrent Set

        fileGroup.getFileChecksumChunkReferencesList().parallelStream().forEach(references -> {
            ByteString signature = references.getFileChecksum();
            List<ChunkReference> list = references.getChunkReferencesList();

            signatureToChunkReferences.put(signature, list);

            list.stream().forEach(reference -> {
                long containerIndex = reference.getContainerIndex();

                signatureToContainers
                        .putIfAbsent(signature, Collections.<Long>newSetFromMap(new ConcurrentHashMap<>()));
                signatureToContainers.get(signature).add(containerIndex);

                containerToSignatures
                        .putIfAbsent(containerIndex, Collections.<ByteString>newSetFromMap(new ConcurrentHashMap<>()));
                containerToSignatures.get(containerIndex).add(signature);
            });
        });

        // Concurrent Set
        Set<ByteString> completed = Collections.<ByteString>newSetFromMap(new ConcurrentHashMap<>());

        logger.debug(CLOUD, "-- from() > signatureToChunkReferences: {}", signatureToChunkReferences);
        logger.debug(CLOUD, "-- from() > signatureToContainers: {}", signatureToContainers);
        logger.debug(CLOUD, "-- from() > containerToSignatures: {}", containerToSignatures);

        ChunkManager chunkManager = new ChunkManager(
                MemoryStore.newInstance(),
                containerToSignatures,
                signatureToContainers,
                signatureToChunkReferences,
                completed);

        logger.trace(">> from()");
        return chunkManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(ChunkManager.class);

    private final Store store;
    private final ConcurrentMap<Long, Set<ByteString>> containerToSignatures;
    private final ConcurrentMap<ByteString, Set<Long>> signatureToContainers;
    private final ConcurrentMap<ByteString, List<ChunkReference>> signatureToChunkReferences;
    private final Set<ByteString> completed;    // Concurrent set required

    ChunkManager(
            Store store,
            ConcurrentMap<Long, Set<ByteString>> containerToSignatures,
            ConcurrentMap<ByteString, Set<Long>> signatureToContainers,
            ConcurrentMap<ByteString, List<ChunkReference>> signatureToChunkReferences,
            Set<ByteString> completed) {

        this.store = Objects.requireNonNull(store);
        this.containerToSignatures = Objects.requireNonNull(containerToSignatures);
        this.signatureToContainers = Objects.requireNonNull(signatureToContainers);
        this.signatureToChunkReferences = Objects.requireNonNull(signatureToChunkReferences);
        this.completed = Objects.requireNonNull(completed);
    }

    public Map<ByteString, IOFunction<OutputStream, Long>> put(long containerIndex, List<byte[]> data) {

        logger.trace("<< put() < containerIndex: {} chunkDataSize: {}", containerIndex, data.size());

        for (int chunkIndex = 0; chunkIndex < data.size(); chunkIndex++) {
            if (store.contains(containerIndex, chunkIndex)) {
                logger.warn("--put () > duplicate data ignored, containerIndex: {} chunkIndex: {}",
                        containerIndex, chunkIndex);
            } else {
                store.put(containerIndex, chunkIndex, data.get(chunkIndex));
            }
        }

        Map<ByteString, IOFunction<OutputStream, Long>> writers = process(containerIndex);

        logger.trace(">> put() > writers: {}", writers);
        return writers;
    }

    Map<ByteString, IOFunction<OutputStream, Long>> process(long containerIndex) {
        return containerToSignatures.get(containerIndex).stream()
                .map(signature -> new SimpleEntry<>(signature, process(signature)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    IOFunction<OutputStream, Long> process(ByteString signature) {
        List<ChunkReference> references = signatureToChunkReferences.get(signature);

        // Exit if already completed or not all chunks references are available.
        if (references == null || !store.contains(references)) {
            return null;
        }

        // Remove chunk references. Null if another thread beat us to it.
        if (signatureToChunkReferences.remove(signature) == null) {
            return null;
        }

        // Completed. We won't retry on writer failure.
        completed.add(signature);

        // Writer.
        return output -> store.write(references, output);
    }

    public void destroy(ByteString signature) {
        logger.trace("<< destroy() < signature: {}", signature);

        // Destroy unreferenced data (or risk leaking memory).
        signatureToContainers.get(signature).forEach(containerIndex -> {
            if (containerToSignatures.get(containerIndex).isEmpty()) {
                store.destroy(containerIndex);
            }
        });

        signatureToContainers.remove(signature);

        logger.trace(">> destroy()");
    }
}
