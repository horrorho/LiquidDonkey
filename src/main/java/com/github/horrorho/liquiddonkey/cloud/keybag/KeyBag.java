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

import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Keybag
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class KeyBag {

    private final Map<Integer, Map<String, ByteString>> classKeys;
    private final Map<String, ByteString> attributes;
    private final ByteString uuid;
    private final KeyBagType type;

    KeyBag(
            Map<Integer, Map<String, ByteString>> classKeys,
            Map<String, ByteString> attributes,
            ByteString uuid,
            KeyBagType type) {

        this.attributes = new HashMap<>(attributes);
        this.uuid = Objects.requireNonNull(uuid);
        this.type = Objects.requireNonNull(type);

        // Deep copy
        this.classKeys = classKeys.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashMap<>(entry.getValue())));

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
        return classKeys.containsKey(protectionClass)
                ? classKeys.get(protectionClass).get(key)
                : null;
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
