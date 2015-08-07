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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.util.Printer;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.jcip.annotations.ThreadSafe;

/**
 * OutcomesPrinter.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class OutcomesPrinter
        implements Consumer<Map<ICloud.MBSFile, Outcome>>, BiConsumer<String, Map<ICloud.MBSFile, Outcome>> {

    public static OutcomesPrinter create() {
        return new OutcomesPrinter(System.out::print, System.err::print);
    }

    public static OutcomesPrinter from(Printer out, Printer err) {
        return new OutcomesPrinter(out, err);
    }

    private final Printer out;
    private final Printer err;

    OutcomesPrinter(Printer out, Printer err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public void accept(Map<ICloud.MBSFile, Outcome> outcomes) {
        accept("\t", outcomes);
    }

    @Override
    public void accept(String prefix, Map<ICloud.MBSFile, Outcome> outcomes) {
        if (outcomes != null) {
            outcomes.entrySet().stream()
                    .forEach(entry -> {
                        ICloud.MBSFile file = entry.getKey();
                        Outcome outcome = entry.getValue();
                        Printer printer = outcome.isSuccess() ? out : err;
                        printer.println(prefix + file.getDomain() + " " + file.getRelativePath() + " " + outcome);
                    });
        }
    }
}
