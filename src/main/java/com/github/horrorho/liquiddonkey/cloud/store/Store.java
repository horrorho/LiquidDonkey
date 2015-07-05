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
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Container store.
 *
 * @author Ahseya
 */
public interface Store {

    /**
     * Returns whether the referenced chunk is present in this Store.
     *
     * @param containerIndex
     * @param chunkIndex
     * @return true if present, false if not present
     */
    boolean contains(long containerIndex, long chunkIndex);

    /**
     * Removes the referenced container from the Store.
     *
     * @param containerIndex
     * @throws IllegalStateException if the referenced container does not exist.
     */
    void remove(long containerIndex);

    /**
     * Puts the specified data into the store at the the chunk reference location.
     *
     * @param containerIndex
     * @param chunkIndex
     * @param chunkData, not null
     * @throws IllegalStateException if the referenced location is not empty
     */
    void put(long containerIndex, long chunkIndex, byte[] chunkData);

    /**
     * Returns an IOFUnction that writes the referenced chunk's data to the specified output stream.
     * <p>
     * Subsequent modifications to the Store will not alter its output.
     *
     * @param containerIndex
     * @param chunkIndex
     * @return bytes written
     * @throws IllegalStateException if the chunk is not present in this Store
     */
    IOFunction<OutputStream, Long> writer(long containerIndex, long chunkIndex);

    /**
     * Puts the specified data into the store at the the chunk reference location.
     *
     * @param chunkReference, not null
     * @param chunkData, not null
     * @throws IllegalStateException if the referenced location is not empty
     */
    default void put(ChunkServer.ChunkReference chunkReference, byte[] chunkData) {
        long containerIndex = chunkReference.getContainerIndex();
        long chunkIndex = chunkReference.getChunkIndex();
        put(containerIndex, chunkIndex, chunkData);
    }

    /**
     * Returns whether the referenced chunk is present in this store.
     *
     * @param chunkReference, not null
     * @return true if present, false if not present
     */
    default boolean contains(ChunkServer.ChunkReference chunkReference) {
        long containerIndex = chunkReference.getContainerIndex();
        long chunkIndex = chunkReference.getContainerIndex();
        return contains(containerIndex, chunkIndex);
    }

    /**
     * Returns whether all the referenced chunks are present in this Store.
     *
     * @param chunkReferences chunk references, not null
     * @return true if all present, false if not all present
     */
    default boolean contains(List<ChunkServer.ChunkReference> chunkReferences) {
        return chunkReferences.stream().allMatch(this::contains);
    }

    /**
     * Returns an IOFunction that writes the referenced chunk's data to the specified output stream.
     * <p>
     * Subsequent modifications to the Store will not alter its output.
     *
     * @param chunkReference chunk reference, not null
     * @return bytes written
     * @throws IllegalStateException if the chunk is not present in this Store
     */
    default IOFunction<OutputStream, Long> writer(ChunkServer.ChunkReference chunkReference) {
        long containerIndex = chunkReference.getContainerIndex();
        long chunkIndex = chunkReference.getContainerIndex();
        return writer(containerIndex, chunkIndex);
    }

    /**
     * Returns an IOFunction that in order writes the referenced chunk data to the specified output stream.
     * <p>
     * Subsequent modifications to the Store will not alter its output.
     *
     * @param chunkReferences chunk references, not null
     * @return bytes written
     * @throws IllegalStateException if any chunks are missing from this Store
     */
    default IOFunction<OutputStream, Long> writer(List<ChunkServer.ChunkReference> chunkReferences) {
        List<IOFunction<OutputStream, Long>> writers = chunkReferences.stream()
                .map(this::writer)
                .collect(Collectors.toList());

        return outputStream -> {
            long total = 0;
            for (IOFunction<OutputStream, Long> writer : writers) {
                total += writer.apply(outputStream);
            }
            return total;
        };
    }
}
