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
package com.github.horrorho.liquiddonkey.cloud.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;

/**
 * BiRef. Bi-mapped references.
 * <p>
 * Lightweight container. Remove only. No put methods. Null values not permitted.
 *
 * @author Ahseya
 * @param <K> key type
 * @param <V> value type
 */
@ThreadSafe
public class BiRef<K, V> {

    public static <K, V> BiRef<K, V> from(Map<K, ? extends Collection<V>> map) {

        ConcurrentMap<K, Set<V>> kToVSet = new ConcurrentHashMap<>();
        ConcurrentMap<V, Set<K>> vToKSet = new ConcurrentHashMap<>();

        map.entrySet().stream()
                .peek(entry -> {
                    if (entry.getKey() == null || entry.getValue() == null || entry.getValue().contains(null)) {
                        throw new IllegalArgumentException("Null values not permitted");
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

        return new BiRef(kToVSet, vToKSet);
    }

    public static <K, V> BiRef<K, V> fromx(Map<K, Collection<V>> map) {

        ConcurrentMap<K, Set<V>> kToVSet = new ConcurrentHashMap<>();
        ConcurrentMap<V, Set<K>> vToKSet = new ConcurrentHashMap<>();

        map.entrySet().stream()
                .peek(entry -> {
                    if (entry.getKey() == null || entry.getValue() == null || entry.getValue().contains(null)) {
                        throw new IllegalArgumentException("Null values not permitted");
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

        return new BiRef(kToVSet, vToKSet);
    }

    private final ConcurrentMap<K, Set<V>> kToVSet;    // Requires concurrent Set
    private final ConcurrentMap<V, Set<K>> vToKSet;    // Requires concurrent Set

    BiRef(ConcurrentMap kToVSet, ConcurrentMap vToKSet) {
        this.kToVSet = kToVSet;
        this.vToKSet = vToKSet;
    }

    public Set<K> keySet() {
        return kToVSet.keySet();
    }

    public Set<V> valueSet() {
        return vToKSet.keySet();
    }

    public Set<K> key(V value) {
        return new HashSet<>(vToKSet.get(value));
    }

    public Set<V> value(K key) {
        return new HashSet<>(kToVSet.get(key));
    }

    public List<V> removeKey(K key) {
        return remove(key, vToKSet, kToVSet);
    }

    public List<K> removeValue(V value) {
        return remove(value, kToVSet, vToKSet);
    }

    <T, U> List<U> remove(T t, Map<U, Set<T>> uToTSet, Map<T, Set<U>> tToUSet) {
        List<U> removed = new ArrayList<>();
        Set<U> uSet = tToUSet.get(t);

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
