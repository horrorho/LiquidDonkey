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
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StackTraceHook. Shutdown hook stack dump.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class StackTraceHook extends Thread {

    public static StackTraceHook from(Printer printer) {
        return new StackTraceHook(printer);
    }

    private static final Logger logger = LoggerFactory.getLogger(StackTraceHook.class);

    private final Printer printer;
    private boolean isAdded;

    StackTraceHook(Printer printer, boolean isAdded) {
        this.printer = printer;
        this.isAdded = isAdded;
    }

    StackTraceHook(Printer printer) {
        this(printer, false);
    }

    /**
     * Add virtual-machine hook.
     */
    public void add() {
        if (isAdded) {
            logger.warn("-- add() > already added");
        }
        try {
            Runtime.getRuntime().addShutdownHook(this);
            isAdded = true;
        } catch (IllegalStateException | IllegalArgumentException | SecurityException ex) {
            logger.warn("-- add() > exception: ", ex);
        }
    }

    /**
     * Remove virtual-machine hook.
     */
    public void remove() {
        if (!isAdded) {
            logger.warn("-- remove() > already removed");
        }
        try {
            Runtime.getRuntime().removeShutdownHook(this);
            isAdded = false;
        } catch (IllegalStateException | SecurityException ex) {
            logger.warn("-- remove() > exception: ", ex);
        }
    }

    @Override
    public void run() {
        Thread.getAllStackTraces().entrySet().stream().forEach(stackTrace -> {
            Thread thread = stackTrace.getKey();
            ThreadGroup threadGroup = thread.getThreadGroup();
            if (threadGroup != null && "main".equals(threadGroup.getName())) {
                printer.println(thread);
                Arrays.asList(stackTrace.getValue()).forEach(trace -> printer.println("\t" + trace));
            }
        });
    }
}
