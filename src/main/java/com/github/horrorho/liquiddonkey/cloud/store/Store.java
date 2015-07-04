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
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Container store.
 *
 * @author Ahseya
 */
public interface Store {

    /**
     * Returns whether the referenced chunk is present in this store.
     *
     * @param chunkReference, not null
     * @return true if present, false if not present
     */
    boolean contains(ChunkServer.ChunkReference chunkReference);

    /**
     * Puts the specified data into the store at the the chunk reference location.
     *
     * @param chunkReference, not null
     * @param chunkData, not null
     * @throws IllegalStateException if the referenced location is not empty
     */
    void put(ChunkServer.ChunkReference chunkReference, byte[] chunkData);

    /**
     * Destroys the referenced container, removing all chunk data.
     *
     * @param containerIndex
     * @throws IllegalStateException if the referenced container does not exist.
     */
    void destroy(long containerIndex);

    /**
     * Writes the referenced chunk's data to the specified output stream.
     *
     * @param chunkReference chunk reference, not null
     * @param output output stream, not null
     * @return bytes written
     * @throws IOException
     * @throws IllegalStateException if the chunk is not present in this store
     */
    long write(ChunkServer.ChunkReference chunkReference, OutputStream output) throws IOException;

    /**
     * Returns whether all the referenced chunks are present in this store.
     *
     * @param chunkReferences chunk references, not null
     * @return true if all present, false if not all present
     */
    default boolean contains(List<ChunkServer.ChunkReference> chunkReferences) {
        return chunkReferences.stream().allMatch(this::contains);
    }

    /**
     * Writes all the referenced chunk data to the specified output stream.
     *
     * @param chunkReferences chunk references, not null
     * @param output output stream, not null
     * @return bytes written
     * @throws IOException
     * @throws IllegalStateException if all chunks are not present in this store
     */
    default long write(List<ChunkServer.ChunkReference> chunkReferences, OutputStream output) throws IOException {
        long total = 0;
        for (ChunkServer.ChunkReference reference : chunkReferences) {
            total += write(reference, output);
        }
        return total;
    }
}
