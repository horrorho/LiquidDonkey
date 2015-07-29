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
package com.github.horrorho.liquiddonkey.cloud.data;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import java.util.ArrayList;
import java.util.List;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Snapshot.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public class Snapshot extends Backup {

    private final ICloud.MBSSnapshot mbsSnapshot;
    private final List<ICloud.MBSFile> files;
    private final Backup backup;

    Snapshot(Backup backup, ICloud.MBSSnapshot mbsSnapshot, List<ICloud.MBSFile> files) {
        super(backup);
        this.mbsSnapshot = mbsSnapshot;
        this.files = files;
        this.backup = backup;
    }

    Snapshot(Snapshot snapshot, List<ICloud.MBSFile> files) {
        this(snapshot.backup, snapshot.mbsSnapshot, files);
    }

    Snapshot(Snapshot snapshot) {
        this(snapshot.backup, snapshot.mbsSnapshot, snapshot.files());
    }

    public final int snapshotID() {
        return mbsSnapshot.getSnapshotID();
    }

    public final ICloud.MBSSnapshot mbsSnapshot() {
        return mbsSnapshot;
    }

    public final List<ICloud.MBSFile> files() {
        return new ArrayList<>(files);
    }

    public final int filesCount() {
        return files.size();
    }
    
    @Override
    public String toString() {
        return "Snapshot{"
                + "dsPrsID=" + dsPrsID()
                + "udid=" + backupUDID()
                + "mbsSnapshot=" + mbsSnapshot
                + ", files=" + files.size()
                + '}';
    }
}
