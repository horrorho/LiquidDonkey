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
import java.util.Map;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
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

    public static MemoryStore.Builder builder() {
        return new Builder();
    }

    private final Map<Long, Map<Long, byte[]>> containers;

    MemoryStore(Map<Long, Map<Long, byte[]>> containers) {
        this.containers = Objects.requireNonNull(containers);
    }

    @Override
    public long write(ChunkServer.ChunkReference chunkReference, OutputStream output)
            throws BadDataException, IOException {

        long container = chunkReference.getContainerIndex();
        long index = chunkReference.getContainerIndex();

        if (!contains(chunkReference)) {
            throw new BadDataException("Missing chunk");
        }

        byte[] chunk = containers.get(container).get(index);
        output.write(chunk);
        return chunk.length;
    }

    @Override
    public boolean contains(ChunkServer.ChunkReference chunkReference) {
        long container = chunkReference.getContainerIndex();
        long index = chunkReference.getContainerIndex();

        return containers.containsKey(container)
                ? containers.get(container).containsKey(index)
                : false;
    }

    @NotThreadSafe
    public static class Builder implements StoreBuilder {

        private static final Logger logger = LoggerFactory.getLogger(Builder.class);

        private final Map<Long, Map<Long, byte[]>> containers = new HashMap<>();

        @Override
        public Store build() {
            return new MemoryStore(containers);
        }

        @Override
        public StoreBuilder add(long containerIndex, long chunkIndex, byte[] chunkData) {
            if (chunkData == null) {
                logger.warn("-- add() > null chunkData. containerIndex: {}, chunkIndex: {}",
                        containerIndex, chunkIndex);
            } else {
                byte[] copy = Arrays.copyOf(chunkData, chunkData.length);
                containers.computeIfAbsent(containerIndex, key -> new HashMap<>()).put(chunkIndex, copy);
            }
            return this;
        }
    }
}
