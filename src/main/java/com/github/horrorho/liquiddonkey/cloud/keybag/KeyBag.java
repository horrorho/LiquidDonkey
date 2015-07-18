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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.crypto.AESWrap;
import com.github.horrorho.liquiddonkey.crypto.Curve25519;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keybag. Lightweight key bag implementation.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class KeyBag {

    /**
     * Returns a new unlocked KeyBag instance.
     *
     * @param keySet the key set, not null
     * @return a new unlocked KeyBag instance, not null
     * @throws BadDataException if the KeyBag cannot be unlocked or a data handling error occurred
     */
    public static KeyBag from(ICloud.MBSKeySet keySet) throws BadDataException {
        logger.trace("<< from()");

        KeyBagAssistant assistant = KeyBagAssistant.from(keySet);

        KeyBag keyBag = new KeyBag(
                assistant.copyClassKeys(),
                assistant.copyAttributes(),
                assistant.uuid(),
                assistant.type());

        logger.trace(">> from() > {}", keyBag);
        return keyBag;
    }

    private static final Logger logger = LoggerFactory.getLogger(KeyBag.class);

    private final Map<Integer, Map<String, ByteString>> classKeys;
    private final Map<String, ByteString> attributes;
    private final ByteString uuid;
    private final KeyBagType type;
    private final Supplier<AESWrap> aesWraps;
    private final Supplier<SHA256Digest> sha256s;

    KeyBag(
            Map<Integer, Map<String, ByteString>> classKeys,
            Map<String, ByteString> attributes,
            ByteString uuid,
            KeyBagType type,
            Supplier<AESWrap> aesWraps,
            Supplier<SHA256Digest> sha256s) {

        this.attributes = new HashMap<>(attributes);
        this.uuid = Objects.requireNonNull(uuid);
        this.type = Objects.requireNonNull(type);

        // Deep copy
        this.classKeys = classKeys.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashMap<>(entry.getValue())));

        this.aesWraps = aesWraps;
        this.sha256s = sha256s;
    }

    KeyBag(
            Map<Integer, Map<String, ByteString>> classKeys,
            Map<String, ByteString> attributes,
            ByteString uuid,
            KeyBagType type) {

        this(classKeys, attributes, uuid, type, AESWrap::create, SHA256Digest::new);
    }

    /**
     * Returns the file key for the given file or null if unavailable.
     *
     * @param file the file
     * @return the file key for the given file or null if unavailable
     */
    public ByteString fileKey(ICloud.MBSFile file) {
        return fileKey(file, aesWraps.get(), sha256s.get());
    }

    ByteString fileKey(ICloud.MBSFile file, AESWrap aesWrap, SHA256Digest sha256) {
        ICloud.MBSFileAttributes fileAttributes = file.getAttributes();

        if (!fileAttributes.hasEncryptionKey()) {
            logger.warn("-- file() > no encryption key: {}", file.getRelativePath());
            return null;
        }

        ByteString key = fileAttributes.getEncryptionKey();
        int protectionClass = key.substring(0x18, 0x1C).asReadOnlyByteBuffer().getInt();

        if (protectionClass != fileAttributes.getProtectionClass()) {
            logger.warn("-- fileKey() > mismatched file/ key protection class: {}/{} file: {}",
                    fileAttributes.getProtectionClass(), protectionClass, file.getRelativePath());
            //return null;
        }

        ByteString wrappedKey = null;
        ByteString fileKey = null;

        if (fileAttributes.hasEncryptionKeyVersion() && fileAttributes.getEncryptionKeyVersion() == 2) {
            if (uuid.equals(key.substring(0, 0x10))) {
                int keyLength = key.substring(0x20, 0x24).asReadOnlyByteBuffer().getInt();
                if (keyLength == 0x48) {
                    wrappedKey = key.substring(0x24);
                } else {
                    logger.warn("-- fileKey() > expected unwrapped key length 72, got: {} file: {}",
                            keyLength, file.getRelativePath());
                }
            } else {
                logger.warn("-- fileKey() > mismatched file/ keybag UUIDs: {}/{} file: {}",
                        Bytes.hex(key.substring(0, 0x10)), Bytes.hex(uuid), file.getRelativePath());
            }
        } else {
            wrappedKey = key.substring(0x1c, key.size());
        }

        if (wrappedKey != null) {
            fileKey = unwrapCurve25519(protectionClass, wrappedKey, aesWrap, sha256);
        }

        if (fileKey == null) {
            logger.warn("-- fileKey() > failed to unwrap key: {}, file: {}", Bytes.hex(key), file.getRelativePath());
        }

        return fileKey;
    }

    ByteString unwrapCurve25519(int protectionClass, ByteString key, AESWrap aesWrap, SHA256Digest sha256) {
        if (key.size() != 0x48) {
            logger.warn("-- unwrapCurve25519() > bad key length: {}", Bytes.hex(key));
            return null;
        }

        byte[] myPrivateKey = classKey(protectionClass, "KEY").toByteArray();
        if (myPrivateKey == null) {
            logger.warn("-- unwrapCurve25519() > no KEY key for protection class: {}", protectionClass);
            return null;
        }

        byte[] myPublicKey = classKey(protectionClass, "PBKY").toByteArray();
        if (myPublicKey == null) {
            logger.warn("-- unwrapCurve25519() > no PBKY key for protection class: {}", protectionClass);
            return null;
        }

        byte[] otherPublicKey = key.substring(0, 32).toByteArray();
        byte[] shared = Curve25519.create().agreement(otherPublicKey, myPrivateKey);
        byte[] pad = new byte[]{0x00, 0x00, 0x00, 0x01};
        byte[] hash = new byte[sha256.getDigestSize()];

        sha256.reset();
        sha256.update(pad, 0, pad.length);
        sha256.update(shared, 0, shared.length);
        sha256.update(otherPublicKey, 0, otherPublicKey.length);
        sha256.update(myPublicKey, 0, myPublicKey.length);
        sha256.doFinal(hash, 0);

        try {
            return ByteString.copyFrom(aesWrap.unwrap(hash, key.substring(0x20, key.size()).toByteArray()));
        } catch (IllegalStateException | InvalidCipherTextException ex) {
            logger.warn("-- unwrapCurve25519() > failed to unwrap key: {} protection class: {} exception: {}",
                    Bytes.hex(key), protectionClass, ex);
            return null;
        }
    }

    public boolean hasAttribute(String tag) {
        return attributes.containsKey(tag);
    }

    public ByteString attribute(String tag) {
        return attributes.get(tag);
    }

    public boolean hasClassKeys(int protectionClass) {
        return classKeys.containsKey(protectionClass);
    }

    public ByteString classKey(int protectionClass, String key) {
        if (!classKeys.containsKey(protectionClass)) {
            return null;
        }
        return classKeys.get(protectionClass).get(key);
    }

    public KeyBagType type() {
        return type;
    }

    public ByteString uuid() {
        return uuid;
    }

    @Override
    public String toString() {
        Map<String, String> attributesHex = hex(attributes);
        Map<Integer, Map<String, String>> classKeysHex
                = classKeys.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> hex(e.getValue())));

        return "KeyBag{"
                + "classKeys=" + classKeysHex
                + ", attributes=" + attributesHex
                + ", uuid=" + uuid
                + ", type=" + type
                + '}';
    }

    <T> Map<T, String> hex(Map<T, ByteString> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Bytes.hex(e.getValue())));
    }
}
