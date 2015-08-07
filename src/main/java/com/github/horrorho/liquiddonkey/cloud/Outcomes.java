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
import com.github.horrorho.liquiddonkey.util.Printer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jcip.annotations.ThreadSafe;

/**
 * Outcomes.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class Outcomes implements Consumer<Map<ICloud.MBSFile, Outcome>> {

    public static Outcomes create() {
        return new Outcomes();
    }

    public static Outcomes add(Outcomes... outcomes) {
        ConcurrentMap<Outcome, Set<ICloud.MBSFile>> outcomeToFiles
                = Stream.of(outcomes)
                .flatMap(o -> o.outcomes().entrySet().stream())
                .collect(
                        Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                entry -> {
                                    Set<ICloud.MBSFile> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
                                    set.addAll(entry.getValue());
                                    return set;
                                },
                                (a, b) -> {
                                    a.addAll(b);
                                    return a;
                                }));

        return new Outcomes(outcomeToFiles);
    }

    private final ConcurrentMap<Outcome, Set<ICloud.MBSFile>> outcomeToFiles; // Requires concurrent Set.

    Outcomes(ConcurrentMap<Outcome, Set<ICloud.MBSFile>> outcomeToFiles) {
        this.outcomeToFiles = outcomeToFiles;
    }

    Outcomes() {
        this(new ConcurrentHashMap<>());
    }

    @Override
    public void accept(Map<ICloud.MBSFile, Outcome> outcomes) {
        if (outcomes != null) {
            outcomes.entrySet().stream()
                    .forEach(entry -> {
                        ICloud.MBSFile file = entry.getKey();
                        Outcome outcome = entry.getValue();

                        outcomeToFiles
                        .computeIfAbsent(outcome, o -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                        .add(file);
                    });
        }
    }

    public Map<Outcome, Set<ICloud.MBSFile>> outcomes() {
        return outcomeToFiles.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashSet<>(entry.getValue())));
    }

    public void print(Printer out) {
        outcomeToFiles.entrySet().stream().forEach(entry -> {
            Outcome outcome = entry.getKey();
            Set<ICloud.MBSFile> files = entry.getValue();

            int count = files == null ? 0 : files.size();

            if (count > 0) {
                out.println(outcome + ": " + count);
            }
        });
    }
}
