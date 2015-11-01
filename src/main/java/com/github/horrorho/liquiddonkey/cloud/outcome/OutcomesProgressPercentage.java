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
package com.github.horrorho.liquiddonkey.cloud.outcome;

import com.github.horrorho.liquiddonkey.cloud.data.Snapshot;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.util.Printer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OutcomesProgress.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class OutcomesProgressPercentage implements Consumer<Map<ICloud.MBSFile, Outcome>> {

    public static OutcomesProgressPercentage from(Snapshot snapshot, Printer out) {
        long totalBytes = snapshot.files().stream()
                .mapToLong(ICloud.MBSFile::getSize)
                .sum();
        long tickDelta = totalBytes / 1000;

        logger.debug("-- from() > totalBytes: {}", totalBytes);
        return new OutcomesProgressPercentage(totalBytes, tickDelta, out, false);
    }

    private static final Logger logger = LoggerFactory.getLogger(OutcomesProgressPercentage.class);

    private final long totalBytes;
    private final Printer out;
    private final Lock lock;

    @GuardedBy("lock")
    private long bytes;
    @GuardedBy("lock")
    private double percentage;

    OutcomesProgressPercentage(
            long totalBytes,
            Printer out,
            Lock lock,
            long bytes,
            double percentage) {

        this.totalBytes = totalBytes;
        this.out = Objects.requireNonNull(out);
        this.lock = Objects.requireNonNull(lock);
        this.bytes = bytes;
        this.percentage = percentage;;
    }

    public OutcomesProgressPercentage(long totalBytes, long tickDelta, Printer out, boolean fair) {
        this(totalBytes, out, new ReentrantLock(fair), 0, 0.0);
    }

    @Override
    public void accept(Map<ICloud.MBSFile, Outcome> outcomes) {
        long delta = outcomes.keySet().stream()
                .mapToLong(ICloud.MBSFile::getSize)
                .sum();

        try {
            process(delta);

        } catch (InterruptedException ex) {
            logger.warn("-- accept() > re-interrupted: {}", ex);
            Thread.currentThread().interrupt();
        }
    }

    public long totalBytes() {
        return totalBytes;
    }

    void process(long delta) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            bytes += delta;

            double p = (double) bytes * 10.0 / (double) totalBytes;

            for (int i = (int) percentage; i < (int) p; i += 1) {
                out.println(i + 1 + "0%");
            }

            percentage = p;

        } finally {
            lock.unlock();
        }
    }
}
