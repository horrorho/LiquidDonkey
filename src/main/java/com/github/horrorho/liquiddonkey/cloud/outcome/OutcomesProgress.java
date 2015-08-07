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
public final class OutcomesProgress implements Consumer<Map<ICloud.MBSFile, Outcome>> {

    public static OutcomesProgress from(Snapshot snapshot, Printer out) {
        long totalBytes = snapshot.files().stream()
                .mapToLong(ICloud.MBSFile::getSize)
                .sum();
        long tickDelta = totalBytes / 1000;

        logger.debug("-- from() > totalBytes: {}", totalBytes);
        return new OutcomesProgress(tickDelta, out, false);
    }

    private static final Logger logger = LoggerFactory.getLogger(OutcomesProgress.class);

    private final long tickDelta;
    private final Printer out;
    private final Lock lock;

    @GuardedBy("lock")
    private long bytes;
    @GuardedBy("lock")
    private int tick;
    @GuardedBy("lock")
    private int percentage;
    @GuardedBy("lock")
    private long ms;
    @GuardedBy("lock")
    private final StringBuffer sb = new StringBuffer();

    OutcomesProgress(long tickDelta, Printer out, Lock lock, long bytes, int tick, int percentage, long ms) {
        this.tickDelta = tickDelta;
        this.out = Objects.requireNonNull(out);
        this.lock = Objects.requireNonNull(lock);
        this.bytes = bytes;
        this.tick = tick;
        this.percentage = percentage;
        this.ms = ms;
    }

    public OutcomesProgress(long tickDelta, Printer out, boolean fair) {
        this(tickDelta, out, new ReentrantLock(fair), 0, 0, 0, System.currentTimeMillis());
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

    void process(long delta) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            bytes += delta;
            while (bytes >= tickDelta) {
                bytes -= tickDelta;
                tick();
            }
        } finally {
            lock.unlock();
        }
    }

    @GuardedBy("lock")
    void tick() {
        if (percentage > 100) {
            return;
        }

        if (tick == 0) {
            sb.append(String.format("%2s", percentage)).append("% .");
            out.print("\r" + sb.toString());
            percentage += 4;
            tick++;

        } else if (tick == 39) {
            long currentMs = System.currentTimeMillis();
            long deltaMs = currentMs - ms;
            ms = currentMs;

            long rate = deltaMs == 0
                    ? -1
                    : tickDelta * 40000 / (deltaMs * 1024);

            sb.append('.').append(String.format("%6s", rate)).append(" KiB/s");
            out.print("\r" + sb.toString());
            sb.setLength(0);
            out.println();
            tick = 0;

        } else {
            if (tick % 5 == 0) {
                sb.append(" .");
            } else {
                sb.append(".");
            }
            out.print("\r" + sb.toString());
            tick++;
        }
    }
}
