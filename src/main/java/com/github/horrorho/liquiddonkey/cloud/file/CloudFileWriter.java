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

import com.github.horrorho.liquiddonkey.cloud.outcome.Outcome;
import com.github.horrorho.liquiddonkey.cloud.data.Snapshot;
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBagManager;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CloudFileWriter.
 * <p>
 * Writes out the {@link ICloud.MBSFile}/s referenced by signatures. Locking single threaded mode from action.
 *
 * @author ahseya
 */
@NotThreadSafe
public final class CloudFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(CloudFileWriter.class);

    /**
     * Returns a new instance.
     *
     * @param snapshot not null
     * @param fileConfig not null
     * @return a new instance, not null
     */
    public static CloudFileWriter from(Snapshot snapshot, FileConfig fileConfig) {
        return new CloudFileWriter(
                FileDecrypter.create(),
                snapshot.keyBagManager(),
                SnapshotDirectory.from(snapshot, fileConfig),
                fileConfig.setLastModifiedTimestamp());
    }

    private final FileDecrypter decrypter;
    private final KeyBagManager keyBag;
    private final SnapshotDirectory directory;
    private final boolean setLastModifiedTime;

    CloudFileWriter(
            FileDecrypter decrypter,
            KeyBagManager keyBagTools,
            SnapshotDirectory directory,
            boolean setLastModifiedTime) {

        this.decrypter = Objects.requireNonNull(decrypter);
        this.keyBag = Objects.requireNonNull(keyBagTools);
        this.directory = Objects.requireNonNull(directory);
        this.setLastModifiedTime = setLastModifiedTime;
    }

    /**
     * Writes an empty file.
     * <p>
     * Optionally sets the last-modified timestamp.
     *
     * @param file the file, not null
     * @throws IOException
     */
    public void writeEmpty(MBSFile file) throws IOException {
        if (file.hasSize() && file.getSize() != 0) {
            logger.warn("-- writeEmpty() > bad state, file is not empty: {} bytes", file.getSize());
            return;
        }

        write(file, outputStream -> {
            outputStream.write(new byte[]{});
            return 0L;
        });
    }

    /**
     * Writes the specified file.
     * <p>
     * If encrypted, attempts to decrypt the file. Optionally sets the last-modified timestamp.
     *
     * @param file not null
     * @param writer not null
     * @return map from ICloud.MBSFile to Outcome/s, or null if the signature doesn't reference any files
     * @throws IOException
     * @throws IllegalStateException if the signature is unknown
     */
    public Outcome write(ICloud.MBSFile file, IOFunction<OutputStream, Long> writer) throws IOException {
        logger.trace("<< write() < file: {}", file.getRelativePath());

        Path path = directory.apply(file);

        long written = createDirectoryWriteFile(path, writer);
        logger.debug("-- write() > path: {} written: {}", path, written);

        Outcome result;

        if (file.getAttributes().hasEncryptionKey()) {
            result = decrypt(path, file);
        } else {
            logger.debug("-- write() > success: {}", file.getRelativePath());
            result = Outcome.WRITTEN;
        }

        if (setLastModifiedTime) {
            setLastModifiedTime(path, file);
        }

        logger.trace(">> write() > file: {} result: {}", file.getRelativePath(), result);
        return result;
    }

    Outcome decrypt(Path path, MBSFile file) throws IOException {
        ByteString key = keyBag.fileKey(file);

        if (key == null) {
            logger.warn("-- decrypt() > failed to derive key: {}", file.getRelativePath());
            return Outcome.FAILED_DECRYPT_NO_KEY;
        }

        if (!Files.exists(path)) {
            logger.warn("-- decrypt() > no such file: {}", file.getRelativePath());
            return Outcome.FAILED_DECRYPT_NO_FILE;
        }

        try {
            decrypter.decrypt(path, key, file.getAttributes().getDecryptedSize());
            logger.debug("-- decrypt() > success: {}", file.getRelativePath());
            return Outcome.WRITTEN_DECRYPT;

        } catch (BadDataException ex) {
            logger.warn("-- decrypt() > failed: {} exception: {}", file.getRelativePath(), ex);
            return Outcome.FAILED_DECRYPT_ERROR;
        }
    }

    long createDirectoryWriteFile(Path path, IOFunction<OutputStream, Long> writer) throws IOException {
        Files.createDirectories(path.getParent());

        try (OutputStream output = Files.newOutputStream(path, CREATE, WRITE, TRUNCATE_EXISTING)) {
            return writer.apply(output);
        }
    }

    void setLastModifiedTime(Path path, MBSFile file) throws IOException {
        if (Files.exists(path)) {
            long lastModifiedTimestamp = file.getAttributes().getLastModified();
            FileTime fileTime = FileTime.from(lastModifiedTimestamp, TimeUnit.SECONDS);
            Files.setLastModifiedTime(path, fileTime);
        }
    }
}
