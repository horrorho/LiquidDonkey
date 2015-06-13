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

import static com.github.horrorho.liquiddonkey.util.Bytes.hex;
import com.github.horrorho.liquiddonkey.crypto.AESWrap;
import com.github.horrorho.liquiddonkey.crypto.PBKDF2;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSKeySet;
import com.github.horrorho.liquiddonkey.tagvalue.TagValue;
import static com.github.horrorho.liquiddonkey.util.Bytes.integerOrFail;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keybag.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class KeyBag {

    /**
     * Unlock and return a new KeyBag instance.
     *
     * @param keySet the key set, not null
     * @return a new KeyBag instance
     * @throws BadDataException if the KeyBag cannot be unlocked or the contents are corrupted
     * @throws NullPointerException if the keySet argument is null
     */
    public static KeyBag newInstance(MBSKeySet keySet) throws BadDataException {
        return new KeyBag(keySet);
    }

    private static final int WRAP_DEVICE = 1;
    private static final int WRAP_PASSCODE = 2;

    private static final Logger logger = LoggerFactory.getLogger(KeyBag.class);

    private final Map<Integer, Map<String, ByteString>> classKeys = new HashMap<>();
    private final Map<String, ByteString> attributes = new HashMap<>();
    private final ByteString salt;
    private final ByteString uuid;
    private final KeyBagType type;
    private final int iterations;

    KeyBag(MBSKeySet keySet) throws BadDataException {
        Objects.requireNonNull(keySet);

        parse(keySet);

        if (logger.isTraceEnabled()) {
            attributes.entrySet().stream().forEach(tagAttribute -> {
                logger.trace("** KeyBag() < attribute {}: {}", tagAttribute.getKey(), hex(tagAttribute.getValue()));
            });
        }

        iterations = integerOrFail(attributeOrFail("ITER"));
        salt = attributeOrFail("SALT");
        uuid = attributeOrFail("UUID");
        type = KeyBagType.from(integerOrFail(attributeOrFail("TYPE")));

        logger.trace("** KeyBag() < iterations: {}", iterations);
        logger.trace("** KeyBag() < salt: {}", hex(salt));
        logger.trace("** KeyBag() < type: {}", type);
        logger.trace("** KeyBag() < uuid: {}", hex(uuid));

        unlock(keySet.getKey(0).getKeyData());
        logger.trace("** KeyBag() > KeyBag unlocked.");

        if (logger.isTraceEnabled()) {
            classKeys.entrySet().stream().forEach(keys -> {
                keys.getValue().entrySet().stream().forEach(entry -> {
                    logger.trace("** KeyBag() < class: {} key {}: {}",
                            keys.getKey(), entry.getKey(), hex(entry.getValue()));
                });
            });
        }
    }

    private void parse(MBSKeySet keySet) throws BadDataException {
        if (keySet.getKeyCount() < 2) {
            throw new BadDataException("Bad keybag.");
        }

        Map<String, ByteString> block = new HashMap<>();

        for (TagValue tagValue : TagValue.parseTagLengthValues(keySet.getKey(keySet.getKeyCount() - 1).getKeyData())) {
            if (tagValue.tag().equals("UUID")) {
                sort(block);
                block.clear();
            }
            block.put(tagValue.tag(), tagValue.value());
        }
        sort(block);
    }

    private void sort(Map<String, ByteString> block) throws BadDataException {
        if (block.containsKey("CLAS")) {
            classKeys.put(integerOrFail(block.get("CLAS")) & 0xF, new HashMap<>(block));
        } else {
            attributes.putAll(block);
        }
    }

    private void unlock(ByteString passCode) throws BadDataException {
        logger.trace("-- unlock() < {}", hex(passCode));
        if (type != KeyBagType.BACKUP && type != KeyBagType.OTA) {
            throw new BadDataException("Not a backup keybag");
        }

        byte[] passCodeKey = PBKDF2.newInstance().generate(
                passCode.toByteArray(),
                salt.toByteArray(),
                iterations,
                32);
        logger.debug("-- unlock() > passCodeKey: {}", hex(passCodeKey));

        AESWrap aesWrap = AESWrap.newInstance();

        for (Map.Entry<Integer, Map<String, ByteString>> entrySet : classKeys.entrySet()) {
            Map<String, ByteString> keys = entrySet.getValue();

            if (!keys.containsKey("WPKY") || !keys.containsKey("WRAP")) {
                continue;
            }

            int wrap = integerOrFail(keys.get("WRAP"));
            if ((wrap & WRAP_DEVICE) == 0 && (wrap & WRAP_PASSCODE) != 0) {
                byte[] wrappedKey = keys.get("WPKY").toByteArray();
                try {
                    byte[] unwrappedKey = aesWrap.unwrap(passCodeKey, wrappedKey);
                    keys.put("KEY", ByteString.copyFrom(unwrappedKey));
                    logger.trace("-- unlock() > key unwrapped: {} class: {}", hex(unwrappedKey), entrySet.getKey());
                } catch (InvalidCipherTextException ex) {
                    logger.warn("-- unlock() >- failed to unwrap key: {} class: {}",
                            hex(wrappedKey), entrySet.getKey(), ex);
                }
            }
        }
    }

    private ByteString attributeOrFail(String tag) throws BadDataException {
        if (!attributes.containsKey(tag)) {
            throw new BadDataException("-- attributeOrFail() - attribute missing: " + tag);
        }
        return attributes.get(tag);
    }

    public boolean containsAttribute(String tag) {
        return attributes.containsKey(tag);
    }

    public ByteString attribute(String tag) {
        return attributes.get(tag);
    }

    public boolean containsClassKeys(int protectionClass) {
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
}
