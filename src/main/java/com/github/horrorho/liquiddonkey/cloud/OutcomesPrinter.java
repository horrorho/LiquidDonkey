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
import java.io.PrintStream;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * OutcomesPrinter.
 *
 * @author Ahseya
 */
public class OutcomesPrinter
        implements Consumer<Map<ICloud.MBSFile, Outcome>>, BiConsumer<Double, Map<ICloud.MBSFile, Outcome>> {

    public static OutcomesPrinter create() {
        return new OutcomesPrinter(System.out, System.err);
    }

    public static OutcomesPrinter from(PrintStream out, PrintStream err) {
        return new OutcomesPrinter(out, err);
    }

    private final PrintStream out;
    private final PrintStream err;

    OutcomesPrinter(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public void accept(Map<ICloud.MBSFile, Outcome> outcomes) {
        print("\t", outcomes);
    }

    @Override
    public void accept(Double progress, Map<ICloud.MBSFile, Outcome> outcomes) {
        String percent = "   " + String.format("%4s", (int) (progress * 100.0) + "% ");
        print(percent, outcomes);
    }

    public void print(String prefix, Map<ICloud.MBSFile, Outcome> outcomes) {
        if (outcomes != null) {
            outcomes.entrySet().stream()
                    .forEach(entry -> {
                        ICloud.MBSFile file = entry.getKey();
                        Outcome result = entry.getValue();
                        PrintStream printStream = result.isSuccess() ? out : err;
                        printStream.println(prefix + file.getDomain() + " " + file.getRelativePath() + " " + result);
                    });
        }
    }
}
