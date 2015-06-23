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

import com.github.horrorho.liquiddonkey.cloud.file.Mode;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
public class Snapshot {

    private static final Logger logger = LoggerFactory.getLogger(Snapshot.class);

    public static Snapshot newInstance(int id, List<ICloud.MBSFile> files, Predicate<ICloud.MBSFile> filter) {
        return new Snapshot(id, files, filter);
    }

    private final int id;
    private final List<ICloud.MBSFile> files;
    private final Predicate<ICloud.MBSFile> filter;

    Snapshot(int id, List<ICloud.MBSFile> files, Predicate<ICloud.MBSFile> filter) {
        this.id = id;
        this.files = files;
        this.filter = filter;
    }

    public ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures() {

        Map<Mode, List<ICloud.MBSFile>> modeToFiles = groupingBy(files, Mode::mode);
        logger.info("-- signatures() > modes: {}", summary(modeToFiles));

        Map<Boolean, List<ICloud.MBSFile>> isFilteredToFiles = groupingBy(files, filter::test);
        logger.info("-- signatures() > filtered: {}", summary(isFilteredToFiles));

        ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatureToFileMap
                = isFilteredToFiles.getOrDefault(Boolean.TRUE, new ArrayList<>()).stream()
                .collect(Collectors.groupingByConcurrent(ICloud.MBSFile::getSignature, Collectors.toSet()));
        return signatureToFileMap;
    }

    <T, K> Map<K, List<T>> groupingBy(List<T> t, Function<T, K> classifier) {
        return t == null
                ? new HashMap<>()
                : t.stream().collect(Collectors.groupingBy(classifier));
    }

    <K, V> Map<K, Integer> summary(Map<K, List<V>> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }

    public int id() {
        return id;
    }
}
