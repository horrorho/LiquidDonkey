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
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBag;
import com.github.horrorho.liquiddonkey.cloud.store.Store;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBagTools;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.printer.Level;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import com.github.horrorho.liquiddonkey.settings.config.FileConfig;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes files.
 *
 * Writes out files from the specified {@link Store} and {@link com.cain.donkeylooter.protobuf.ICloud.ChunkReference}
 * lists.
 *
 * @author ahseya
 */
@NotThreadSafe
public final class SignatureWriter {

    private static final Logger logger = LoggerFactory.getLogger(SignatureWriter.class);

    /**
     * Returns a new instance.
     *
     * @param snapshot not null
     * @param fileConfig not null
     * @param printer not null
     * @return a new instance, not null
     */
    // TODO rework this
    public static SignatureWriter from(
            Snapshot snapshot,
            FileConfig fileConfig,
            Printer printer) {

        return new SignatureWriter(
                snapshot.signatures(),
                FileDecrypter.newInstance(),
                KeyBagTools.newInstance(snapshot.backup().keybag()),
                SnapshotDirectory.from(snapshot, fileConfig),
                printer,
                fileConfig.setLastModifiedTimestamp());
    }

    private final Map<ByteString, Set<ICloud.MBSFile>> signatureToFileSet;
    private final FileDecrypter decrypter;
    private final KeyBagTools keyBagTools;
    private final SnapshotDirectory directory;
    private final Printer print;
    private final boolean setLastModifiedTime;

    SignatureWriter(
            Map<ByteString, Set<ICloud.MBSFile>> signatureToFile,
            FileDecrypter decrypter,
            KeyBagTools keyBagTools,
            SnapshotDirectory directory,
            Printer print,
            boolean setLastModifiedTime) {

        this.signatureToFileSet = Objects.requireNonNull(signatureToFile);
        this.decrypter = Objects.requireNonNull(decrypter);
        this.keyBagTools = Objects.requireNonNull(keyBagTools);
        this.directory = Objects.requireNonNull(directory);
        this.print = Objects.requireNonNull(print);
        this.setLastModifiedTime = setLastModifiedTime;
    }

    /**
     * Writes an empty file. Optionally set's the last-modified timestamp.
     *
     * @param file the file, not null
     * @throws IOException
     */
    public void writeEmpty(MBSFile file) throws IOException {
        if (file.hasSize() && file.getSize() != 0) {
            logger.warn("-- writeEmpty() > ignored, file is not empty: {} bytes", file.getSize());
            return;
        }

        doWrite(file, outputStream -> 0L);
    }

    /**
     * Writes the files referenced by the specified signature.
     * <p>
     * If encrypted, attempts to decrypt the file. Optionally set's the last-modified timestamp.
     *
     * @param signature not null
     * @param writer not null
     * @return bytes written
     * @throws IOException
     */
    public long write(ByteString signature, IOFunction<OutputStream, Long> writer) throws IOException {
        logger.trace("<< write() < signature: {}", Bytes.hex(signature));
        Set<ICloud.MBSFile> files = signatureToFileSet.get(signature);
        if (files == null) {
            logger.warn("-- write() >> bad state, unknown signature: {}", Bytes.hex(signature));
            return 0;
        }

        long total = 0;
        for (ICloud.MBSFile file : files) {
            total += doWrite(file, writer);
        }

        if (signatureToFileSet.remove(signature) == null) {
            logger.warn("-- write() >> bad state, concurrent removal: {}", Bytes.hex(signature));
        }

        logger.trace(">> write() > bytes written: {}", total);
        return total;
    }

    long doWrite(ICloud.MBSFile file, IOFunction<OutputStream, Long> writer) throws IOException {
        logger.trace("<< doWrite() < file: {}", file.getRelativePath());

        Path path = directory.apply(file);

        long written = writeFile(path, writer);
        logger.debug("-- doWrite() > path: {} written: {}", path, written);

        if (file.hasAttributes() && file.getAttributes().hasEncryptionKey()) {
            decrypt(path, file);
        } else {
            logger.debug("-- doWrite() > success: {}", file.getRelativePath());
            print.println(Level.VV, "\t" + file.getDomain() + " " + file.getRelativePath());
        }

        setLastModifiedTime(path, file);

        logger.trace(">> doWrite() > written: {}", written);
        return written;
    }

    void decrypt(Path path, MBSFile file) throws IOException {
        ByteString key = keyBagTools.fileKey(file);
        if (key == null) {
            logger.warn("-- decrypt() > failed to derive key: {}", file.getRelativePath());
            print.println(Level.VV, "\t" + file.getDomain() + " " + file.getRelativePath() + " Failed. No key.");
            return;
        }

        try {
            decrypter.decrypt(path, key, file.getAttributes().getDecryptedSize());
            logger.debug("-- decrypt() > success: {}", file.getRelativePath());
            print.println(Level.VV, "\t" + file.getDomain() + " " + file.getRelativePath() + " Decrypted.");
        } catch (BadDataException ex) {
            logger.warn("-- decrypt() > failed: {} exception: {}", file.getRelativePath(), ex);
            print.println(Level.VV, "\t" + file.getDomain() + " " + file.getRelativePath() + " Failed. Decrypt error.");
        }
    }

    long writeFile(Path path, IOFunction<OutputStream, Long> writer) throws IOException {
        Files.createDirectories(path.getParent());

        try (OutputStream output = Files.newOutputStream(path, CREATE, WRITE, TRUNCATE_EXISTING)) {
            return writer.apply(output);
        }
    }

    boolean exists(MBSFile file) {
        return Files.exists(directory.apply(file));
    }

    void setLastModifiedTime(Path path, MBSFile file) throws IOException {
        if (setLastModifiedTime && Files.exists(path)) {
            // Default to 0 timestamp if non existant.
            // TODO simplify
            long lastModifiedTimestamp = file.hasAttributes() && file.getAttributes().hasLastModified()
                    ? file.getAttributes().getLastModified()
                    : 0;

            Files.setLastModifiedTime(
                    path,
                    FileTime.from(
                            lastModifiedTimestamp,
                            TimeUnit.SECONDS));
        }
    }
}
// TODO simply protobufs
// Path checks
