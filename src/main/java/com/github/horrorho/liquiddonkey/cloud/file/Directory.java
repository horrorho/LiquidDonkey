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

import com.github.horrorho.liquiddonkey.crypto.MessageDigestFactory;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.settings.config.DirectoryConfig;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Objects;
import net.jcip.annotations.NotThreadSafe;

/**
 * Directory helper.
 * <p>
 * Handles the standard and iTunes-flat directory structures, either separating or combining snapshots.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class Directory {

    public static Directory newInstance(ByteString backupUdid, DirectoryConfig config) {
        return Directory.newInstance(
                config.base().resolve(Bytes.hex(backupUdid)),
                config.isFlat(),
                config.isCombined());
    }

    /**
     * Returns a new Directory instance.
     *
     * @param base the base output folder, not null
     * @param isFlat the iTunes-flat structure switch
     * @param isCombined the combine snapshots switch
     * @return new IPath instance
     * @throws NullPointerException if backupUuid or baseFolder are null
     */
    public static Directory newInstance(Path base, boolean isFlat, boolean isCombined) {
        return Directory.newInstance(MessageDigestFactory.SHA1(), base, isFlat, isCombined);
    }

    static Directory newInstance(MessageDigest messageDigest, Path base, boolean isFlat, boolean isCombined) {
        return new Directory(messageDigest, base, isFlat, isCombined);
    }

    private static final String FILTER = "[:|*<>?\"]";
    private static final String REPLACE = "_";
    private static final ByteString HYPHEN = ByteString.copyFromUtf8("-");

    private final MessageDigest messageDigest;
    private final Path base;
    private final boolean isFlat;
    private final boolean isCombined;

    Directory(MessageDigest messageDigest, Path base, boolean isFlat, boolean isCombined) {
        this.messageDigest = Objects.requireNonNull(messageDigest);
        this.base = Objects.requireNonNull(base);
        this.isFlat = isFlat;
        this.isCombined = isCombined;
    }

    /**
     * Returns the local path for the specified file.
     *
     * @param snapshot the snapshot number
     * @param file the file, not null
     * @return the local path for the specified file
     * @throws NullPointerException if the file argument is null
     */
    public Path path(int snapshot, ICloud.MBSFile file) {
        Path path;
        if (isFlat) {
            String hash = Bytes.hex(
                    messageDigest.digest(file.getDomainBytes()
                            .concat(HYPHEN)
                            .concat(file.getRelativePathBytes())
                            .toByteArray()));

            path = isCombined
                    ? base.resolve(hash)
                    : base.resolve("snapshot_" + snapshot).resolve(hash);
        } else {
            String domain = clean(file.getDomain());
            path = isCombined
                    ? base.resolve(domain).resolve(file.getRelativePath())
                    : base.resolve("snapshot_" + snapshot).resolve(domain).resolve(file.getRelativePath());
        }
        return path;
    }

    static String clean(String string) {
        return string.replaceAll(FILTER, REPLACE);
    }
}
