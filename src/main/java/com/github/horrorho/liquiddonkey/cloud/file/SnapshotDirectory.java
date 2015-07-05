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
import com.github.horrorho.liquiddonkey.crypto.MessageDigestFactory;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.function.Function;
import net.jcip.annotations.NotThreadSafe;

/**
 * SnapshotDirectory.
 * <p>
 * Handles the standard and iTunes-flat directory structures, either separating or combining snapshots.
 *
 * @author Ahseya
 */
@NotThreadSafe
public abstract class SnapshotDirectory implements Function<ICloud.MBSFile, Path> {

    /**
     * Returns a new instance.
     *
     * @param snapshot not null
     * @param config not null
     * @return new instance, not null
     */
    public static SnapshotDirectory from(Snapshot snapshot, FileConfig config) {
        return SnapshotDirectory.newInstance(
                config.base(),
                snapshot.backup().udidString(),
                Integer.toString(snapshot.id()),
                config.isFlat(),
                config.isCombined());
    }

    static SnapshotDirectory newInstance(
            Path base,
            String udidStr,
            String snapshotIdStr,
            boolean isFlat,
            boolean isCombined) {

        MessageDigest digest = MessageDigestFactory.getInstance().SHA1();

        Path folder = isCombined
                ? base.resolve(udidStr)
                : base.resolve(udidStr).resolve(snapshotIdStr);

        return isFlat
                ? new FlatSnapshotDirectory(folder, digest)
                : new NonFlatSnapshotDirectory(folder, digest);
    }

    private static final String FILTER = "[:|*<>?\"].";
    private static final String REPLACE = "_";
    private static final ByteString HYPHEN = ByteString.copyFromUtf8("-");

    private final Path folder;
    private final MessageDigest messageDigest;

    SnapshotDirectory(Path folder, MessageDigest messageDigest) {
        this.folder = folder;
        this.messageDigest = messageDigest;
    }

    String clean(String string) {
        return string.replaceAll(FILTER, REPLACE);
    }

    @NotThreadSafe
    public static final class NonFlatSnapshotDirectory extends SnapshotDirectory {

        NonFlatSnapshotDirectory(Path folder, MessageDigest messageDigest) {
            super(folder, messageDigest);
        }

        @Override
        public Path apply(ICloud.MBSFile file) {
            return super.folder
                    .resolve(clean(file.getDomain()))
                    .resolve(file.getRelativePath());
        }
    }

    @NotThreadSafe
    public static final class FlatSnapshotDirectory extends SnapshotDirectory {

        FlatSnapshotDirectory(Path folder, MessageDigest messageDigest) {
            super(folder, messageDigest);
        }

        @Override
        public Path apply(ICloud.MBSFile file) {
            byte[] hash = super.messageDigest.digest(
                    file.getDomainBytes()
                    .concat(HYPHEN)
                    .concat(file.getRelativePathBytes()).toByteArray());
            return super.folder.resolve(Bytes.hex(hash));
        }
    }
}
