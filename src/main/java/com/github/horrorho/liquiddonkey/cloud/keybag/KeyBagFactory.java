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
import com.github.horrorho.liquiddonkey.crypto.PBKDF2;
import com.github.horrorho.liquiddonkey.util.TagValue;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KeyBagFactory.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class KeyBagFactory {

    /**
     * Returns a new unlocked KeyBag instance.
     *
     * @param key, not null
     * @param passcode, not null
     * @return a new unlocked KeyBag instance, not null
     * @throws BadDataException if the KeyBag cannot be unlocked or a data handling error occurred
     */
    public static KeyBag from(ICloud.MBSKey key, ByteString passcode) throws BadDataException {
        logger.trace("<< from()");

        KeyBagFactory factory = new KeyBagFactory(key, passcode).unlock();

        logger.trace(">> from()");
        return new KeyBag(
                factory.classKeys(),
                factory.attributes(),
                factory.uuid(),
                factory.type());
    }

    private static final Logger logger = LoggerFactory.getLogger(KeyBagFactory.class);

    private static final int WRAP_DEVICE = 1;
    private static final int WRAP_PASSCODE = 2;

    private final ICloud.MBSKey key;
    private final ByteString passcode;
    private final Map<Integer, Map<String, ByteString>> classKeys;
    private final Map<String, ByteString> attributes;
    private ByteString salt;
    private ByteString uuid;
    private KeyBagType type;
    private int iterations;

    KeyBagFactory(
            ICloud.MBSKey key,
            ByteString passcode,
            Map<Integer, Map<String, ByteString>> classKeys,
            Map<String, ByteString> attributes) {

        this.key = key;
        this.passcode = passcode;
        this.classKeys = classKeys;
        this.attributes = attributes;
    }

    KeyBagFactory(ICloud.MBSKey key, ByteString passcode) {
        this(key, passcode, new HashMap<>(), new HashMap<>());
    }

    KeyBagFactory unlock() throws BadDataException {
        parseKeySet();

        iterations = Bytes.integer32(attribute("ITER"));
        salt = attribute("SALT");
        uuid = attribute("UUID");
        type = KeyBagType.from(Bytes.integer32(attribute("TYPE")));

        unlock(passcode);
        return this;
    }

    void parseKeySet() throws BadDataException {
        Map<String, ByteString> block = new HashMap<>();

        for (TagValue tagValue : TagValue.from(key.getKeyData())) {
            if (tagValue.tag().equals("UUID")) {
                sort(block);
                block.clear();
            }
            block.put(tagValue.tag(), tagValue.value());
        }
        sort(block);
    }

    void sort(Map<String, ByteString> block) throws BadDataException {
        if (block.containsKey("CLAS")) {
            classKeys.put(Bytes.integer32(block.get("CLAS")) & 0xF, new HashMap<>(block));
        } else {
            attributes.putAll(block);
        }
    }

    void unlock(ByteString passCode) throws BadDataException {
        logger.trace("<< unlock() < uuid: {} passcode: {}", Bytes.hex(uuid), Bytes.hex(passCode));

        if (type != KeyBagType.BACKUP && type != KeyBagType.OTA) {
            throw new BadDataException("Not a backup keybag");
        }

        byte[] passCodeKey = PBKDF2.create().generate(
                passCode.toByteArray(),
                salt.toByteArray(),
                iterations,
                32);
        logger.debug("-- unlock() > passCodeKey: {}", Bytes.hex(passCodeKey));

        AESWrap aesWrap = AESWrap.create();

        for (Map.Entry<Integer, Map<String, ByteString>> entrySet : classKeys.entrySet()) {
            Map<String, ByteString> keys = entrySet.getValue();

            if (!keys.containsKey("WPKY") || !keys.containsKey("WRAP")) {
                continue;
            }

            int wrap = Bytes.integer32(keys.get("WRAP"));
            if ((wrap & WRAP_DEVICE) == 0 && (wrap & WRAP_PASSCODE) != 0) {
                byte[] wrappedKey = keys.get("WPKY").toByteArray();
                try {
                    byte[] unwrappedKey = aesWrap.unwrap(passCodeKey, wrappedKey);
                    keys.put("KEY", ByteString.copyFrom(unwrappedKey));
                    logger.trace("-- unlock() > key unwrapped: {} class: {}",
                            Bytes.hex(unwrappedKey), entrySet.getKey());
                } catch (InvalidCipherTextException ex) {
                    logger.warn("-- unlock() > failed to unwrap key: {} class: {}",
                            Bytes.hex(wrappedKey), entrySet.getKey(), ex);
                }
            }
        }
        logger.trace(">> unlock()");
    }

    ByteString attribute(String tag) throws BadDataException {
        if (!attributes.containsKey(tag)) {
            throw new BadDataException("-- attribute() > missing: " + tag);
        }
        return attributes.get(tag);
    }

    ByteString uuid() {
        return uuid;
    }

    KeyBagType type() {
        return type;
    }

    Map<Integer, Map<String, ByteString>> classKeys() {
        return classKeys.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashMap<>(entry.getValue())));
    }

    Map<String, ByteString> attributes() {
        return new HashMap<>(attributes);
    }
}
