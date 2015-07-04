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
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memory based Store.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class MemoryStore implements Store {

    private static final Logger logger = LoggerFactory.getLogger(MemoryStore.class);

    private final ConcurrentMap<Long, ConcurrentMap<Long, byte[]>> containers;

    MemoryStore(ConcurrentMap<Long, ConcurrentMap<Long, byte[]>> containers) {
        this.containers = Objects.requireNonNull(containers);
    }

    @Override
    public boolean contains(ChunkServer.ChunkReference chunkReference) {
        long container = chunkReference.getContainerIndex();
        long index = chunkReference.getContainerIndex();

        return containers.containsKey(container)
                ? containers.get(container).containsKey(index)
                : false;
    }

    @Override
    public void put(ChunkServer.ChunkReference chunkReference, byte[] chunkData) {
        long containerIndex = chunkReference.getContainerIndex();
        long chunkIndex = chunkReference.getChunkIndex();

        ConcurrentMap<Long, byte[]> container
                = containers.computeIfAbsent(containerIndex, key -> new ConcurrentHashMap<>());

        if (container.containsKey(chunkIndex)) {
            throw new IllegalStateException("Put to an non-empty chunklocation");
        }

        if (chunkData == null) {
            logger.warn("-- put() > null chunkData. containerIndex: {}, chunkIndex: {}", containerIndex, chunkIndex);
        } else {
            byte[] copy = Arrays.copyOf(chunkData, chunkData.length);
            container.put(chunkIndex, copy);
        }
    }

    @Override
    public long write(ChunkServer.ChunkReference chunkReference, OutputStream output) throws IOException {
        long container = chunkReference.getContainerIndex();
        long index = chunkReference.getContainerIndex();

        if (!contains(chunkReference)) {
            throw new IllegalStateException("Missing chunk");
        }

        byte[] chunk = containers.get(container).get(index);
        output.write(chunk);
        return chunk.length;
    }
}
