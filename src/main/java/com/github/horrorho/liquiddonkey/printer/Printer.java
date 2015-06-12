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
package com.github.horrorho.liquiddonkey.printer;

import com.github.horrorho.liquiddonkey.settings.config.PrintConfig;
import java.io.PrintWriter;
import java.io.StringWriter;
import net.jcip.annotations.ThreadSafe;

/**
 * Synchronized print utility class.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class Printer {

    public static Printer instanceOf(PrintConfig config) {
        return instanceOf(config
                .toPrintStackTrace());
    }

    public static Printer instanceOf(boolean toPrintStackTrace) {
        return toPrintStackTrace
                ? PRINT_STACK_TRACE_ENABLED
                : PRINT_STACK_TRACE_DISABLED;
    }

    private static final Printer PRINT_STACK_TRACE_ENABLED = new Printer(true);
    private static final Printer PRINT_STACK_TRACE_DISABLED = new Printer(false);
    private static final Object LOCK = new Object();

    private final boolean toPrintStackTrace;

    Printer(boolean toPrintStackTrace) {
        this.toPrintStackTrace = toPrintStackTrace;
    }

    public void println(Level level, String string) {
        println(level + string);
    }

    public void println(Level level, Exception ex) {
        println(level, toString(ex));
    }

    public void println(Level level, String string, Exception ex) {
        println(level, string + " " + toString(ex));
    }

    void println(String string) {
        synchronized (LOCK) {
            System.out.println(string);
        }
    }

    String toString(Exception exception) {
        if (toPrintStackTrace) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);

            return stringWriter.toString();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(simpleNameMessage(exception));
            return stringBuilder.toString();
        }
    }

    String simpleNameMessage(Throwable throwable) {
        String s = throwable.getClass().getSimpleName();
        String message = throwable.getLocalizedMessage();
        return message != null
                ? s + ": " + message
                : s;
    }
}
