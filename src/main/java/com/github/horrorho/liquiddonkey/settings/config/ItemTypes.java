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
package com.github.horrorho.liquiddonkey.settings.config;

import com.github.horrorho.liquiddonkey.settings.Configuration;
import com.github.horrorho.liquiddonkey.settings.Property;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class ItemTypes {

    public static ItemTypes newInstance(Configuration configuration) {
        return new ItemTypes(itemTypeToPaths(configuration));
    }

    static Map<String, Set<String>> itemTypeToPaths(Configuration config) {
        String itemTypePrefix = config.get(Property.CONFIG_PREFIX_ITEM_TYPE);
        int prefixLength = itemTypePrefix.length();

        return config.entrySet().stream()
                .filter(property -> property.getKey().toString().startsWith(itemTypePrefix))
                .collect(
                        Collectors.toMap(
                                property -> property.getKey().toString().substring(prefixLength).toLowerCase(Locale.US),
                                property -> new HashSet<>(Arrays.asList(property.getValue().toString().split("\\s")))));
    }

    private final Map<String, Set<String>> itemTypeToPaths;

    ItemTypes(Map<String, Set<String>> itemTypeToRelativePaths) {
        this.itemTypeToPaths = itemTypeToRelativePaths;
    }

    public Set<String> paths(String itemType) {
        if (itemType == null || itemType.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> paths = itemTypeToPaths.get(itemType.toLowerCase(Locale.US));
        if (paths == null) {
            throw new IllegalArgumentException("unknown item type: " + itemType);
        }
        return paths;
    }

    public Set<String> paths(List<String> itemTypes) {
        return itemTypes.stream().map(this::paths).flatMap(Set::stream).collect(Collectors.toSet());
    }
}
