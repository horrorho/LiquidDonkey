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
package com.github.horrorho.liquiddonkey.cloud;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Accumulator.
 *
 * @author Ahseya
 * @param <K> key type
 * @param <V> value type
 */
public final class Accumulator<K, V> {

    public static <K, V> Accumulator<K, V> newInstance(BiConsumer<K, V> callback) {
        return new Accumulator<>(
                new ConcurrentHashMap<>(),
                callback);
    }

    private final ConcurrentMap<K, Queue<V>> keyToValues;
    private final BiConsumer<K, V> callback;

    Accumulator(ConcurrentMap<K, Queue<V>> keyToValues, BiConsumer<K, V> callback) {
        this.keyToValues = Objects.requireNonNull(keyToValues);
        this.callback = callback;
    }

    public void put(K key, V value) {
        keyToValues.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(value);
        if (callback != null) {
            callback.accept(key, value);
        }
    }

    public Map<K, List<V>> values() {
        return keyToValues.entrySet().stream()
                .map(entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().stream().collect(Collectors.toList())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
