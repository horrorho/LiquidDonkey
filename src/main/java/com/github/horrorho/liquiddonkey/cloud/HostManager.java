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
import static com.github.horrorho.liquiddonkey.settings.Markers.HOST;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        logger.debug(HOST, "-- from() > fileGroup: {}", fileGroup);

        List<ChunkServer.StorageHostChunkList> list = fileGroup.getStorageHostChunkListList();

        ConcurrentMap<Long, ChunkServer.StorageHostChunkList> containerToStorageHost = new ConcurrentHashMap<>();
        Set<Long> containers = Collections.<Long>newSetFromMap(new ConcurrentHashMap<>());
        for (int index = 0; index < list.size(); index++) {
            containerToStorageHost.put(Long.valueOf(index), list.get(index));
            containers.add((long) index);
        }

        logger.debug(HOST, "-- from() > containerToStorageHost: {}", containerToStorageHost);
        logger.debug(HOST, "-- from() > containers: {}", containers);

        HostManager instance = new HostManager(
                containerToStorageHost,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                containers,
                Collections.<Long>newSetFromMap(new ConcurrentHashMap<>()));

        logger.trace(">> from()");
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(HostManager.class);

    private final ConcurrentMap<Long, ChunkServer.StorageHostChunkList> containerToStorageHost;
    private final ConcurrentMap<Long, Integer> containerToRetryCount;
    private final ConcurrentMap<Long, List<Throwable>> containerToExceptions;   // Collections#synchronizedList
    private final Set<Long> containers;                                         // Concurrent Set
    private final Set<Long> success;                                            // Concurrent Set 

    HostManager(
            ConcurrentMap<Long, ChunkServer.StorageHostChunkList> containerToStorageHost,
            ConcurrentMap<Long, Integer> containerToRetryCount,
            ConcurrentMap<Long, List<Throwable>> containerToExceptions,
            Set<Long> containers,
            Set<Long> success) {

        this.containerToStorageHost = Objects.requireNonNull(containerToStorageHost);
        this.containerToRetryCount = containerToRetryCount;
        this.containerToExceptions = containerToExceptions;
        this.containers = containers;
        this.success = success;
    }

    public ChunkServer.StorageHostChunkList storageHostChunkList(long container) {
        if (success.contains(container)) {
            logger.warn("-- storageHostChunkList() > bad state, duplicated container: {}", container);
        }
        return containerToStorageHost.get(container);
    }

    public Iterator<Long> iterator() {
        logger.trace("<< iterator()");
        logger.debug("-- iterator() > containers size: {}", containers.size());
        logger.debug(HOST, "-- iterator() > containers: {}", containers);
        Iterator<Long> iterator = containers.iterator();

        logger.trace(">> iterator()");
        return iterator;
    }

    public void failed(Long container, Throwable th) {
        logger.trace("<< failed() < container: {} throwable: {}", container, th.getMessage());

        // TODO handle retry policy
        // IllegalStateException also
        containerToExceptions
                .computeIfAbsent(container, c -> Collections.synchronizedList(new ArrayList<>()))
                .add(th);

        logger.trace(">> failed");
    }

    public void success(Long container) {
        logger.trace("<< success() < container: {}", container);

        if (!success.add(container)) {
            logger.warn("-- success() -> bad state, duplicated success: {}", container);
        }
        if (!containers.remove(container)) {
            logger.warn("-- success() -> bad state, duplicated container: {}", container);
        }

        logger.trace(">> success()");
    }

    public boolean isEmpty() {
        return containers.isEmpty();
    }

    // TODO retrieval methods
}
