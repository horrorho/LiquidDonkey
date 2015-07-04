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
import com.github.horrorho.liquiddonkey.iofunction.IOBiConsumer;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import static com.github.horrorho.liquiddonkey.settings.Markers.CLOUD;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
@ThreadSafe
public final class ChunkManager {

    public static ChunkManager from(
            ChunkServer.FileChecksumStorageHostChunkLists fileChecksumStorageHostChunkLists,
            IOBiConsumer<ByteString, IOFunction<OutputStream, Long>> writer) {

        logger.trace("<< from()");
        logger.debug(CLOUD, "-- from() > fileChecksumStorageHostChunkLists: {}", fileChecksumStorageHostChunkLists);

        ConcurrentMap<ByteString, List<ChunkReference>> signatureToChunkReferences = new ConcurrentHashMap<>();
        ConcurrentMap<ByteString, Set<Long>> signatureToContainers = new ConcurrentHashMap<>(); // Concurrent Set
        ConcurrentMap<Long, Set<ByteString>> containerToSignatures = new ConcurrentHashMap<>(); // Concurrent Set

        fileChecksumStorageHostChunkLists.getFileChecksumChunkReferencesList().parallelStream().forEach(references -> {
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
                completed,
                writer);

        logger.trace(">> from()");
        return chunkManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(ChunkManager.class);

    private final Store store;
    private final ConcurrentMap<Long, Set<ByteString>> containerToSignatures;
    private final ConcurrentMap<ByteString, Set<Long>> signatureToContainers;
    private final ConcurrentMap<ByteString, List<ChunkReference>> signatureToChunkReferences;
    private final Set<ByteString> completed;    // Concurrent set required
    private final IOBiConsumer<ByteString, IOFunction<OutputStream, Long>> writer;

    ChunkManager(
            Store store,
            ConcurrentMap<Long, Set<ByteString>> containerToSignatures,
            ConcurrentMap<ByteString, Set<Long>> signatureToContainers,
            ConcurrentMap<ByteString, List<ChunkReference>> signatureToChunkReferences,
            Set<ByteString> completed,
            IOBiConsumer<ByteString, IOFunction<OutputStream, Long>> writer) {

        this.store = Objects.requireNonNull(store);
        this.containerToSignatures = Objects.requireNonNull(containerToSignatures);
        this.signatureToContainers = Objects.requireNonNull(signatureToContainers);
        this.signatureToChunkReferences = Objects.requireNonNull(signatureToChunkReferences);
        this.completed = Objects.requireNonNull(completed);
        this.writer = writer;
    }

    void put(ChunkServer.ChunkReference chunkReference, byte[] chunkData) {
        if (store.contains(chunkReference)) {
            logger.warn("--put () > duplicate chunkData ignored. chunkReference: ", chunkReference);
            return;
        }

        store.put(chunkReference, chunkData);
        ChunkManager.this.process(chunkReference.getContainerIndex());
    }

    void process(long containerIndex) {
        for (ByteString signature : containerToSignatures.get(containerIndex)) {
            ChunkManager.this.process(signature);
        }
    }

    void process(ByteString signature) {
        List<ChunkReference> references = signatureToChunkReferences.get(signature);

        // Exit if already completed or not all chunks references are available.
        if (references == null || !store.contains(references)) {
            return;
        }

        // Remove chunk references. Null if another thread beat us to it.
        if (signatureToChunkReferences.remove(signature) == null) {
            return;
        }

        // Completed. We won't retry on writer failure.
        completed.add(signature);

        // Writer.
        try {
            writer.accept(signature, output -> store.write(references, output));
        } catch (IOException ex) {
            logger.warn("--process() > exception: ", ex);
        }

        // Remove obsolete references and chunk data.
        signatureToContainers.get(signature).forEach(containerIndex -> {
            if (containerToSignatures.get(containerIndex).isEmpty()) {
                store.destroy(containerIndex);
            }            
        });
        
        signatureToContainers.remove(signature);
    }
}
