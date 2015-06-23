///*
// * The MIT License
// *
// * Copyright 2015 Ahseya.
// *
// * Permission is hereby granted, free from charge, to any person obtaining a copy
// * from this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies from the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions from the Software.
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
//import com.github.horrorho.liquiddonkey.cloud.*;
//import com.github.horrorho.liquiddonkey.cloud.client.Client;
//import com.github.horrorho.liquiddonkey.exception.FatalException;
//import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBag;
//import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
//import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
//import com.github.horrorho.liquiddonkey.pipe.ArgumentExceptionPair;
//import com.github.horrorho.liquiddonkey.settings.config.DonkeyExecutorConfig;
//import com.github.horrorho.liquiddonkey.util.CallableFunction;
//import com.google.protobuf.ByteString;
//import java.io.IOException;
//import java.io.UncheckedIOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Set;
//import java.util.concurrent.CancellationException;
//import java.util.concurrent.ConcurrentMap;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//import net.jcip.annotations.Immutable;
//import net.jcip.annotations.ThreadSafe;
//import org.apache.http.client.HttpResponseException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Donkey executor.
// * <p>
// * Concurrency manager for {@link CallableFunction} Donkeys.
// *
// * @author Ahseya
// */
//@Immutable
//@ThreadSafe
//public final class DonkeyExecutor {
//
//    private static final Logger logger = LoggerFactory.getLogger(DonkeyExecutor.class);
//
//    /**
//     * Returns a new instance.
//     *
//     * @param factory not null
//     * @param config not null
//     * @return a new instance, not null
//     */
//    public static DonkeyExecutor newInstance(DonkeyFactory factory, DonkeyExecutorConfig config) {
//
//        return new DonkeyExecutor(
//                factory,
//                config.threads(),
//                config.staggerDelayMs(),
//                config.retryCount());
//    }
//
//    private final DonkeyFactory factory;
//    private final int threads;
//    private final int staggerDelayMs;
//    private final int retryCount;
//
//    DonkeyExecutor(DonkeyFactory factory, int threads, int staggerDelayMs, int retryCount) {
//        this.factory = Objects.requireNonNull(factory);
//        this.threads = threads;
//        this.staggerDelayMs = staggerDelayMs;
//        this.retryCount = retryCount;
//    }
//
//    /**
//     * Concurrently download the specified backup/ snapshot/ files.
//     *
//     * @param client not null
//     * @param backup not null
//     * @param keyBag not null
//     * @param snapshot the required snapshot
//     * @param signatures the required signatures, not null
//     * @param tally the Tally, not null
//     * @return the list of failed signatures, not null
//     */
//    public Map<ByteString, Set<ICloud.MBSFile>> execute(
//            Client client,
//            Backup backup,
//            KeyBag keyBag,
//            int snapshot,
//            ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures,
//            Tally tally) {
//
//        logger.trace("<< execute()");
//
//        tally.reset(Tally.size(signatures));
//        List<Map<ByteString, Set<ICloud.MBSFile>>> failed = new ArrayList<>();
//
//        int count = 0;
//        while (count++ < retryCount) {
//            logger.debug("-- execute() : count: {}/{} signatures: {}", count, retryCount, signatures.size());
//            failed.stream().forEach(signatures::putAll);
//            failed = doExecute(client, backup, keyBag, snapshot, signatures, tally);
//        }
//
//        logger.trace(">> execute()");
//        return failed.stream().collect(HashMap::new, Map::putAll, Map::putAll);
//    }
//
//    List<Map<ByteString, Set<ICloud.MBSFile>>> doExecute(
//            Client client,
//            Backup backup,
//            KeyBag keyBag,
//            int snapshot,
//            ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures,
//            Tally tally) {
//
//        logger.trace("<< doExecute() < signatureToFileList: {}", signatures.size());
//
//        ExecutorService executor = Executors.newFixedThreadPool(threads);
//
//        List<Future<List<ArgumentExceptionPair<Map<ByteString, Set<ICloud.MBSFile>>>>>> futures
//                = Stream.generate(() -> factory.from(client, backup, keyBag, snapshot, signatures))
//                .limit(threads)
//                .map(executor::submit)
//                .peek(x -> stagger())
//                .collect(Collectors.toList());
//        // Threads all fired up.
//        executor.shutdown();
//
//        // Await completion/ update progress.
//        while (!executor.isTerminated()) {
//            tally.setProgress(Tally.size(signatures) - tally.progress());
//            try {
//                TimeUnit.MILLISECONDS.sleep(1000);
//            } catch (InterruptedException ex) {
//                logger.warn("-- doExecute() > interrupted");
//            }
//        }
//
//        // All done.
//        List<Map<ByteString, Set<ICloud.MBSFile>>> failed = futures.stream()
//                .map(this::failed)
//                .flatMap(List::stream)
//                .collect(Collectors.toList());
//
//        if (!signatures.isEmpty()) {
//            logger.warn("-- doExecute() > signatureToFileList not empty: {}", signatures.size());
//        }
//        if (!failed.isEmpty()) {
//            logger.warn("-- doExecute() > failures: {}", failed.size());
//        }
//
//        logger.trace(">> doExecute() > failures: {}", failed.size());
//        return failed;
//    }
//
//    List<Map<ByteString, Set<ICloud.MBSFile>>> failed(
//            Future<List<ArgumentExceptionPair<Map<ByteString, Set<ICloud.MBSFile>>>>> future) {
//
//        try {
//            List<Map<ByteString, Set<ICloud.MBSFile>>> failed = future.get().stream()
//                    .filter(this::notFatalIO)
//                    .map(ArgumentExceptionPair::argument)
//                    .collect(Collectors.toList());
//
//            logger.trace("-- process() > failed lists: {}", failed.size());
//            return failed;
//
//        } catch (ExecutionException | CancellationException | InterruptedException ex) {
//            throw new FatalException(ex);
//        }
//    }
//
//    boolean notFatalIO(ArgumentExceptionPair<Map<ByteString, Set<ICloud.MBSFile>>> argumentExceptionPair) {
//        Exception ex = argumentExceptionPair.exception();
//
//        if (!(ex instanceof UncheckedIOException)) {
//            return true;
//        }
//
//        IOException io = ((UncheckedIOException) ex).getCause();
//
//        if (io instanceof HttpResponseException) {
//            int code = ((HttpResponseException) io).getStatusCode();
//            // Unauthorized
//            if (code == 401) {
//                throw new AuthenticationException(ex);
//            }
//        }
//        return true;
//    }
//
//    void stagger() {
//        try {
//            // Stagger to avoid triggering sensitive anti-flood protection with high thread counts,
//            // or to disrupt the initial coinciding download/ decrypt phases between threads.
//            TimeUnit.MILLISECONDS.sleep(staggerDelayMs);
//        } catch (InterruptedException ex) {
//            throw new FatalException(ex);
//        }
//    }
//}
