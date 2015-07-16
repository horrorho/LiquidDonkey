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
package com.github.horrorho.liquiddonkey.cloud.file;

import com.github.horrorho.liquiddonkey.cloud.Snapshot;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.iofunction.IOPredicate;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LocalFileFilter. Tests whether files are available locally.
 * <p>
 * Currently compares the file location, file size and last-modified timestamp.
 * <p>
 * Ideally the 160-bit file checksum would be employed, but the algorithm remains elusive.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class LocalFileFilter implements IOPredicate<ICloud.MBSFile> {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileFilter.class);

    /**
     * Returns a new instance.
     *
     * @param snapshot, not null
     * @param config, not null
     * @return a new instance, not null
     */
    public static LocalFileFilter from(
            Snapshot snapshot,
            FileConfig config) {

        return new LocalFileFilter(
                SnapshotDirectory.from(snapshot, config),
                config.setLastModifiedTimestamp(),
                config.isCombined()
        );
    }

    /**
     * Returns a new instance.
     *
     * @param directory, not null
     * @param toCheckLastModifiedTimestamp to test the last modified timestamp
     * @param isCombined
     * @return a new instance, not null
     */
    public static LocalFileFilter from(
            SnapshotDirectory directory,
            boolean toCheckLastModifiedTimestamp,
            boolean isCombined) {

        return new LocalFileFilter(directory, toCheckLastModifiedTimestamp, isCombined);
    }

    private final SnapshotDirectory directory;
    private final boolean toCheckLastModifiedTimestamp;
    private final boolean isCombined;

    LocalFileFilter(SnapshotDirectory directory, boolean toCheckLastModifiedTimestamp, boolean isCombined) {
        this.directory = Objects.requireNonNull(directory);
        this.toCheckLastModifiedTimestamp = toCheckLastModifiedTimestamp;
        this.isCombined = isCombined;
    }

    @Override
    public boolean test(ICloud.MBSFile remote) throws IOException, SecurityException {
        logger.trace("<< test() < file: {}", remote.getRelativePath());

        Path local = directory.apply(remote);

        boolean isLocal = isCombined
                ? testExists(local, remote) && !(testLastModified(local, remote) > 0) // Last modified equal or before.
                : testExists(local, remote) && testSize(local, remote) && (testLastModified(local, remote) == 0);

        logger.trace(">> test() > is local: {}", isLocal);
        return isLocal;
    }

    boolean testExists(Path local, ICloud.MBSFile remote) throws IOException {
        boolean match = Files.exists(local);

        logger.debug("-- testExists() < match: {} local: {} file: {}",
                match, local, remote.getRelativePath());
        return match;
    }

    boolean testSize(Path local, ICloud.MBSFile remote) throws IOException {
        Long localSize = Files.size(local);
        Long remoteSize = remote.hasSize()
                ? remote.getSize()
                : null;
        Long remoteDecryptedSize = remote.getAttributes().hasDecryptedSize()
                ? remote.getAttributes().getDecryptedSize()
                : null;
        boolean match = Objects.equals(remoteDecryptedSize, localSize) || Objects.equals(remoteSize, localSize);

        logger.debug("-- testSize() < match: {} local: {} remote: {} decrypted: {} file: {}",
                match, localSize, remoteSize, remoteDecryptedSize, remote.getRelativePath());
        return match;
    }

    int testLastModified(Path local, ICloud.MBSFile remote) throws IOException {
        FileTime localTimestamp = Files.getLastModifiedTime(local);
        FileTime remoteTimestamp = FileTime.from(remote.getAttributes().getLastModified(), TimeUnit.SECONDS);
        int comparision = localTimestamp.compareTo(remoteTimestamp);

        logger.debug("-- testLastModified() < comparision: {} local: {} remote: {} file: {}",
                comparision, localTimestamp, remoteTimestamp, remote.getRelativePath());
        return comparision;
    }
}
