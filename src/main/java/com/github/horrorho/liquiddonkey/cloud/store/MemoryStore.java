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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
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

    public static MemoryStore create() {
        logger.trace("<< newInstance()");
        
        MemoryStore instance = new MemoryStore();
        
        logger.trace(">> newInstance()");
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(MemoryStore.class);

    private final ConcurrentMap<K, byte[]> containers;
    private final AtomicLong size;

    MemoryStore(ConcurrentMap<K, byte[]> containers, AtomicLong size) {
        this.containers = Objects.requireNonNull(containers);
        this.size = Objects.requireNonNull(size);
    }

    MemoryStore() {
        this(new ConcurrentHashMap<>(), new AtomicLong(0));
    }

    @Override
    public List<K> keys() {
        return new ArrayList<>(containers.keySet());
    }

    @Override
    public boolean put(K key, byte[] data) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(data);

        byte[] oldData = containers.put(key, Arrays.copyOf(data, data.length));
        long in = data.length;
        long out = oldData == null ? 0 : oldData.length;

        long delta = in - out;
        long instant = size.addAndGet(delta);

        logger.debug("-- put() > in: {} out: {} size: {}", in, out, instant);
        return oldData == null;
    }

    @Override
    public boolean remove(K key) {
        Objects.requireNonNull(key);
        
        byte[] oldData = containers.remove(key);
        long out = oldData == null ? 0 : oldData.length;
        long instant = size.addAndGet(-out);

        logger.debug("-- remove() > out: {} size: {}", out, instant);
        return oldData != null;
    }

    @Override
    public int size(K key) {
        return containers.get(key).length;
    }

    @Override
    public long size() {
        return size.get();
    }

    @Override
    public DataWriter writer(K key) {
        return new Writer(key);
    }

    @Override
    public boolean contains(K key) {
        Objects.requireNonNull(key);        
        return containers.containsKey(key);
    }

    public final class Writer implements DataWriter {

        private byte[] data;

        Writer(K key) {
            if (!contains(key)) {
                throw new NullPointerException("No such container: " + key);
            }
            data = containers.get(key);
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
        public void close() {
            data = null;
        }
    }
}
