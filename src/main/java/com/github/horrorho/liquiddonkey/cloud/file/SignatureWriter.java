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
package com.github.horrorho.liquiddonkey.cloud.file;
 
import com.github.horrorho.liquiddonkey.cloud.data.Snapshot;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Signature Writer.
 * <p>
 * Writes out the {@link ICloud.MBSFile}/s referenced by signatures. Locking single threaded mode from action.
 *
 * @author ahseya
 */
@ThreadSafe
public final class SignatureWriter {

    private static final Logger logger = LoggerFactory.getLogger(SignatureWriter.class);

    /**
     * Returns a new instance.
     *
     * @param snapshot not null
     * @param fileConfig not null
     * @return a new instance, not null
     */
    public static SignatureWriter from(Snapshot  snapshot, FileConfig fileConfig) {
        logger.trace("<< from() < snapshot: {} fileConfig: {}", snapshot, fileConfig);

        CloudFileWriter cloudWriter = CloudFileWriter.from(snapshot, fileConfig);
        Map<ByteString, Set<ICloud.MBSFile>> signatures = snapshot.signatures();

        long totalBytes = signatures.values().stream()
                .flatMap(Set::stream)
                .mapToLong(ICloud.MBSFile::getSize)
                .sum();

        Lock lock = new ReentrantLock();

        SignatureWriter instance = new SignatureWriter(signatures, cloudWriter, lock, totalBytes, 0);

        logger.trace(">> from() > {}", instance);
        return instance;
    }

    private final Map<ByteString, Set<ICloud.MBSFile>> signatureToFileSet;
    private final CloudFileWriter cloudWriter;
    private final Lock lock;
    private final long totalBytes;
    private volatile long outBytes;

    public SignatureWriter(
            Map<ByteString, Set<MBSFile>> signatureToFileSet,
            CloudFileWriter cloudWriter,
            Lock lock,
            long totalBytes,
            long outBytes) {

        this.signatureToFileSet = Objects.requireNonNull(signatureToFileSet);
        this.cloudWriter = Objects.requireNonNull(cloudWriter);
        this.lock = Objects.requireNonNull(lock);
        this.totalBytes = totalBytes;
        this.outBytes = outBytes;
    }

    /**
     * Writes the files referenced by the specified signature.
     * <p>
     * If encrypted, attempts to decrypt the file. Optionally set's the last-modified timestamp.
     *
     * @param signature not null
     * @param writer not null
     * @return map from ICloud.MBSFile to CloudWriterResult/s, or null if the signature doesn't reference any files
     * @throws IOException
     * @throws InterruptedException
     */
    public Map<ICloud.MBSFile, WriterResult> write(ByteString signature, IOFunction<OutputStream, Long> writer)
            throws IOException, InterruptedException {

        logger.trace("<< write() < signature: {}", Bytes.hex(signature));

        lock.lockInterruptibly();
        try {
            Set<ICloud.MBSFile> files = signatureToFileSet.get(signature);
            if (files == null) {
                return null;
            }

            Map<ICloud.MBSFile, WriterResult> results = new HashMap<>();
            for (ICloud.MBSFile file : files) {
                results.put(file, cloudWriter.write(file, writer));
                outBytes += file.getSize();
            }

            signatureToFileSet.remove(signature);

            logger.debug("-- write() > out: {} total: {}", outBytes, totalBytes);
            logger.trace(">> write()");
            return results;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "SignatureWriter{"
                + "signatures=" + signatureToFileSet.size()
                + ", cloudWriter=" + cloudWriter
                + ", lock=" + lock
                + ", totalBytes=" + totalBytes
                + ", outBytes=" + outBytes
                + '}';
    }
}
