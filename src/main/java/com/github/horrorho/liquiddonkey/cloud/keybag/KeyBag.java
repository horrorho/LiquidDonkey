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
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import static com.github.horrorho.liquiddonkey.util.Bytes.hex;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
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
        return new KeyBagFactory().unlock(keySet);
    }

    private static final Logger logger = LoggerFactory.getLogger(KeyBag.class);

    private final Map<Integer, Map<String, ByteString>> classKeys;
    private final Map<String, ByteString> attributes;
    private final ByteString uuid;
    private final KeyBagType type;

    KeyBag(
            Map<Integer, Map<String, ByteString>> classKeys,
            Map<String, ByteString> attributes,
            ByteString uuid,
            KeyBagType type) {

        // Deep copy
        this.classKeys
                = classKeys.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashMap<>(entry.getValue())));
        this.attributes = new HashMap<>(attributes);
        this.uuid = uuid;
        this.type = type;

        classKeys.entrySet().stream().forEach(keys -> {
            keys.getValue().entrySet().stream().forEach(entry -> {
                logger.trace("** KeyBag() < class: {} key {}: {}",
                        keys.getKey(), entry.getKey(), hex(entry.getValue()));
            });
        });

        attributes.entrySet().stream().forEach(tagAttribute -> {
            logger.trace("** KeyBag() < attribute {}: {}", tagAttribute.getKey(), hex(tagAttribute.getValue()));
        });

        logger.debug("** KeyBag() < uuid: {}", hex(uuid));
        logger.debug("** KeyBag() < type: {}", type);
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
        return "KeyBag{"
                + "classKeys=" + classKeys
                + ", attributes=" + attributes
                + ", uuid=" + uuid
                + ", type=" + type
                + '}';
    }
}
