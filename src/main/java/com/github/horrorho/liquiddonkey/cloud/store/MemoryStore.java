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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memory based Store.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class MemoryStore implements Store {

    public static MemoryStore newInstance() {
        logger.trace("<< newInstance()");
        MemoryStore instance = new MemoryStore(new ConcurrentHashMap<>());
        logger.trace(">> newInstance()");
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(MemoryStore.class);

    private final ConcurrentMap<Long, List<byte[]>> containers;

    MemoryStore(ConcurrentMap<Long, List<byte[]>> containers) {
        this.containers = Objects.requireNonNull(containers);
    }

    @Override
    public boolean contains(List<ChunkServer.ChunkReference> chunkReferences) {
        return chunkReferences.stream().allMatch(this::contains);
    }

    boolean contains(ChunkServer.ChunkReference chunkReference) {
        long containerIndex = chunkReference.getContainerIndex();
        long chunkIndex = chunkReference.getChunkIndex();

        return containers.containsKey(containerIndex)
                ? containers.get(containerIndex).size() > chunkIndex
                : false;
    }

    @Override
    public boolean put(long containerIndex, List<byte[]> chunkData) {
        List<byte[]> copy = chunkData.stream()
                .map(data -> Arrays.copyOf(data, data.length))
                .collect(Collectors.toList());

        return containers.put(containerIndex, copy) == null;
    }

    @Override
    public boolean remove(long containerIndex) {
        return containers.remove(containerIndex) != null;
    }

    @Override
    public int size(long containerIndex) {
        return containers.containsKey(containerIndex)
                ? containers.get(containerIndex).size()
                : -1;
    }

    @Override
    public IOFunction<OutputStream, Long> writer(List<ChunkServer.ChunkReference> chunkReferences) {
        if (!contains(chunkReferences)) {
            throw new NullPointerException("Missing chunk references");
        }

        List<byte[]> chunks = chunkReferences.stream()
                .map(reference -> containers.get(reference.getContainerIndex()).get((int) reference.getChunkIndex()))
                .collect(Collectors.toList());

        return outputStream -> {
            long total = 0;
            for (byte[] chunk : chunks) {
                outputStream.write(chunk);
                total += chunk.length;
            }
            return total;
        };
    }
}
