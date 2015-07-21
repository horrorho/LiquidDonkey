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
package com.github.horrorho.liquiddonkey.cloud.clients;

import com.github.horrorho.liquiddonkey.cloud.clients.*;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Snapshot.
 *
 * @author Ahseya
 */
public class Snapshot {

    public static Snapshot from(Snapshot snapshot, Predicate<ICloud.MBSFile> filter) {

        List<ICloud.MBSFile> filtered = snapshot.files.stream().filter(filter::test).collect(Collectors.toList());

        return new Snapshot(snapshot.backup, snapshot.mbsSnapshot, filtered);
    }

    public static Snapshot from(Backup backup, ICloud.MBSSnapshot snapshot, List<ICloud.MBSFile> files) {
        return new Snapshot(backup, snapshot, files);
    }

    private final Backup backup;
    private final ICloud.MBSSnapshot mbsSnapshot;
    private final List<ICloud.MBSFile> files;

    Snapshot(Backup backup, ICloud.MBSSnapshot mbsSnapshot, List<ICloud.MBSFile> files) {
        this.backup = Objects.requireNonNull(backup);
        this.mbsSnapshot = Objects.requireNonNull(mbsSnapshot);
        this.files = new ArrayList<>(files);
    }

    public Backup backup() {
        return backup;
    }

    public ICloud.MBSSnapshot snapshot() {
        return mbsSnapshot;
    }

    public String udid() {
        return backup.udidString();
    }

    public int id() {
        return mbsSnapshot.getSnapshotID();
    }

    public List<ICloud.MBSFile> files() {
        return new ArrayList<>(files);
    }

    /**
     * Returns a new concurrent signature to file Set map.
     *
     * @return a new concurrent signature to file Set map, not null
     */
    public ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures() {
        return files().stream()
                .collect(Collectors.groupingByConcurrent(ICloud.MBSFile::getSignature, Collectors.toSet()));
    }
}
