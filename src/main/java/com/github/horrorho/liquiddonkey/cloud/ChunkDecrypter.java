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
package com.github.horrorho.liquiddonkey.cloud;

import com.github.horrorho.liquiddonkey.crypto.MessageDigestFactory;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chunk decrypter.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class ChunkDecrypter {

    /**
     * Returns a new instance.
     *
     * @return a new instance, not null
     */
    public static ChunkDecrypter newInstance() {
        return new ChunkDecrypter(
                new CFBBlockCipher(new AESEngine(), 128),
                MessageDigestFactory.getInstance().SHA256());
    }

    private static final Logger logger = LoggerFactory.getLogger(ChunkDecrypter.class);

    private final StreamBlockCipher cfbAes;
    private final MessageDigest chunkDigest;

    ChunkDecrypter(StreamBlockCipher cfbAes, MessageDigest chunkDigest) {
        this.cfbAes = Objects.requireNonNull(cfbAes);
        this.chunkDigest = Objects.requireNonNull(chunkDigest);
    }

    /**
     * Decrypts chunk data.
     *
     * @param chunks the chunk info list, not null
     * @param data the chunk data, not null
     * @return the decrypted chunk list or null if an error occurred
     */
    // TODO redo to exception
    public List<byte[]> decrypt(ChunkServer.StorageHostChunkList chunks, byte[] data) {
        List<byte[]> decrypted = new ArrayList<>();
        int offset = 0;
        for (ChunkServer.ChunkInfo chunkInfo : chunks.getChunkInfoList()) {
            byte[] decryptedChunk = decrypt(chunkInfo, data, offset);
            if (decryptedChunk == null) {
                return null;
            }
            decrypted.add(decryptedChunk);
            offset += chunkInfo.getChunkLength();
        }

        return decrypted;
    }

    byte[] decrypt(ChunkServer.ChunkInfo chunkInfo, byte[] data, int offset) {
        try {
            if (!chunkInfo.hasChunkEncryptionKey()) {
                logger.warn("-- decrypt() > missing key.");
                return null;
            }

            if (keyType(chunkInfo) != 1) {
                logger.warn("-- decrypt() >  unhandled key type: {} value: {}", keyType(chunkInfo));
                return null;
            }

            byte[] decrypted = decryptCfbAes(chunkInfo, data, offset);

            if (chunkInfo.hasChunkChecksum()) {
                if (!checksum(chunkInfo).equals(checksum(decrypted))) {
                    logger.warn("-- decrypt() >  checksum failed: {} expected: {}",
                            Bytes.hex(checksum(decrypted)), Bytes.hex(checksum(chunkInfo)));
                    return null;
                }
            } else {
                logger.warn("-- decrypt() >  missing chunk info checksum, unable to verify data integrity");
            }

            return decrypted;
        } catch (DataLengthException | ArrayIndexOutOfBoundsException | NullPointerException ex) {

            logger.warn("-- decrypt() > exception: ", ex);
            return null;
        }
    }

    byte[] decryptCfbAes(ChunkServer.ChunkInfo chunkInfo, byte[] data, int offset) {
        // TOFIX input buffer too small exception, are we truncating too early somewhere?
        cfbAes.init(false, key(chunkInfo));
        byte[] decrypted = new byte[chunkInfo.getChunkLength()];
        cfbAes.processBytes(data, offset, chunkInfo.getChunkLength(), decrypted, 0);
        return decrypted;
    }

    ByteString checksum(ChunkServer.ChunkInfo chunkInfo) {
        return chunkInfo.getChunkChecksum().substring(1);
    }

    KeyParameter key(ChunkServer.ChunkInfo chunkInfo) {
        return new KeyParameter(chunkInfo.getChunkEncryptionKey().substring(1).toByteArray());
    }

    Byte keyType(ChunkServer.ChunkInfo chunkInfo) {
        return chunkInfo.getChunkEncryptionKey().substring(0, 1).byteAt(0);
    }

    ByteString checksum(byte[] data) {
        chunkDigest.reset();
        byte[] hash = chunkDigest.digest(data);
        byte[] hashhash = chunkDigest.digest(hash);
        return ByteString.copyFrom(hashhash).substring(0, 20);
    }
}
