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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
@ThreadSafe
public class HostManager {

    public static HostManager from(ChunkServer.FileGroups fileGroups) {
        logger.trace("<< from()");
        logger.debug(HOST, "-- from() > fileGroup: {}", fileGroups);

        ConcurrentMap<Long, ConcurrentMap<Long, ChunkServer.StorageHostChunkList>> groupToContainerToStorageHost
                = new ConcurrentHashMap<>();
        ConcurrentMap<Long, Set<Long>> groupToContainerSet = new ConcurrentHashMap<>();

        List<ChunkServer.FileChecksumStorageHostChunkLists> fileGroupsList = fileGroups.getFileGroupsList();

        for (int groupIndex = 0; groupIndex < fileGroupsList.size(); groupIndex++) {
            List<ChunkServer.StorageHostChunkList> list = fileGroupsList.get(groupIndex).getStorageHostChunkListList();

            for (int containerIndex = 0; containerIndex < list.size(); containerIndex++) {
                groupToContainerToStorageHost
                        .computeIfAbsent(Long.valueOf(groupIndex), key -> new ConcurrentHashMap<>())
                        .put(Long.valueOf(containerIndex), list.get(containerIndex));

                groupToContainerSet
                        .computeIfAbsent(Long.valueOf(groupIndex), key -> newConcurrentSet())
                        .add(Long.valueOf(containerIndex));
            }
        }

        logger.debug(HOST, "-- from() > containerToStorageHost: {}", groupToContainerToStorageHost);
        logger.debug(HOST, "-- from() > containers: {}", groupToContainerSet);

        HostManager instance = new HostManager(
                groupToContainerToStorageHost,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                groupToContainerSet,
                new ConcurrentHashMap<>());

        logger.trace(">> from()");
        return instance;
    }

    static <T> Set<T> newConcurrentSet() {
        return Collections.<T>newSetFromMap(new ConcurrentHashMap<>());
    }

    static <T> List<T> newSyncList() {
        return Collections.synchronizedList(new ArrayList<>());
    }

    private static final Logger logger = LoggerFactory.getLogger(HostManager.class);

    // Lists are Collections#synchronizedList
    // Sets are wrapped ConcurrentMap
    private final ConcurrentMap<Long, ConcurrentMap<Long, ChunkServer.StorageHostChunkList>> groupToContainerToStorageHost;
    private final ConcurrentMap<Long, ConcurrentMap<Long, Integer>> groupToContainerToRetryCount;
    private final ConcurrentMap<Long, ConcurrentMap<Long, List<Exception>>> groupToContainerToExceptions;
    private final ConcurrentMap<Long, Set<Long>> groupToContainerSet;
    private final ConcurrentMap<Long, Set<Long>> groupToContainerSetSuccess;

    private final List<Long> emptyList = Collections.unmodifiableList(new ArrayList<>());
    private final Set<Long> emptySet = Collections.unmodifiableSet(new HashSet<>());

    public HostManager(
            ConcurrentMap<Long, ConcurrentMap<Long, ChunkServer.StorageHostChunkList>> groupToContainerToStorageHost,
            ConcurrentMap<Long, ConcurrentMap<Long, Integer>> groupToContainerToRetryCount,
            ConcurrentMap<Long, ConcurrentMap<Long, List<Exception>>> groupToContainerToExceptions,
            ConcurrentMap<Long, Set<Long>> groupToContainerSet,
            ConcurrentMap<Long, Set<Long>> groupToContainerSetSuccess) {

        this.groupToContainerToStorageHost = Objects.requireNonNull(groupToContainerToStorageHost);
        this.groupToContainerToRetryCount = Objects.requireNonNull(groupToContainerToRetryCount);
        this.groupToContainerToExceptions = Objects.requireNonNull(groupToContainerToExceptions);
        this.groupToContainerSet = Objects.requireNonNull(groupToContainerSet);
        this.groupToContainerSetSuccess = Objects.requireNonNull(groupToContainerSetSuccess);
    }

    public ChunkServer.StorageHostChunkList storageHostChunkList(long groupIndex, long containerIndex) {
        // TODO remove completed
        if (groupToContainerSetSuccess.getOrDefault(groupIndex, emptySet).contains(containerIndex)) {
            logger.warn("-- storageHostChunkList() > duplicated group: {} container: {}",
                    groupIndex, containerIndex);
        }

        return groupToContainerToStorageHost.get(groupIndex).get(containerIndex);
    }

    public Map<Long, Iterator<Long>> iterator() {
        logger.trace("<< iterator()");

        groupToContainerSet.entrySet().stream().forEach(entry
                -> logger.debug("-- iterator() > group: {} containers: {}", entry.getKey(), entry.getValue()));

        Map<Long, Iterator<Long>> iterator = groupToContainerSet.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().iterator()));

        logger.trace(">> iterator()");
        return iterator;
    }

    public void failed(Long groupIndex, Long containerIndex, Exception ex) {
        logger.trace("<< failed() < group: {} container: {} exception: {}",
                groupIndex, containerIndex, ex.getMessage());

        // TODO handle retry policy
        // IllegalStateException also
        groupToContainerToExceptions
                .computeIfAbsent(groupIndex, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(containerIndex, key -> newSyncList())
                .add(ex);

        logger.trace(">> failed");
    }

    public void success(Long groupIndex, Long containerIndex) {
        logger.trace("<< success() < container: {}", groupIndex, containerIndex);

        if (!groupToContainerSetSuccess
                .computeIfAbsent(groupIndex, key -> newConcurrentSet())
                .add(containerIndex)) {
            logger.warn("-- success() -> duplicated success, group: {} container: {}", groupIndex, containerIndex);
        }

        if (groupToContainerSet.containsKey(groupIndex)) {
            if (!groupToContainerSet.get(groupIndex).remove(containerIndex)) {
                logger.warn("-- success() -> duplicated remove, group: {} container: {}", groupIndex, containerIndex);
            } else if (groupToContainerSet.get(groupIndex).isEmpty()) {
                groupToContainerSet.remove(groupIndex);

            }
        } else {
            logger.warn("-- success() > duplicated remove, group: {} container: {}", groupIndex, containerIndex);
        }

        logger.trace(">> success()");
    }

    public boolean isEmpty() {
        return groupToContainerSet.isEmpty();
    }

    // TODO retrieval methods
}
