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
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import static com.github.horrorho.liquiddonkey.settings.Markers.STORE;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.OutputStream;
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
import static org.bouncycastle.asn1.cms.CMSObjectIdentifiers.data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StoreManager.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class StoreManager {

    public static StoreManager from(ChunkServer.FileGroups fileGroups) {
        logger.trace("<< from()");
        logger.debug(STORE, "-- from() > fileGroup: {}", fileGroups);

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
        logger.debug(STORE, "-- from() > signatureToChunkListReferenceList: {}", signatureToChunkListReferenceList);
        logger.debug(STORE, "-- from() > signatureToChunkList: {}", signatureToChunkList);
        logger.debug(STORE, "-- from() > chunkListToSignatures: {}", chunkListToSignatures);

        StoreManager chunkManager = new StoreManager(
                MemoryStore.newInstance(),
                chunkListToSignatures,
                signatureToChunkList,
                signatureToChunkListReferenceList,
                ChunkDecrypter::newInstance);

        logger.trace(">> from()");
        return chunkManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(StoreManager.class);

    private final Store<ChunkServer.StorageHostChunkList> store;
    private final ConcurrentMap<ChunkServer.StorageHostChunkList, Set<ByteString>> chunkListToSignatures;   // Requires concurrent Set
    private final ConcurrentMap<ByteString, Set<ChunkServer.StorageHostChunkList>> signatureToChunkList;   // Requires concurrent Set
    private final ConcurrentMap<ByteString, List<ChunkListReference>> signatureToChunkListReferenceList;
    private final Supplier<ChunkDecrypter> decrypters;

    StoreManager(
            Store<ChunkServer.StorageHostChunkList> store,
            ConcurrentMap<ChunkServer.StorageHostChunkList, Set<ByteString>> chunkListToSignatures,
            ConcurrentMap<ByteString, Set<ChunkServer.StorageHostChunkList>> signatureToChunkList,
            ConcurrentMap<ByteString, List<ChunkListReference>> signatureToChunkListReferenceList,
            Supplier<ChunkDecrypter> decrypters) {

        this.store = Objects.requireNonNull(store);
        this.chunkListToSignatures = Objects.requireNonNull(chunkListToSignatures);
        this.signatureToChunkList = Objects.requireNonNull(signatureToChunkList);
        this.signatureToChunkListReferenceList = Objects.requireNonNull(signatureToChunkListReferenceList);
        this.decrypters = Objects.requireNonNull(decrypters);
    }

    public Map<ByteString, DataWriter> put(ChunkServer.StorageHostChunkList chunkList, byte[] chunkData)
            throws BadDataException {

        logger.trace("<< put() < uri: {} length: {}", chunkList.getHostInfo().getUri(), chunkData.length);

        List<byte[]> chunks = decrypters.get().decrypt(chunkList, chunkData);

        if (!store.put(chunkList, chunks)) {
            logger.warn("-- put() > overwritten store container: {}", chunkList.getHostInfo().getUri());
        }

        Map<ByteString, DataWriter> writers = process(chunkList);
        if (!writers.isEmpty()) {
            clear(writers.keySet());
        }

        logger.trace(">> put() > writers: {}", writers);
        return writers;
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

        return new Writer(writers);
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

    static class ChunkListReference {

        private final ChunkServer.StorageHostChunkList chunkList;
        private final int index;

        ChunkListReference(ChunkServer.StorageHostChunkList chunkList, int index) {
            this.chunkList = chunkList;
            this.index = index;
        }

        ChunkServer.StorageHostChunkList chunkList() {
            return chunkList;
        }

        int index() {
            return index;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 61 * hash + Objects.hashCode(this.chunkList);
            hash = 61 * hash + this.index;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ChunkListReference other = (ChunkListReference) obj;
            if (!Objects.equals(this.chunkList, other.chunkList)) {
                return false;
            }
            return this.index == other.index;
        }

        @Override
        public String toString() {
            return "ChunkListReference{" + "chunkList=" + chunkList + ", index=" + index + '}';
        }
    }

    public static class Writer implements DataWriter {

        private List<DataWriter> writers;

        Writer(List<DataWriter> writers) {
            this.writers = writers;
        }

        @Override
        public Long apply(OutputStream outputStream) throws IOException {
            if (data == null) {
                throw new IllegalStateException("Closed");
            }

            long total = 0;
            for (DataWriter writer : writers) {
                total += writer.apply(outputStream);
            }
            return total;
        }

        @Override
        public void close() throws IOException {
            for (DataWriter writer : writers) {
                writer.close();
            }
            writers = null;
        }
    }
}
