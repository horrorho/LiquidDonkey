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

import java.io.IOException;
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
 * @param <K> key type
 */
@ThreadSafe
public final class MemoryStore<K> implements Store<K> {

    public static MemoryStore newInstance() {
        logger.trace("<< newInstance()");
        MemoryStore instance = new MemoryStore(new ConcurrentHashMap<>());
        logger.trace(">> newInstance()");
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(MemoryStore.class);

    private final ConcurrentMap<K, List<byte[]>> containers;

    MemoryStore(ConcurrentMap<K, List<byte[]>> containers) {
        this.containers = Objects.requireNonNull(containers);
    }

    @Override
    public boolean put(K key, List<byte[]> chunkData
    ) {
        if (chunkData.contains(null)) {
            throw new NullPointerException("Null chunk data entry");
        }
        List<byte[]> copy = chunkData.stream()
                .map(data -> Arrays.copyOf(data, data.length))
                .collect(Collectors.toList());

        return containers.put(key, copy) == null;
    }

    @Override
    public boolean remove(K key
    ) {
        return containers.remove(key) != null;
    }

    @Override
    public int size(K key) {
        return containers.containsKey(key)
                ? containers.get(key).size()
                : -1;
    }

    @Override
    public DataWriter writer(K key, int index) {
        return new Writer(key, index);
    }

    public final class Writer implements DataWriter {

        private byte[] data;

        Writer(K key, int index) {
            if (!contains(key, index)) {
                throw new IllegalStateException("Missing item, key: " + key + " index: " + index);
            }
            data = containers.get(key).get(index);
        }

        @Override
        public Long apply(OutputStream outputStream) throws IOException {
            if (data == null) {
                throw new IllegalStateException("Closed");
            }

            outputStream.write(data);
            return (long) data.length;
        }

        @Override
        public void close() throws IOException {
            data = null;
        }
    }
}
