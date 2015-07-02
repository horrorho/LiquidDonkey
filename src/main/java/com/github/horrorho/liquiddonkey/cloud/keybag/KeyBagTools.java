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
package com.github.horrorho.liquiddonkey.cloud.keybag;

import com.github.horrorho.liquiddonkey.crypto.AESWrap;
import com.github.horrorho.liquiddonkey.crypto.Curve25519;
import com.github.horrorho.liquiddonkey.crypto.MessageDigestFactory;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.security.MessageDigest;
import java.util.Objects;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KeyBag utility class.
 *
 * @author ahseya
 */
@NotThreadSafe
public final class KeyBagTools {

    private static final Logger logger = LoggerFactory.getLogger(KeyBagTools.class);

    /**
     * Returns a new instance.
     *
     * @param keyBag, not null
     * @return a new instance, not null
     */
    public static KeyBagTools newInstance(KeyBag keyBag) {
        return new KeyBagTools(keyBag);
    }

    private final AESWrap aesWrap = AESWrap.newInstance();
    private final MessageDigest sha256 = MessageDigestFactory.SHA256();
    private final KeyBag keyBag;

    private KeyBagTools(KeyBag keyBag) {
        this.keyBag = Objects.requireNonNull(keyBag);
    }

    /**
     * Returns the file key for the given file or null if unavailable.
     *
     * @param file the file
     * @return the file key for the given file or null if unavailable
     */
    public ByteString fileKey(ICloud.MBSFile file) {
        ICloud.MBSFileAttributes attributes = file.getAttributes();
        ByteString key = attributes.getEncryptionKey();
        int protectionClass = key.substring(0x18, 0x1C).asReadOnlyByteBuffer().getInt();

        if (protectionClass != attributes.getProtectionClass()) {
            logger.warn("-- fileKey() > mismatched file/ key protection class: {}/{}, file: {}",
                    attributes.getProtectionClass(), protectionClass, file.getRelativePath());
            return null;
        }

        ByteString wrappedKey = null;
        ByteString fileKey = null;

        if (attributes.hasEncryptionKeyVersion() && attributes.getEncryptionKeyVersion() == 2) {
            if (keyBag.uuid().equals(key.substring(0, 0x10))) {
                int keyLength = key.substring(0x20, 0x24).asReadOnlyByteBuffer().getInt();
                if (keyLength == 0x48) {
                    wrappedKey = key.substring(0x24);
                } else {
                    logger.warn("-- fileKey() > expected unwrapped key length 72, got: {}, file: {}",
                            keyLength, file.getRelativePath());
                }
            } else {
                logger.warn("-- fileKey() > mismatched file/ keybag UUIDs: {}/{}, file: {}",
                        Bytes.hex(key.substring(0, 0x10)), Bytes.hex(keyBag.uuid()), file.getRelativePath());
            }
        } else {
            wrappedKey = key.substring(0x1c, key.size());
        }

        if (wrappedKey != null) {
            fileKey = unwrapCurve25519(protectionClass, wrappedKey);
        }

        if (fileKey == null) {
            logger.warn("-- fileKey() > failed to unwrap key: {}, file: {}", Bytes.hex(key), file.getRelativePath());
        }

        return fileKey;
    }

    ByteString unwrapCurve25519(int protectionClass, ByteString key) {
        if (key.size() != 0x48) {
            logger.warn("-- unwrapCurve25519() > bad key length: {}", Bytes.hex(key));
            return null;
        }

        byte[] myPrivateKey = keyBag.classKey(protectionClass, "KEY").toByteArray();
        if (myPrivateKey == null) {
            logger.warn("-- unwrapCurve25519() > no KEY key for protection class: {}", protectionClass);
            return null;
        }

        byte[] myPublicKey = keyBag.classKey(protectionClass, "PBKY").toByteArray();
        if (myPublicKey == null) {
            logger.warn("-- unwrapCurve25519() > no PBKY key for protection class: {}", protectionClass);
            return null;
        }

        byte[] otherPublicKey = key.substring(0, 32).toByteArray();
        byte[] shared = Curve25519.getInstance().agreement(otherPublicKey, myPrivateKey);

        sha256.reset();
        sha256.update(new byte[]{0x00, 0x00, 0x00, 0x01});
        sha256.update(shared);
        sha256.update(otherPublicKey);
        sha256.update(myPublicKey);
        byte[] hash = sha256.digest();

        try {
            return ByteString.copyFrom(aesWrap.unwrap(hash, key.substring(0x20, key.size()).toByteArray()));
        } catch (IllegalStateException | InvalidCipherTextException ex) {
            logger.warn("-- unwrapCurve25519() > failed to unwrap key: {} protection class: {} exception: {}",
                    Bytes.hex(key), protectionClass, ex);
            return null;
        }
    }
}
