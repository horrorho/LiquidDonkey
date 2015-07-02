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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundler. Bundles map entries.
 * <p>
 *
 * Elements are removed from the specified signature map, filtered and then grouped into bundles with respect to the
 * specified minimum size threshold. As the map is depleted the trailing elements are bundled regardless.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class Bundler implements Iterator<Map<ByteString, Set<ICloud.MBSFile>>> {

    /**
     * Returns a new instance wrapped around the specified map.
     *
     * @param map, not null
     * @param filter, not null
     * @param batchSizeBytes
     * @return a new instance, not null
     */
    public static Bundler wrap(
            ConcurrentMap<ByteString, Set<ICloud.MBSFile>> map,
            Predicate<ICloud.MBSFile> filter,
            long batchSizeBytes) {

        return new Bundler(map, filter, batchSizeBytes);
    }

    private static final Logger logger = LoggerFactory.getLogger(Bundler.class);

    private final ConcurrentMap<ByteString, Set<ICloud.MBSFile>> map;
    private final Predicate<ICloud.MBSFile> filter;
    private final long batchSizeBytes;

    Bundler(
            ConcurrentMap<ByteString, Set<ICloud.MBSFile>> map,
            Predicate<ICloud.MBSFile> filter,
            long batchSizeBytes) {

        this.map = map;
        this.filter = filter;
        this.batchSizeBytes = batchSizeBytes;
    }

    @Override
    public boolean hasNext() {
        return !map.isEmpty();
    }

    @Override
    public Map<ByteString, Set<ICloud.MBSFile>> next() {
        logger.trace("<< next()");

        Map<ByteString, Set<ICloud.MBSFile>> files = new HashMap<>();
        long total = 0;

        loop:
        for (Map.Entry<ByteString, Set<ICloud.MBSFile>> entry : map.entrySet()) {
            if (total > batchSizeBytes) {
                break;
            }

            // If null, another thread has already acquired this map entry.
            // TODO optimization: if one copy of a signature set exists, to duplicate rather than download
            if (map.remove(entry.getKey()) == null || entry.getValue().stream().noneMatch(filter)) {
                continue;
            }

            files.put(entry.getKey(), entry.getValue());
            total += entry.getValue().stream().mapToLong(ICloud.MBSFile::getSize).findAny().orElse(0);
        }

        logger.trace(">> next() > entries: {} size: {}", files.size(), total);
        return files;
    }
}
