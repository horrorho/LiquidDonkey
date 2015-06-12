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

import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.exception.FatalException;
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBag;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.pipe.ArgumentExceptionPair;
import com.github.horrorho.liquiddonkey.settings.config.DonkeyExecutorConfig;
import com.github.horrorho.liquiddonkey.util.CallableFunction;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Donkey executor.
 * <p>
 * Concurrency manager for {@link CallableFunction} Donkeys.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class DonkeyExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DonkeyExecutor.class);

    public static DonkeyExecutor newInstance(DonkeyFactory factory, DonkeyExecutorConfig config) {

        return new DonkeyExecutor(
                factory,
                config.threads(),
                config.staggerDelayMs(),
                config.retryCount());
    }

    private final DonkeyFactory factory;
    private final int threads;
    private final int staggerDelayMs;
    private final int retryCount;

    DonkeyExecutor(DonkeyFactory factory, int threads, int staggerDelayMs, int retryCount) {
        this.factory = Objects.requireNonNull(factory);
        this.threads = threads;
        this.staggerDelayMs = staggerDelayMs;
        this.retryCount = retryCount;
    }

    public ConcurrentMap<ByteString, Set<ICloud.MBSFile>> execute(
            Client client,
            Backup backup,
            KeyBag keyBag,
            int snapshot,
            ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatureToFileList) {

        logger.trace("<< execute()");

        // Defensive deep copy
        ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures
                = signatureToFileList.entrySet().stream()
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, entry -> new HashSet<>(entry.getValue())));

        int count = 0;
        do {
            logger.debug("-- execute() : count: {}/{} signatures: {}", count, retryCount, signatures.size());
            doExecute(client, backup, keyBag, snapshot, signatures)
                    .stream().forEach(signatures::putAll);
        } while (count++ < retryCount);

        logger.trace(">> execute() : failures: {}", signatures.size());
        return signatures;
    }

    List<Map<ByteString, Set<ICloud.MBSFile>>> doExecute(
            Client client,
            Backup backup,
            KeyBag keyBag,
            int snapshot,
            ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatureToFileList) {

        logger.trace("<< doExecute() < signatureToFileList: {}", signatureToFileList.size());

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Map<ByteString, Set<ICloud.MBSFile>>> failed
                = Stream.generate(() -> factory.newInstance(client, backup, keyBag, snapshot, signatureToFileList))
                .limit(threads)
                .map(executor::submit)
                .peek(x -> stagger())
                .collect(Collectors.toList()).stream()
                // All threads fired up. 
                .map(this::results)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        executor.shutdown();

        if (!signatureToFileList.isEmpty()) {
            logger.warn("-- doExecute() > signatureToFileList not empty: {}", signatureToFileList.size());
        }
        if (!failed.isEmpty()) {
            logger.warn("-- doExecute() > failures: {}", failed.size());
        }

        logger.trace(">> doExecute() > failures: {}", failed.size());
        return failed;
    }

    List<Map<ByteString, Set<ICloud.MBSFile>>> results(
            Future<List<ArgumentExceptionPair<Map<ByteString, Set<ICloud.MBSFile>>>>> future) {

        try {
            List<Map<ByteString, Set<ICloud.MBSFile>>> failed = future.get().stream()
                    .filter(this::fatal)
                    .map(ArgumentExceptionPair::argument)
                    .collect(Collectors.toList());

            logger.trace("-- process() > failed lists: {}", failed.size());
            return failed;

        } catch (ExecutionException | CancellationException | InterruptedException ex) {
            throw new FatalException(ex);
        }
    }

    boolean fatal(ArgumentExceptionPair<Map<ByteString, Set<ICloud.MBSFile>>> argumentExceptionPair) {
        Exception ex = argumentExceptionPair.exception();

        if (!(ex instanceof UncheckedIOException)) {
            return true;
        }

        IOException io = ((UncheckedIOException) ex).getCause();

        if (io instanceof HttpResponseException) {
            int code = ((HttpResponseException) io).getStatusCode();
            // Unauthorized
            if (code == 401) {
                throw new AuthenticationException(ex);
            }
        }
        return true;
    }

    void stagger() {
        try {
            // Stagger to avoid triggering sensitive anti-flood protection with high thread counts,
            // or to disrupt the initial conciding download/ decrypt phases between threads.
            TimeUnit.MILLISECONDS.sleep(staggerDelayMs);
        } catch (InterruptedException ex) {
            throw new FatalException(ex);
        }
    }
}
