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

import java.util.concurrent.atomic.AtomicLong;
import net.jcip.annotations.ThreadSafe;

/**
 *
 * @author Ahseya
 */
@ThreadSafe
public class Tally {

    public static Tally newInstance(long total) {
        return new Tally(total);
    }

    private final long total;
    private final AtomicLong cancelled = new AtomicLong();
    private final AtomicLong filtered = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong local = new AtomicLong();

    Tally(long total) {
        this.total = total;
    }

    public Tally addCancelled(long delta) {
        cancelled.addAndGet(delta);
        return this;
    }

    public long cancelled() {
        return cancelled.get();
    }

    public Tally addFiltered(long delta) {
        filtered.addAndGet(delta);
        return this;
    }

    public long filtered() {
        return filtered.get();
    }

    public Tally addFailed(long delta) {
        failed.addAndGet(delta);
        return this;
    }

    public long failed() {
        return failed.get();
    }

    public Tally addLocal(long delta) {
        local.addAndGet(delta);
        return this;
    }

    public long local() {
        return local.get();
    }

    public long total() {
        return total;
    }
}
