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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final Map<Long, List<byte[]>> emptyGroup = Collections.unmodifiableMap(new HashMap<>());

    private final ConcurrentMap<Long, ConcurrentMap<Long, List<byte[]>>> groupToContainerToList;

    MemoryStore(ConcurrentMap<Long, ConcurrentMap<Long, List<byte[]>>> groupToContainerToList) {
        this.groupToContainerToList = Objects.requireNonNull(groupToContainerToList);
    }

    @Override
    public boolean contains(long groupIndex, List<ChunkServer.ChunkReference> chunkReferences) {
        return chunkReferences.stream().allMatch(reference -> contains(groupIndex, reference));
    }

    boolean contains(long groupIndex, ChunkServer.ChunkReference chunkReference) {
        if (!groupToContainerToList.containsKey(groupIndex)) {
            return false;
        }
        
        long containerIndex = chunkReference.getContainerIndex();
        long chunkIndex = chunkReference.getChunkIndex();

        ConcurrentMap<Long, List<byte[]>> container = groupToContainerToList.get(groupIndex);
        if (!container.containsKey(containerIndex)) {
            return false;
        }

        return container.get(containerIndex).size() > chunkIndex;
    }

    @Override
    public boolean put(long groupIndex, long containerIndex, List<byte[]> chunkData) {
        List<byte[]> copy = chunkData.stream()
                .map(data -> Arrays.copyOf(data, data.length))
                .collect(Collectors.toList());

        return groupToContainerToList
                .computeIfAbsent(groupIndex, key -> new ConcurrentHashMap<>())
                .put(containerIndex, copy) == null;
    }

    @Override
    public boolean remove(long groupIndex, long containerIndex) {
        if (!groupToContainerToList.containsKey(groupIndex)) {
            return false;
        }
        return groupToContainerToList.get(groupIndex).remove(containerIndex) != null;
    }

    @Override
    public int size(long groupIndex, long containerIndex) {
        if (!groupToContainerToList.containsKey(groupIndex)) {
            return -1;
        }
        ConcurrentMap<Long, List<byte[]>> containers = groupToContainerToList.get(groupIndex);
        return containers.containsKey(containerIndex)
                ? containers.get(containerIndex).size()
                : -1;
    }

    @Override
    public IOFunction<OutputStream, Long> writer(long groupIndex, List<ChunkServer.ChunkReference> chunkReferences) {
        if (!contains(groupIndex, chunkReferences)) {
            throw new NullPointerException("Missing chunk references");
        }
        ConcurrentMap<Long, List<byte[]>> containers = groupToContainerToList.get(groupIndex);
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
