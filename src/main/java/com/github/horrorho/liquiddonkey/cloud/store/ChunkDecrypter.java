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
package com.github.horrorho.liquiddonkey.cloud.store;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.SHA256Digest;
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
    public static ChunkDecrypter create() {
        return new ChunkDecrypter(
                new CFBBlockCipher(new AESEngine(), 128),
                new SHA256Digest());
    }

    private static final Logger logger = LoggerFactory.getLogger(ChunkDecrypter.class);

    private final StreamBlockCipher cfbAes;
    private final GeneralDigest digest;

    ChunkDecrypter(StreamBlockCipher cfbAes, GeneralDigest digest) {
        this.cfbAes = Objects.requireNonNull(cfbAes);
        this.digest = Objects.requireNonNull(digest);
    }

    /**
     * Decrypts chunk data.
     *
     * @param chunkList the chunk info list, not null
     * @param data the chunk data, not null
     * @return the decrypted chunk list, not null
     * @throws BadDataException if a decryption error occurs
     */
    public List<byte[]> decrypt(ChunkServer.StorageHostChunkList chunkList, byte[] data) throws BadDataException {
        List<byte[]> decrypted = new ArrayList<>();
        int offset = 0;
        for (ChunkServer.ChunkInfo chunkInfo : chunkList.getChunkInfoList()) {
            byte[] decryptedChunk = decrypt(chunkInfo, data, offset);
            decrypted.add(decryptedChunk);
            offset += chunkInfo.getChunkLength();
        }

        return decrypted;
    }

    byte[] decrypt(ChunkServer.ChunkInfo chunkInfo, byte[] data, int offset) throws BadDataException {
        try {
            if (!chunkInfo.hasChunkEncryptionKey()) {
                throw new BadDataException("Missing key");
            }

            if (keyType(chunkInfo) != 1) {
                throw new BadDataException("Unknown key type: " + keyType(chunkInfo));
            }

            byte[] decrypted = decryptCfbAes(chunkInfo, data, offset);

            if (chunkInfo.hasChunkChecksum()) {
                if (!checksum(chunkInfo).equals(checksum(decrypted))) {
                    logger.debug("-- decrypt() >  checksum failed: {} expected: {}",
                            Bytes.hex(checksum(decrypted)), Bytes.hex(checksum(chunkInfo)));
                    throw new BadDataException("Decrypt bad checksum");
                }
            } else {
                logger.warn("-- decrypt() >  missing chunk info checksum, unable to verify data integrity");
            }

            return decrypted;
        } catch (DataLengthException | ArrayIndexOutOfBoundsException | NullPointerException ex) {
            throw new BadDataException("Decrypt failed", ex);
        }
    }

    byte[] decryptCfbAes(ChunkServer.ChunkInfo chunkInfo, byte[] data, int offset) {
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

    byte keyType(ChunkServer.ChunkInfo chunkInfo) {
        return chunkInfo.getChunkEncryptionKey().substring(0, 1).byteAt(0);
    }

    ByteString checksum(byte[] data) {
        byte[] hash = new byte[digest.getDigestSize()];
        byte[] hashHash = new byte[digest.getDigestSize()];

        digest.reset();
        digest.update(data, 0, data.length);
        digest.doFinal(hash, 0);
        digest.update(hash, 0, hash.length);
        digest.doFinal(hashHash, 0);

        return ByteString.copyFrom(hashHash).substring(0, 20);
    }
}
