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

import java.util.Arrays;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shutdown hook stack dump.
 *
 * @author Ahseya
 */
@Immutable
@NotThreadSafe
public final class DumpStackTraceHook extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DumpStackTraceHook.class);

    private static final DumpStackTraceHook instance = new DumpStackTraceHook();

    /**
     * Add virtual-machine hook.
     */
    public static void add() {
        try {
            Runtime.getRuntime().addShutdownHook(instance);
        } catch (IllegalStateException | IllegalArgumentException | SecurityException ex) {
            logger.warn("-- add() > exception: ", ex);
        }
    }

    /**
     * Remove virtual-machine hook.
     */
    public static void remove() {
        try {
            Runtime.getRuntime().removeShutdownHook(instance);
        } catch (IllegalStateException | SecurityException ex) {
            logger.warn("-- remove() > exception: ", ex);
        }
    }

    private DumpStackTraceHook() {
    }

    @Override
    public void run() {
        Thread.getAllStackTraces().entrySet().stream().forEach(stackTrace -> {
            Thread thread = stackTrace.getKey();
            ThreadGroup threadGroup = thread.getThreadGroup();
            if (threadGroup != null && "main".equals(threadGroup.getName())) {
                System.out.println(thread);
                Arrays.asList(stackTrace.getValue()).forEach(trace -> System.out.println("\t" + trace));
            }
        });
    }
}
