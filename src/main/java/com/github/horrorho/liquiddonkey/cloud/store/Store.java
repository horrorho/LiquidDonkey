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

/**
 * Container store.
 *
 * @author Ahseya
 */
public interface Store {

    /**
     * Returns the size of the specified container.
     *
     * @param containerIndex
     * @return size of the container, or -1 if no such container exists
     */
    int size(long containerIndex);

    /**
     * Removes the specified container from the Store.
     *
     * @param containerIndex
     * @return true if the Store contained the specified element
     */
    boolean remove(long containerIndex);

    /**
     * Creates the specified container and copies over the specified data.
     *
     * @param containerIndex
     * @param chunkData, not null
     * @return true if the Store did not already contain the specified container
     */
    boolean put(long containerIndex, List<byte[]> chunkData);

    /**
     * Returns an IOFunction that writes the referenced containers data to the specified output stream. Subsequent
     * modifications to the Store will not alter its output.
     *
     * @param chunkReferences chunk references, not null
     * @return immutable writer, not null
     * @throws NullPointerException if the specified container is not present in the Store
     */
    IOFunction<OutputStream, Long> writer(List<ChunkServer.ChunkReference> chunkReferences);

    /**
     * Returns whether the referenced chunks are present in this Store.
     *
     * @param chunkReferences chunk references, not null
     * @return true if all present
     */
    boolean contains(List<ChunkServer.ChunkReference> chunkReferences);

    /**
     * Returns whether the specified container is present in the Store.
     *
     * @param containerIndex
     * @return true if present
     */
    default boolean contains(long containerIndex) {
        return size(containerIndex) != -1;
    }
}
