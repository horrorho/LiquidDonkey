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

import com.github.horrorho.liquiddonkey.cloud.store.Store;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBagTools;
import com.github.horrorho.liquiddonkey.printer.Level;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import com.github.horrorho.liquiddonkey.exception.FileErrorException;
import com.github.horrorho.liquiddonkey.iofunction.IOWriter;
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
 * Writes files.
 *
 * Writes out files from the specified {@link Store} and
 * {@link com.cain.donkeylooter.protobuf.ICloud.ChunkReference} lists.
 *
 * @author ahseya
 */
@NotThreadSafe
public final class LocalFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileWriter.class);

    /**
     * Returns a new instance.
     *
     * @param keyBagTools a KeyBagTools instance, not null
     * @param backupFolder the backup folder, not null
     * @param print the output for messages, not null
     * @param setLastModifiedTime true if last-modified timestamps should be set
     * @return a new instance, not null
     */
    public static LocalFileWriter newInstance(
            KeyBagTools keyBagTools,
            Directory backupFolder,
            Printer print,
            boolean setLastModifiedTime) {

        return new LocalFileWriter(LocalFileDecrypter.newInstance(), keyBagTools, backupFolder, print, setLastModifiedTime);
    }

    static LocalFileWriter newInstance(
            LocalFileDecrypter decrypter,
            KeyBagTools keyBagTools,
            Directory backupFolder,
            Printer print,
            boolean setLastModifiedTime) {

        return new LocalFileWriter(decrypter, keyBagTools, backupFolder, print, setLastModifiedTime);
    }

    private final LocalFileDecrypter decrypter;
    private final KeyBagTools keyBagTools;
    private final Directory backupFolder;
    private final Printer print;
    private final boolean setLastModifiedTime;

    LocalFileWriter(
            LocalFileDecrypter decrypter,
            KeyBagTools keyBagTools,
            Directory backupFolder,
            Printer print,
            boolean setLastModifiedTime) {

        this.decrypter = Objects.requireNonNull(decrypter);
        this.keyBagTools = Objects.requireNonNull(keyBagTools);
        this.backupFolder = Objects.requireNonNull(backupFolder);
        this.print = Objects.requireNonNull(print);
        this.setLastModifiedTime = setLastModifiedTime;
    }

    /**
     * Writes an empty file. Optionally set's the last-modified timestamp.
     *
     * @param snapshot the file's snapshot
     * @param file the file, not null
     * @throws FileErrorException
     */
    public void writeEmpty(int snapshot, MBSFile file) throws FileErrorException {
        if (file.hasSize() && file.getSize() != 0) {
            logger.warn("-- writeEmpty() > ignored, file is not empty: {} bytes", file.getSize());
        } else {
            write(snapshot, file, outputStream -> 0L);
        }
    }

    /**
     * Writes a file. If encrypted, attempts to decrypt the file. Optionally set's the last-modified timestamp.
     *
     * @param snapshot the file's snapshot
     * @param file the file, not null
     * @param writer the IOWriter, not null
     * @throws FileErrorException
     */
    public void write(int snapshot, MBSFile file, IOWriter writer) throws FileErrorException {
        try {
            Path path = backupFolder.path(snapshot, file);

            if (writeFile(path, writer) == -1) {
                logger.warn("-- write() > missing data: {}", file.getRelativePath());
                print.println(
                        Level.VV, "\t" + file.getDomain() + " " + file.getRelativePath() + " Failed. Missing data.");
                return;
            }

            if (file.hasAttributes() && file.getAttributes().hasEncryptionKey()) {
                decrypt(path, file);
            } else {
                logger.debug("-- write() > success: {}", file.getRelativePath());
                print.println(Level.VV, "\t" + file.getDomain() + " " + file.getRelativePath());
            }

            setLastModifiedTime(path, file);
        } catch (IOException ex) {
            throw new IllegalStateException("File io error", ex);
        }
    }

    void decrypt(Path path, MBSFile file) throws FileErrorException {
        ByteString key = keyBagTools.fileKey(file);
        if (key == null) {
            logger.warn("-- write() > failed to derive key: {}", file.getRelativePath());
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

    boolean exists(int snapshot, MBSFile file) {
        return Files.exists(backupFolder.path(snapshot, file));
    }

    void setLastModifiedTime(Path path, MBSFile file) throws IOException {
        if (setLastModifiedTime && Files.exists(path)) {
            // Default to 0 timestamp if non existant.
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
