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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests whether files are available locally.
 * <p>
 * Currently compares the file location, file size and last-modified timestamp.
 * <p>
 * Ideally the 160-bit file checksum would be employed, but the algorithm remains elusive.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class SnapshotFileFilter implements Predicate<ICloud.MBSFile> {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotFileFilter.class);

    /**
     * Returns a new instance.
     *
     * @param directory the SnapshotDirectory referencing the local files, not null
     * @param snapshot the snapshot id
     * @param toCheckLastModifiedTimestamp to test the last modified timestamp
     * @return a new instance
     */
    public static SnapshotFileFilter newInstance(
            SnapshotDirectory directory,
            boolean toCheckLastModifiedTimestamp) {

        return new SnapshotFileFilter(directory, toCheckLastModifiedTimestamp);
    }

    private final SnapshotDirectory directory;
    private final boolean toCheckLastModifiedTimestamp;

    SnapshotFileFilter(SnapshotDirectory directory, boolean toCheckLastModifiedTimestamp) {
        this.directory = Objects.requireNonNull(directory);
        this.toCheckLastModifiedTimestamp = toCheckLastModifiedTimestamp;
    }

    @Override
    public boolean test(ICloud.MBSFile remote) {
        try {
            logger.trace("<< test() < {}", remote.getRelativePath());

            Path local = directory.apply(remote);

            if (!Files.exists(local)) {
                logger.trace(">> test() < doesn't exist: {}, false", remote.getRelativePath());
                return false;
            }

            if (!(testSize(local, remote) || testDecryptedSize(local, remote))) {
                logger.trace(">> test() < mismatched size: {}, false", remote.getRelativePath());
                return false;
            }

            if (toCheckLastModifiedTimestamp && !testLastModified(local, remote)) {
                logger.trace(">> test() < mismatched last-modified timestamp: {}, false", remote.getRelativePath());
                return false;
            }

            logger.trace(">> test() < {} true", remote.getRelativePath());
            return true;
        } catch (IOException | SecurityException ex) {
            throw new IllegalStateException("File io error");
        }
    }

    boolean testSize(Path local, ICloud.MBSFile remote) throws IOException {
        return remote.hasSize()
                ? Files.size(local) == remote.getSize()
                : false;
    }

    boolean testDecryptedSize(Path local, ICloud.MBSFile remote) throws IOException {
        return remote.hasAttributes() && remote.getAttributes().hasDecryptedSize()
                ? Files.size(local) == remote.getAttributes().getDecryptedSize()
                : false;
    }

    boolean testLastModified(Path local, ICloud.MBSFile remote) throws IOException {
        return remote.hasAttributes() && remote.getAttributes().hasLastModified()
                ? Files.getLastModifiedTime(local).equals(FileTime.from(remote.getAttributes().getLastModified(), TimeUnit.SECONDS))
                : false;
    }
}
