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
package com.github.horrorho.liquiddonkey.cloud.aold;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Combines Map entries into batches.
 *
 * {@link java.util.Map.Entry} elements are removed from the specified {@link java.util.concurrent.ConcurrentMap} and
 * grouped into batches with respect to the specified minimum size threshold. As the map is depleted the trailing
 * elements are batched regardless.
 *
 * @author Ahseya
 *
 * @param <K> the Map key type
 * @param <V> the Map value type
 */
@NotThreadSafe
public final class BundlerOld<K, V> implements Iterator<Map<K, V>> {

    /**
     * Returns a new Bundler instance.
     *
     * @param <K> the Map key type
     * @param <V> the Map value type
     * @param map the map supplying the elements, not null
     * @param size the function to determine the size of entry values, not null
     * @param filter the Map value filter, not null
     * @param batchSizeBytes the minimum batch size threshold
     * @return a new Bundler instance
     * @throws NullPointerException if map, size or filter arguments are null
     */
    public static <K, V> BundlerOld<K, V> newInstance(
            ConcurrentMap<K, V> map,
            Function<V, Long> size,
            Predicate<V> filter,
            long batchSizeBytes) {
        return new BundlerOld<>(map, size, filter, batchSizeBytes);
    }

    private static final Logger logger = LoggerFactory.getLogger(BundlerOld.class);

    private final ConcurrentMap<K, V> map;
    private final Function<V, Long> size;
    private final Predicate<V> filter;
    private final long batchSizeBytes;

    BundlerOld(ConcurrentMap<K, V> map, Function<V, Long> size, Predicate<V> filter, long batchSizeBytes) {
        this.map = Objects.requireNonNull(map);
        this.size = Objects.requireNonNull(size);
        this.filter = Objects.requireNonNull(filter);
        this.batchSizeBytes = batchSizeBytes;
    }

    @Override
    public boolean hasNext() {
        return !map.isEmpty();
    }

    @Override
    public Map<K, V> next() {
        logger.trace("<< next()");
        Map<K, V> files = new HashMap<>();
        long total = 0;

        loop:
        while (!map.isEmpty()) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                if (map.remove(entry.getKey()) == null) {
                    continue;
                }

                if (!filter.test(entry.getValue())) {
                    continue;
                }

                files.put(entry.getKey(), entry.getValue());

                total += size.apply(entry.getValue());
                if (total > batchSizeBytes) {
                    break loop;
                }
            }
        }

        logger.trace(">> next() > entries: {} size: {}", files.size(), total);
        return files;
    }
}
