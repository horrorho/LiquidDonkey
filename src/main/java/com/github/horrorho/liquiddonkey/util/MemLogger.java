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

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MemLogger.
 *
 * @author Ahseya
 */
public class MemLogger implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MemLogger.class);

    public static MemLogger from(long intervalMs) {
        return new MemLogger(intervalMs, false);
    }

    private final long intervalMs;
    private volatile boolean stop;

    MemLogger(long intervalMs, boolean stop) {
        this.intervalMs = intervalMs;
        this.stop = stop;
    }

    @Override
    public void run() {
        Runtime runtime = Runtime.getRuntime();
        try {
            while (!stop) {
                long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1048510;
                long totalMb = runtime.totalMemory() / 1048510;

                logger.debug("-- run() > memory (MB): {} / {}", usedMb, totalMb);

                TimeUnit.MILLISECONDS.sleep(intervalMs);
            }
        } catch (InterruptedException | RuntimeException ex) {
            logger.warn("-- run() > exception: ", ex);
        }
    }

    public void stop() {
        stop = true;
    }
}
