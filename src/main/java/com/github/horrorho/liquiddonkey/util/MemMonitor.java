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
package com.github.horrorho.liquiddonkey.util;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MemMonitor.
 * <p>
 * Simple memory monitor,
 *
 * @author Ahseya
 */
public class MemMonitor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MemMonitor.class);

    public static MemMonitor from(long intervalMs) {
        return new MemMonitor(intervalMs, new AtomicLong(0), false);
    }

    private final long intervalMs;
    private final AtomicLong max;
    private volatile boolean stop;

    MemMonitor(long intervalMs, AtomicLong max, boolean stop) {
        this.intervalMs = intervalMs;
        this.max = Objects.requireNonNull(max);
        this.stop = stop;
    }

    @Override
    public void run() {
        logger.trace("<< run()");
        Runtime runtime = Runtime.getRuntime();
        try {
            while (!stop) {
                long usedMb = runtime.totalMemory() - runtime.freeMemory();
                long totalMb = runtime.totalMemory();
                max.getAndUpdate(current -> usedMb > current ? usedMb : current);

                logger.debug("-- run() > memory (MB): {} ({}) / {}",
                        usedMb / 1048510, max.get() / 1048510, totalMb / 1048510);

                TimeUnit.MILLISECONDS.sleep(intervalMs);
            }
        } catch (InterruptedException | RuntimeException ex) {
            logger.warn("-- run() > exception: ", ex);
        } finally {
            logger.trace(">> run()");
        }
    }

    public void stop() {
        stop = true;
    }

    public long max() {
        return max.get();
    }
}
