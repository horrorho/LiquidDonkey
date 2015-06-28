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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SnapshotSelector.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class SnapshotFilter implements Predicate<Integer> {

    public static SnapshotFilter newInstance(Backup backup, Collection<Integer> requested) {
        return newInstance(backup.snapshots(), requested);
    }

    public static SnapshotFilter newInstance(Collection<Integer> available, Collection<Integer> requested) {
        int latest = available.stream().mapToInt(Integer::intValue).max().orElse(0);

        return new SnapshotFilter(
                requested.stream().map(request -> request < 0 ? latest + request + 1 : request)
                .filter(id -> id > 0)
                .collect(Collectors.toSet()));
    }

    private final Set<Integer> ids;

    SnapshotFilter(Set<Integer> ids) {
        this.ids = Objects.requireNonNull(ids);
    }

    @Override
    public boolean test(Integer id) {
        return ids.contains(id);
    }

    @Override
    public String toString() {
        return "SnapshotFilter{" + "ids=" + ids + '}';
    }
}
