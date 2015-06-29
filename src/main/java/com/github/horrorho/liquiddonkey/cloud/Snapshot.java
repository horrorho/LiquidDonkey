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
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Snapshot {

    public static Snapshot newInstance(int id, Backup backup, List<ICloud.MBSFile> files) {
        return new Snapshot(id, backup, files);
    }

    private final int id;
    private final Backup backup;
    private final List<ICloud.MBSFile> files;

    Snapshot(int id, Backup backup, List<ICloud.MBSFile> files) {
        this.id = id;
        this.backup = Objects.requireNonNull(backup);
        this.files = Objects.requireNonNull(files);
    }

    public int id() {
        return id;
    }

    public Backup backup() {
        return backup;
    }

    public List<ICloud.MBSFile> files() {
        return files;
    }

    @Override
    public String toString() {
        return "Snapshot{" + "id=" + id + ", backup=" + backup + ", files=" + files.size() + '}';
    }
}
