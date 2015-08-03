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
package com.github.horrorho.liquiddonkey.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;

/**
 * BiMapSet.
 * <p>
 * Lightweight bi-map of sets with common references. Remove only. No put methods. Null values not permitted. Thread safe.
 *
 * @author Ahseya
 * @param <K> key type
 * @param <V> value type
 */
@ThreadSafe
public class BiMapSet<K, V> {

    public static <K, V> BiMapSet<K, V> from(Map<K, ? extends Collection<V>> map) {
        Objects.requireNonNull(map, "Map cannot be null");

        ConcurrentMap<K, Set<V>> kToVSet = new ConcurrentHashMap<>();
        ConcurrentMap<V, Set<K>> vToKSet = new ConcurrentHashMap<>();

        map.entrySet().stream()
                .peek(entry -> {
                    if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue().contains(null)) {
                        throw new NullPointerException("Null values not permitted");
                    }
                })
                .filter(entry -> !entry.getValue().isEmpty())
                .forEach(entry -> {
                    K key = entry.getKey();

                    Set<V> set = Collections.newSetFromMap(new ConcurrentHashMap<>());

                    entry.getValue().stream().forEach(value -> {
                        set.add(value);
                        vToKSet.computeIfAbsent(value, k
                                -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(key);
                    });

                    kToVSet.put(key, set);
                });

        return new BiMapSet(kToVSet, vToKSet);
    }

    final ConcurrentMap<K, Set<V>> kToVSet;    // Requires concurrent Set
    final ConcurrentMap<V, Set<K>> vToKSet;    // Requires concurrent Set

    BiMapSet(ConcurrentMap kToVSet, ConcurrentMap vToKSet) {
        this.kToVSet = kToVSet;
        this.vToKSet = vToKSet;
    }

    public Set<K> keySet() {
        return kToVSet.keySet();
    }

    public Set<V> valueSet() {
        return vToKSet.keySet();
    }

    public Set<K> keys(V value) {
        return set(value, vToKSet);
    }

    public Set<V> values(K key) {
        return set(key, kToVSet);
    }

    public Set<V> removeKey(K key) {
        return remove(key, vToKSet, kToVSet);
    }

    public Set<K> removeValue(V value) {
        return remove(value, kToVSet, vToKSet);
    }

    public boolean isEmpty() {
        return vToKSet.isEmpty() && kToVSet.isEmpty();
    }

    <T, U> Set<U> set(T t, Map<T, Set<U>> tToUSet) {
        Set<U> set = t == null
                ? null
                : tToUSet.get(t);

        return set == null
                ? new HashSet<>()
                : new HashSet<>(set);
    }

    <T, U> Set<U> remove(T t, Map<U, Set<T>> uToTSet, Map<T, Set<U>> tToUSet) {
        Set<U> removed = new HashSet<>();
        Set<U> uSet = t == null
                ? null
                : tToUSet.get(t);

        if (uSet == null) {
            return removed;
        }

        uSet.forEach(u -> {
            Set<T> set = uToTSet.get(u);

            if (set != null) {
                set.remove(t);
                if (set.isEmpty()) {
                    removed.add(u);
                    uToTSet.remove(u);
                }
            }
        });

        tToUSet.remove(t);
        return removed;
    }
}
