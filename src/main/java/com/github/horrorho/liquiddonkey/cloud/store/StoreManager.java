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

import com.github.horrorho.liquiddonkey.cloud.file.SignatureWriter;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer.ChunkReference;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.printer.Level;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.settings.Markers;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * StoreManager.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class StoreManager {

    public static StoreManager from(
            ChunkServer.FileGroups fileGroups,
            SignatureWriter signatureWriter,
            Printer printer) {

        logger.trace("<< from()");
        logger.debug(marker, "-- from() > fileGroup: {}", fileGroups);

        ConcurrentMap<ByteString, Set<ChunkServer.StorageHostChunkList>> signatureToChunkList
                = new ConcurrentHashMap<>();
        ConcurrentMap<ChunkServer.StorageHostChunkList, Set<ByteString>> chunkListToSignatures
                = new ConcurrentHashMap<>();
        ConcurrentMap<ByteString, List<ChunkListReference>> signatureToChunkListReferenceList
                = new ConcurrentHashMap<>();

        fileGroups.getFileGroupsList().stream().forEach(fileGroup -> {

            List<ChunkServer.StorageHostChunkList> chunkListList = fileGroup.getStorageHostChunkListList();
            Map<Long, ChunkServer.StorageHostChunkList> containerToChunkList = new HashMap<>();
            for (int index = 0; index < chunkListList.size(); index++) {
                containerToChunkList.put((long) index, chunkListList.get(index));
            }

            fileGroup.getFileChecksumChunkReferencesList().stream().forEach(references -> {
                ByteString signature = references.getFileChecksum();
                List<ChunkReference> list = references.getChunkReferencesList();
                signatureToChunkListReferenceList.put(signature, new ArrayList<>());

                list.stream().forEach(reference -> {
                    long containerIndex = reference.getContainerIndex();
                    long chunkIndex = reference.getChunkIndex();
                    ChunkServer.StorageHostChunkList chunkList = containerToChunkList.get(containerIndex);
                    ChunkListReference chunkListReference = new ChunkListReference(chunkList, (int) chunkIndex);

                    signatureToChunkListReferenceList.get(signature).add(chunkListReference);

                    signatureToChunkList
                            .computeIfAbsent(signature, s -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                            .add(chunkList);

                    chunkListToSignatures
                            .computeIfAbsent(chunkList, i -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                            .add(signature);
                });
            });
        });
        logger.debug(marker, "-- from() > signatureToChunkListReferenceList: {}", signatureToChunkListReferenceList);
        logger.debug(marker, "-- from() > signatureToChunkList: {}", signatureToChunkList);
        logger.debug(marker, "-- from() > chunkListToSignatures: {}", chunkListToSignatures);

        StoreManager chunkManager = new StoreManager(
                MemoryStore.newInstance(),
                chunkListToSignatures,
                signatureToChunkList,
                signatureToChunkListReferenceList,
                ChunkDecrypter::newInstance,
                signatureWriter,
                printer);

        logger.trace(">> from()");
        return chunkManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(StoreManager.class);
    private static final Marker marker = MarkerFactory.getMarker(Markers.STORE);

    private final Store<ChunkServer.StorageHostChunkList> store;
    private final ConcurrentMap<ChunkServer.StorageHostChunkList, Set<ByteString>> chunkListToSignatures;   // Requires concurrent Set
    private final ConcurrentMap<ByteString, Set<ChunkServer.StorageHostChunkList>> signatureToChunkList;   // Requires concurrent Set
    private final ConcurrentMap<ByteString, List<ChunkListReference>> signatureToChunkListReferenceList;
    private final Supplier<ChunkDecrypter> decrypters;
    private final SignatureWriter signatureWriter;
    private final Printer printer;

    StoreManager(
            Store<ChunkServer.StorageHostChunkList> store,
            ConcurrentMap<ChunkServer.StorageHostChunkList, Set<ByteString>> chunkListToSignatures,
            ConcurrentMap<ByteString, Set<ChunkServer.StorageHostChunkList>> signatureToChunkList,
            ConcurrentMap<ByteString, List<ChunkListReference>> signatureToChunkListReferenceList,
            Supplier<ChunkDecrypter> decrypters,
            SignatureWriter signatureWriter,
            Printer printer) {

        this.store = Objects.requireNonNull(store);
        this.chunkListToSignatures = Objects.requireNonNull(chunkListToSignatures);
        this.signatureToChunkList = Objects.requireNonNull(signatureToChunkList);
        this.signatureToChunkListReferenceList = Objects.requireNonNull(signatureToChunkListReferenceList);
        this.decrypters = Objects.requireNonNull(decrypters);
        this.signatureWriter = Objects.requireNonNull(signatureWriter);
        this.printer = Objects.requireNonNull(printer);
    }

    public void put(ChunkServer.StorageHostChunkList chunkList, byte[] chunkData)
            throws BadDataException, IOException {

        logger.trace("<< put() < uri: {} length: {}", chunkList.getHostInfo().getUri(), chunkData.length);

        List<byte[]> chunks = decrypters.get().decrypt(chunkList, chunkData);

        if (!store.put(chunkList, chunks)) {
            logger.warn("-- put() > overwritten store container: {}", chunkList.getHostInfo().getUri());
        }

        Map<ByteString, DataWriter> writers = process(chunkList);
        if (!writers.isEmpty()) {
            clear(writers.keySet());
        }

        logger.debug("-- put() > writing signatures: {}", writers.keySet());
        write(writers);

        logger.trace(">> put()");
        return;
    }

    void write(Map<ByteString, DataWriter> writers) throws IOException {
        try {
            for (ByteString signature : writers.keySet()) {
                try (DataWriter dataWriter = writers.get(signature)) {
                    signatureWriter.write(signature, dataWriter).entrySet().stream().forEach(
                            entry -> printer.println(Level.VV,
                                    "\t" + entry.getKey().getDomain()
                                    + " " + entry.getKey().getRelativePath()
                                    + " " + entry.getValue()));
                } finally {
                    writers.remove(signature);
                }
            }
        } finally {
            writers.values().stream().forEach(dataWriter -> {
                try {
                    dataWriter.close();
                } catch (IOException ex) {
                    logger.warn("-- write() > exception on close: {}", ex);
                }
            });
        }
    }

    Map<ByteString, DataWriter> process(ChunkServer.StorageHostChunkList chunkList) {

        Map<ByteString, DataWriter> writers = chunkListToSignatures.get(chunkList).stream()
                .map(signature -> new SimpleEntry<>(signature, process(signature)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return writers;
    }

    DataWriter process(ByteString signature) {
        List<ChunkListReference> references = signatureToChunkListReferenceList.get(signature);

        // Exit if any chunks are missing.
        if (!references.stream().allMatch(reference -> store.contains(reference.chunkList(), reference.index()))) {
            return null;
        }

        // We have all the chunks, remove reference. Null if another thread beat us to it.
        if (signatureToChunkListReferenceList.remove(signature) == null) {
            return null;
        }

        // Writer.
        List<DataWriter> writers = references.stream()
                .map(reference -> store.writer(reference.chunkList(), reference.index()))
                .collect(Collectors.toList());

        return CompoundWriter.of(writers);
    }

    void clear(Set<ByteString> signatures) {

        signatures.forEach(signature -> {
            signatureToChunkList.get(signature).stream().forEach(index -> {
                chunkListToSignatures.get(index).remove(signature);

                if (chunkListToSignatures.get(index).isEmpty()) {
                    chunkListToSignatures.remove(index);
                    store.remove(index);
                }
            });
            signatureToChunkList.remove(signature);
        });
    }

    public List<ChunkServer.StorageHostChunkList> chunkListList() {
        return new ArrayList<>(chunkListToSignatures.keySet());
    }
}
// failed list?