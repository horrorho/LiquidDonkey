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
package com.github.horrorho.liquiddonkey.cloud;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import static com.github.horrorho.liquiddonkey.settings.Markers.CLOUD;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
@ThreadSafe
public class HostManager {

    public static HostManager from(ChunkServer.FileChecksumStorageHostChunkLists fileGroup) {
        logger.trace("<< from()");
        logger.debug(CLOUD, "-- from() > fileGroup: {}", fileGroup);

        List<ChunkServer.StorageHostChunkList> list = fileGroup.getStorageHostChunkListList();

        ConcurrentMap<Long, ChunkServer.StorageHostChunkList> containerToStorageHost = new ConcurrentHashMap<>();
        for (int index = 0; index < list.size(); index++) {
            containerToStorageHost.put(Long.valueOf(index), list.get(index));
        }

        ArrayBlockingQueue containers = new ArrayBlockingQueue(list.size(), true, list);

        HostManager instance = new HostManager(
                containers,
                containerToStorageHost,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                Collections.<Long>newSetFromMap(new ConcurrentHashMap<>()),
                Collections.<Long>newSetFromMap(new ConcurrentHashMap<>()));

        logger.trace(">> from()");
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(HostManager.class);

    private final ArrayBlockingQueue<Long> containers;
    private final ConcurrentMap<Long, ChunkServer.StorageHostChunkList> containerToStorageHost;
    private final ConcurrentMap<Long, Integer> containerToRetryCount;
    private final ConcurrentMap<Long, List<Exception>> containerToExceptions;   // Collections#synchronizedList
    private final Set<Long> success;                                            // Concurrent Set
    private final Set<Long> cancelled;

    HostManager(
            ArrayBlockingQueue<Long> containers,
            ConcurrentMap<Long, ChunkServer.StorageHostChunkList> containerToStorageHost,
            ConcurrentMap<Long, Integer> containerToRetryCount,
            ConcurrentMap<Long, List<Exception>> containerToExceptions,
            Set<Long> success,
            Set<Long> cancelled) {

        if (containers.size() == 0) {
            throw new IllegalArgumentException("Empty blocking queue");
        }

        this.containers = containers;
        this.containerToStorageHost = containerToStorageHost;
        this.containerToRetryCount = containerToRetryCount;
        this.containerToExceptions = containerToExceptions;
        this.success = success;
        this.cancelled = cancelled;
    }

    public ChunkServer.StorageHostChunkList storageHostChunkList(long container) {
        if (success.contains(container)) {
            throw new IllegalStateException("Bad request: " + container);
        }
        return containerToStorageHost.get(container);
    }

    public Long next() throws InterruptedException {
        Long next = containers.take();
        if (next == null) {
            // Clear all waiting threads with propagating nulls
            if (!containers.contains(null)) {
                containers.add(null);
            }
        }
        return next;
    }

    public void failed(Exception ex, Long container) {
        // TODO handle retry policy
        containerToExceptions
                .computeIfAbsent(container, c -> Collections.synchronizedList(new ArrayList<>()))
                .add(ex);
    }

    public void success(Long container) {
        if (!success.add(container)) {

        }
    }

    public synchronized void drain() {
        logger.trace("<< drain()");
        containers.drainTo(cancelled);
        if (!containers.contains(null)) {
            containers.add(null);
        }
        logger.trace(">> drain()");
    }

    // TODO retrieval methods
}
