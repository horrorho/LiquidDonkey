/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a flatCopy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, flatCopy, modify, merge, publish, distribute, sublicense, and/or sell
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
package com.github.horrorho.liquiddonkey.settings.config;

import com.github.horrorho.liquiddonkey.settings.Property;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * ItemTypes.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
final class ItemTypes {

    static ItemTypes from(Properties properties) {
        return new ItemTypes(itemTypeToPaths(properties));
    }

    private static final Locale locale = Locale.US;

    static Map<String, Set<String>> itemTypeToPaths(Properties properties) {
        String prefix = properties.getProperty(Property.CONFIG_PREFIX_ITEM_TYPE.name());
        int prefixLength = prefix.length();

        Map<String, Set<String>> map = new HashMap<>();
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String propertyName = propertyNames.nextElement().toString();
            if (propertyName.startsWith(prefix)) {
                String itemType = propertyName.substring(prefixLength).toLowerCase(locale);
                Set<String> paths = new HashSet<>(Arrays.asList(properties.getProperty(propertyName).split("\\s")));
                map.put(itemType, paths);
            }
        }

        return map;
    }

    private final Map<String, Set<String>> itemTypeToPaths;

    ItemTypes(Map<String, Set<String>> itemTypeToRelativePaths) {
        this.itemTypeToPaths = itemTypeToRelativePaths;
    }

    Set<String> paths(String itemType) {
        if (itemType == null || itemType.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> paths = itemTypeToPaths.get(itemType.toLowerCase(locale));
        if (paths == null) {
            throw new IllegalArgumentException("unknown item type: " + itemType);
        }
        return paths;
    }

    Set<String> paths(List<String> itemTypes) {
        return itemTypes.stream().map(this::paths).flatMap(Set::stream).collect(Collectors.toSet());
    }
}
