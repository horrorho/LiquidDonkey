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
public interface ChunkListStore {

    /**
     * Writes the referenced chunk's data to the specified output stream.
     *
     * @param chunkReference chunk reference
     * @param output output stream
     * @return bytes written or -1 if no such chunk exists
     * @throws IOException
     */
    long write(ChunkServer.ChunkReference chunkReference, OutputStream output) throws IOException;

    /**
     * Returns the number of containers present in this ChunkListStore.
     *
     * @return the number of containers present in this storage
     */
    long size();

    /**
     * Writes all the referenced chunk data to the specified output stream. Continues until all the chunk data is
     * written or a null block is encountered.
     *
     * @param chunkReferences chunk references
     * @param output output stream
     * @return bytes written or -1 if a missing chunk was encountered.
     * @throws IOException
     */
    default long write(List<ChunkServer.ChunkReference> chunkReferences, OutputStream output) throws IOException {
        long total = 0;
        for (ChunkServer.ChunkReference reference : chunkReferences) {
            long written = write(reference, output);
            if (written == -1) {
                return -1;
            }
            total += written;
        }
        return total;
    }
}
