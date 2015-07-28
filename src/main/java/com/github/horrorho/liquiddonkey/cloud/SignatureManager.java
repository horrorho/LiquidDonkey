/* 
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies from the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions from the Software.
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

import com.github.horrorho.liquiddonkey.cloud.data.Snapshot;
import com.github.horrorho.liquiddonkey.cloud.file.CloudFileWriter;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import com.github.horrorho.liquiddonkey.cloud.store.DataWriter;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Signature Manager.
 * <p>
 * Manages signatures.
 *
 * @author ahseya
 */
@ThreadSafe
public final class SignatureManager {

    private static final Logger logger = LoggerFactory.getLogger(SignatureManager.class);

    /**
     * Returns a new instance.
     *
     * @param snapshot not null
     * @param fileConfig not null
     * @return a new instance, not null
     */
    public static SignatureManager from(Snapshot snapshot, FileConfig fileConfig) {
        logger.trace("<< from() < snapshot: {} fileConfig: {}", snapshot, fileConfig);

        CloudFileWriter cloudWriter = CloudFileWriter.from(snapshot, fileConfig);
        Map<ByteString, Set<ICloud.MBSFile>> signatures = snapshot.files().stream()
                .collect(Collectors.groupingByConcurrent(ICloud.MBSFile::getSignature, Collectors.toSet()));

        long totalBytes = signatures.values().stream()
                .flatMap(Set::stream)
                .mapToLong(ICloud.MBSFile::getSize)
                .sum();

        Lock lock = new ReentrantLock();

        SignatureManager instance
                = new SignatureManager(signatures, cloudWriter, lock, totalBytes, new AtomicLong(0), new AtomicLong(0));

        logger.trace(">> from() > {}", instance);
        return instance;
    }

    private final Map<ByteString, Set<ICloud.MBSFile>> signatureToFileSet;
    private final CloudFileWriter cloudWriter;
    private final Lock lock;
    private final long totalBytes;
    private final AtomicLong outBytes;
    private final AtomicLong failedBytes;

    public SignatureManager(
            Map<ByteString, Set<MBSFile>> signatureToFileSet,
            CloudFileWriter cloudWriter,
            Lock lock,
            long totalBytes,
            AtomicLong outBytes,
            AtomicLong failedBytes) {

        this.signatureToFileSet = Objects.requireNonNull(signatureToFileSet);
        this.cloudWriter = Objects.requireNonNull(cloudWriter);
        this.lock = Objects.requireNonNull(lock);
        this.totalBytes = totalBytes;
        this.outBytes = outBytes;
        this.failedBytes = failedBytes;
    }

    // Doesn't close the writers
    public Map<ICloud.MBSFile, FileOutcome> write(Map<ByteString, DataWriter> writers)
            throws IOException, InterruptedException {

        logger.trace("<< write() < signatures: {}", writers.keySet());

        Map<ICloud.MBSFile, FileOutcome> outcomes = new HashMap<>();
        for (Map.Entry<ByteString, DataWriter> entry : writers.entrySet()) {
            outcomes.putAll(write(entry.getKey(), entry.getValue()));
        }

        logger.trace(">> write()");
        return outcomes;
    }

    /**
     * Writes the files referenced by the specified signature.
     * <p>
     * If encrypted, attempts to decrypt the file. Optionally set's the last-modified timestamp.
     * <p>
     * The writer is not closed.
     *
     * @param signature not null
     * @param writer not null
     * @return map from ICloud.MBSFile to CloudWriterResult/s, empty if the signature doesn't reference any files
     * @throws IOException
     * @throws InterruptedException
     */
    public Map<ICloud.MBSFile, FileOutcome> write(ByteString signature, IOFunction<OutputStream, Long> writer)
            throws IOException, InterruptedException {

        logger.trace("<< write() < signature: {}", Bytes.hex(signature));

        lock.lockInterruptibly();
        try {
            Map<ICloud.MBSFile, FileOutcome> outcomes = new HashMap<>();

            Set<ICloud.MBSFile> files = signatureToFileSet.remove(signature);
            if (files == null) {
                logger.warn("-- write() > unreferenced signature: {}", Bytes.hex(signature));
            } else {
                for (ICloud.MBSFile file : files) {
                    outcomes.put(file, cloudWriter.write(file, writer));
                    outBytes.addAndGet(file.getSize());
                }
                logger.debug("-- write() > out: {} failed: {} total: {}", outBytes, failedBytes, totalBytes);
            }

            logger.trace(">> write()");
            return outcomes;
        } finally {
            lock.unlock();
        }
    }

    public Map<ICloud.MBSFile, FileOutcome> fail(Set<ByteString> signatures) {
        logger.trace("<< fail() < signatures: {}", signatures);

        Map<ICloud.MBSFile, FileOutcome> outcomes = new HashMap<>();
        signatures.stream().forEach(signature -> outcomes.putAll(fail(signature)));

        logger.trace(">> fail()");
        return outcomes;
    }

    public Map<ICloud.MBSFile, FileOutcome> fail(ByteString signature) {
        logger.trace("<< fail() < signature: {}", Bytes.hex(signature));

        Map<ICloud.MBSFile, FileOutcome> outcomes = new HashMap<>();

        Set<ICloud.MBSFile> files = signatureToFileSet.remove(signature);
        if (files == null) {
            logger.warn("-- writer() > unreferenced signature: {}", Bytes.hex(signature));
        } else {
            long total = files.stream()
                    .peek(file -> outcomes.put(file, FileOutcome.FAILED_DOWNLOAD))
                    .mapToLong(ICloud.MBSFile::getSize)
                    .sum();
            failedBytes.addAndGet(total);
        }

        logger.trace(">> fail()");
        return outcomes;
    }

    public Set<ByteString> remainingSignatures() {
        return new HashSet<>(signatureToFileSet.keySet());
    }

    public Set<ICloud.MBSFile> remainingFiles() {
        return signatureToFileSet.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "SignatureWriter{"
                + "signatures=" + signatureToFileSet.size()
                + ", cloudWriter=" + cloudWriter
                + ", lock=" + lock
                + ", totalBytes=" + totalBytes
                + ", outBytes=" + outBytes
                + ", failedBytes=" + failedBytes
                + '}';
    }
}
