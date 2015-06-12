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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Memory based ChunkListStore.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class MemoryStore implements ChunkListStore {

    public static MemoryStore.Builder builder() {
        return new Builder();
    }

    private final List<List<byte[]>> containers;

    MemoryStore(List<List<byte[]>> containers) {
        this.containers = Objects.requireNonNull(containers);
    }

    @Override
    public long write(ChunkServer.ChunkReference chunkReference, OutputStream output) throws IOException {
        if (chunkReference == null) {
            return -1;
        }

        int containerIndex = (int) chunkReference.getContainerIndex();
        int chunkIndex = (int) chunkReference.getChunkIndex();

        byte[] chunk = (containerIndex < 0 || containerIndex >= containers.size())
                ? null
                : (chunkIndex < 0 || chunkIndex >= containers.get(containerIndex).size())
                        ? null
                        : containers.get(containerIndex).get(chunkIndex);

        if (chunk == null) {
            return -1;
        }

        output.write(chunk);
        return chunk.length;
    }

    @Override
    public long size() {
        return containers.size();
    }

    public static class Builder implements ChunkListStoreBuilder {

        List<List<byte[]>> containers = new ArrayList<>();

        @Override
        public Builder add(List<byte[]> container) {
            Objects.requireNonNull(container);

            List<byte[]> copy = container.stream()
                    .map(bytes -> Arrays.copyOf(bytes, bytes.length))
                    .collect(Collectors.toList());

            containers.add(copy);
            return this;
        }

        @Override
        public ChunkListStore build() {
            return new MemoryStore(containers);
        }
    }
}
