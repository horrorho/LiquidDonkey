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
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import java.security.MessageDigest;
import java.util.Arrays;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decrypts encrypted files.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class FileDecrypter {

    private static final Logger logger = LoggerFactory.getLogger(FileDecrypter.class);

    /**
     * Returns a new instance.
     *
     * @return a new instance, not null
     */
    public static FileDecrypter create() {
        return FileDecrypter.create(
                new BufferedBlockCipher(new CBCBlockCipher(new AESEngine())),
                MessageDigestFactory.getInstance().SHA1());
    }

    static FileDecrypter create(BufferedBlockCipher cbcAes, MessageDigest sha1) {
        return new FileDecrypter(cbcAes, sha1);
    }

    private final BufferedBlockCipher cbcAes;
    private final MessageDigest sha1;
    private final byte[] in = new byte[0x1000];
    private final byte[] out = new byte[0x1000];

    FileDecrypter(BufferedBlockCipher cbcAes, MessageDigest sha1) {
        this.cbcAes = cbcAes;
        this.sha1 = sha1;
    }

    /**
     * Decrypts a file.
     * <p>
     * Decrypts the specified file. The file is temporarily renamed with a .encrypted suffix. The un-encrypted data is
     * written to a fresh file of the same name. The temporary file is deleted. File timestamps are altered.
     * <p>
     * In the presence of exceptions the temporary file may remain undeleted, the original or the un-encrypted file may
     * not exist.
     * <p>
     * iOS 5 files remain untested.
     *
     * @param path the Path to the file
     * @param key the file key
     * @param decryptedSize the expected decrypted size, a value of 0 indicates iOS 5 format
     * @throws BadDataException if a cipher exception occurred
     * @throws IOException
     */
    public void decrypt(Path path, ByteString key, long decryptedSize) throws BadDataException, IOException {
        Path encrypted = null;
        try {
            if (Files.size(path) == 0) {
                logger.warn("--decrypt() > cannot decrypt an empty file: {}", path);
                return;
            }

            ParametersWithIV ivKey = deriveIvKey(key);
            KeyParameter fileKey = deriveFileKey(key);

            long blockCount = Files.size(path) + (decryptedSize > 0 ? 0x0FFF : 0) >> 12;

            encrypted = path.getParent().resolve(path.getFileName() + ".encrypted");
            Files.move(path, encrypted, StandardCopyOption.REPLACE_EXISTING);

            try (InputStream input = Files.newInputStream(encrypted, READ);
                    OutputStream output = Files.newOutputStream(path, CREATE, WRITE, TRUNCATE_EXISTING)) {

                byte[] checksum = decrypt(input, output, blockCount, ivKey, fileKey);

                if (decryptedSize == 0) {
                    // iOS 5
                    decryptedSize = trailer(input, checksum);

                    if (decryptedSize == -1) {
                        logger.warn("-- decrypt() > bad trailer/ checksum");
                    }
                }
            }

            long size = Files.size(path);
            if (decryptedSize > 0 && size > decryptedSize) {
                logger.debug("-- decrypt() > truncating to: {} from: {}", decryptedSize, size);
                Files.newByteChannel(path, WRITE).truncate(decryptedSize).close();
            } else if (Files.size(path) < decryptedSize) {
                logger.warn("-- decrypt() > short output size: {} expected: {}", Files.size(path), decryptedSize);
            }
        } catch (BufferUnderflowException | DataLengthException ex) {
            throw new BadDataException("Cipher exception", ex);
        } finally {
            if (encrypted != null) {
                try {
                    Files.deleteIfExists(encrypted);
                } catch (IOException ex) {
                    logger.warn("-- decrypt() > unable to deleted temporary encrypted file: ", ex);
                }
            }
        }
    }

    byte[] decrypt(
            InputStream input,
            OutputStream output,
            long blockCount,
            ParametersWithIV ivKey,
            KeyParameter fileKey) throws IOException {

        sha1.reset();
        for (int block = 0; block < blockCount; block++) {
            int length = input.read(in);
            if (length == -1) {
                logger.warn("-- decrypt() > empty block");
                break;
            }
            sha1.update(in, 0, length);
            decryptBlock(fileKey, deriveIv(ivKey, block), in, length, out);
            output.write(out, 0, length);
        }

        return sha1.digest();
    }

    void decryptBlock(KeyParameter fileKey, byte[] iv, byte[] in, int length, byte[] out) {
        cbcAes.init(false, new ParametersWithIV(fileKey, iv));
        cbcAes.processBytes(in, 0, length, out, 0);
    }

    long trailer(InputStream input, byte[] checksum) throws IOException {
        byte[] trailer = new byte[0x1C];
        int length = input.read(trailer);
        if (length == -1) {
            logger.warn("-- trailer() > missing trailer");
            return -1;
        }

        ByteBuffer buffer = ByteBuffer.wrap(trailer);
        long decryptedSize = buffer.getLong();
        ByteBuffer expectedChecksum = buffer.slice();

        if (!ByteBuffer.wrap(checksum).equals(expectedChecksum)) {
            logger.warn("-- trailer() - bad checksum");
            return -1;
        }

        return decryptedSize;
    }

    byte[] deriveIv(ParametersWithIV ivKey, int block) {
        byte[] blockHash = blockHash(block);
        byte[] iv = new byte[0x10];
        cbcAes.init(true, ivKey);
        cbcAes.processBytes(blockHash, 0, blockHash.length, iv, 0);
        return iv;
    }

    ParametersWithIV deriveIvKey(ByteString key) {
        sha1.reset();
        return new ParametersWithIV(
                new KeyParameter(Arrays.copyOfRange(sha1.digest(key.toByteArray()), 0, 16)),
                new byte[16]);
    }

    KeyParameter deriveFileKey(ByteString key) {
        return new KeyParameter(key.toByteArray());
    }

    byte[] blockHash(int block) {
        int offset = block << 12;
        byte[] hash = new byte[0x10];
        ByteBuffer buffer = ByteBuffer.wrap(hash);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < 4; i++) {
            offset = ((offset & 1) == 1)
                    ? 0x80000061 ^ (offset >>> 1)
                    : offset >>> 1;
            buffer.putInt(offset);
        }

        return hash;
    }
}
