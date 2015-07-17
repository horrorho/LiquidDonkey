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
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChunkListReference.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
final class ChunkListReference {

    public static ConcurrentMap<ByteString, List<ChunkListReference>>
            toMap(ChunkServer.FileChecksumStorageHostChunkLists fileGroup) {

        List<ChunkServer.StorageHostChunkList> chunkLists = fileGroup.getStorageHostChunkListList();

        Function<ChunkServer.ChunkReference, ChunkListReference> list = reference
                -> ChunkListReference.from(
                        chunkLists.get((int) reference.getContainerIndex()),
                        (int) reference.getChunkIndex());

        return fileGroup.getFileChecksumChunkReferencesList().stream()
                .collect(Collectors.toConcurrentMap(
                                ChunkServer.FileChecksumChunkReferences::getFileChecksum,
                                r -> r.getChunkReferencesList().stream().map(list).collect(Collectors.toList()),
                                (a, b) -> {
                                    if (!Objects.equals(a, b)) {
                                        // Unlikely to be significant.
                                        logger.warn("-- toMap() > signature collision: {} {}", a, b);
                                    }
                                    return a;
                                }));
    }

    public static ChunkListReference from(ChunkServer.StorageHostChunkList chunkList, int index) {
        return new ChunkListReference(chunkList, index);
    }

    private static final Logger logger = LoggerFactory.getLogger(ChunkListReference.class);

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
