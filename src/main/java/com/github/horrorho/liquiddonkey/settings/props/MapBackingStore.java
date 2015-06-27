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
package com.github.horrorho.liquiddonkey.settings.props;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * MapBackingStore.
 *
 * @author Ahseya
 * @param <E> Enum type
 */
public class MapBackingStore<E extends Enum<E>> implements BackingStore<E> {

    private final Map<E, String> map;

    MapBackingStore(Map<E, String> map) {
        this.map = map;
    }

    @Override
    public boolean containsKey(E key) {
        return map.containsKey(key);
    }

    @Override
    public String get(E key) {
        return map.get(key);
    }

    @Override
    public Set<E> keySet() {
        return map.keySet();
    }

    @Override
    public String put(E key, String value) {
        return map.put(key, value);
    }

    @Override
    public String remove(E key) {
        return map.remove(key);
    }

    @Override
    public BackingStore<E> copyOf() {
        return new MapBackingStore<>(new HashMap<>(map));
    }
}
