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
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;

/**
 *
 * @author Ahseya
 */
@NotThreadSafe
public class Tally {

    public static Tally newInstance() {
        return new Tally();
    }

    public static long size(Map<ByteString, Set<ICloud.MBSFile>> signatures) {
        return signatures.values().stream()
                .flatMap(Set::stream)
                .mapToLong(MBSFile::getSize)
                .sum();
    }

    public static long count(Map<ByteString, Set<ICloud.MBSFile>> signatures) {
        return signatures.values().stream()
                .mapToLong(Set::size)
                .sum();
    }

    private long total = 0;
    private long progress = 0;

    Tally() {
    }

    public long total() {
        return total;
    }

    public Tally reset(long total) {
        this.total = total;
        this.progress = 0;
        return this;
    }

    public long progress() {
        return progress;
    }

    public Tally setProgress(long progress) {
        this.progress = progress;
        return this;
    }
}
