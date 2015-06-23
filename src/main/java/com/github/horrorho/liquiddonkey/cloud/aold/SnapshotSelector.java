///*
// * The MIT License
// *
// * Copyright 2015 Ahseya.
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// * THE SOFTWARE.
// */
//package com.github.horrorho.liquiddonkey.cloud.aold;
//
//import com.github.horrorho.liquiddonkey.cloud.Backup;
//import com.github.horrorho.liquiddonkey.printer.Level;
//import com.github.horrorho.liquiddonkey.printer.Printer;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.Objects;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//import net.jcip.annotations.Immutable;
//import net.jcip.annotations.ThreadSafe;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * SnapshotSelector.
// *
// * @author Ahseya
// */
//@Immutable
//@ThreadSafe
//public final class SnapshotSelector implements Function<Backup, List<Integer>> {
//
//    private static final Logger logger = LoggerFactory.getLogger(SnapshotSelector.class);
//
//    /**
//     * Returns a new instance.
//     *
//     * @param printer not null
//     * @param requestedSnapshots the requests snapshots, not null
//     * @return a new instance, not null
//     */
//    public static SnapshotSelector newInstance(Printer printer, Collection<Integer> requestedSnapshots) {
//        return new SnapshotSelector(printer, requestedSnapshots);
//    }
//
//    private final Printer printer;
//    private final List<Integer> requestedSnapshots;
//
//    SnapshotSelector(Printer printer, Collection<Integer> requestedSnapshots) {
//        this.printer = Objects.requireNonNull(printer);
//        this.requestedSnapshots = new ArrayList<>(requestedSnapshots);
//    }
//
//    @Override
//    public List<Integer> apply(Backup backup) {
//        logger.trace("<< apply() < backup: {}", backup.udidString());
//
//        List<Integer> available = backup.snapshots();
//        int latestSnapshot = available.stream().mapToInt(Integer::intValue).max().orElse(0);
//
//        List<Integer> selection = parseUserSelection(latestSnapshot, available, requestedSnapshots);
//        List<Integer> resolved = selection.isEmpty()
//                ? available
//                : resolve(selection, available, printer);
//
//        logger.debug(">> apply() > snapshot/s: {}", resolved);
//        return resolved;
//    }
//
//    List<Integer> parseUserSelection(int latestSnapshot, List<Integer> available, Collection<Integer> requested) {
//        return requested.isEmpty()
//                ? new ArrayList<>()
//                : requested.stream().map(request -> request < 0 ? latestSnapshot + request + 1 : request)
//                .collect(Collectors.toList());
//
//    }
//
//    List<Integer> resolve(Collection<Integer> selection, Collection<Integer> available, Printer printer) {
//        List<Integer> selected = selection.stream()
//                .filter(snapshot -> {
//                    if (!available.contains(snapshot)) {
//                        printer.println(Level.WARN, "backup doesn't contain requested snapshot: " + snapshot);
//                        return false;
//                    }
//                    return true;
//                }).collect(Collectors.toList());
//        return selected;
//    }
//}
