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
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.data.TagValue;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KeyBag factory.
 *
 * @author Ahseya
 */
@NotThreadSafe
public class KeyBagFactory {

    private static final Logger logger = LoggerFactory.getLogger(KeyBagFactory.class);

    private static final int WRAP_DEVICE = 1;
    private static final int WRAP_PASSCODE = 2;

    private final Map<Integer, Map<String, ByteString>> classKeys = new HashMap<>();
    private final Map<String, ByteString> attributes = new HashMap<>();
    private ByteString salt;
    private ByteString uuid;
    private KeyBagType type;
    private int iterations;

    KeyBagFactory() {
    }

    KeyBag unlock(ICloud.MBSKeySet keySet) throws BadDataException {
        parse(keySet);

        iterations = Bytes.integer32(attribute("ITER"));
        salt = attribute("SALT");
        uuid = attribute("UUID");
        type = KeyBagType.from(Bytes.integer32(attribute("TYPE")));

        unlock(keySet.getKey(0).getKeyData());

        return new KeyBag(classKeys, attributes, uuid, type);
    }

    void parse(ICloud.MBSKeySet keySet) throws BadDataException {
        if (keySet.getKeyCount() < 2) {
            throw new BadDataException("Bad keybag.");
        }

        Map<String, ByteString> block = new HashMap<>();

        for (TagValue tagValue : TagValue.from(keySet.getKey(keySet.getKeyCount() - 1).getKeyData())) {
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
        logger.trace("-- unlock() < {}", Bytes.hex(passCode));
        if (type != KeyBagType.BACKUP && type != KeyBagType.OTA) {
            throw new BadDataException("Not a backup keybag");
        }

        byte[] passCodeKey = PBKDF2.newInstance().generate(
                passCode.toByteArray(),
                salt.toByteArray(),
                iterations,
                32);
        logger.debug("-- unlock() > passCodeKey: {}", Bytes.hex(passCodeKey));

        AESWrap aesWrap = AESWrap.newInstance();

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
    }

    ByteString attribute(String tag) throws BadDataException {
        if (!attributes.containsKey(tag)) {
            throw new BadDataException("-- attribute() - attribute missing: " + tag);
        }
        return attributes.get(tag);
    }
}
