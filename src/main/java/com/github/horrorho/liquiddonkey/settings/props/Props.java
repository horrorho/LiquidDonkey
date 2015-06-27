/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copyOf
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copyOf, modify, merge, publish, distribute, sublicense, and/or sell
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Props.
 *
 * Lightweight properties.
 *
 * @author Ahseya
 * @param <E>
 */
@NotThreadSafe
public class Props<E extends Enum<E>> {

    private static final Logger logger = LoggerFactory.getLogger(Props.class);

    private final Map<String, E> stringToEnum;
    private final Props<E> parent;
    private final BackingStore<E> store;

    Props(BackingStore<E> store, Map<String, E> stringToEnum, Props<E> parent) {
        this.store = Objects.requireNonNull(store);
        this.stringToEnum = Objects.requireNonNull(stringToEnum);
        this.parent = parent;
    }

    public boolean contains(E property) {
        return store.containsKey(property)
                ? true
                : parent == null
                        ? false
                        : parent.contains(property);
    }

    public boolean containsDistinct(E property) {
        return store.containsKey(property);
    }

    public Set<E> keySet() {
        Set<E> set = parent == null
                ? new HashSet()
                : parent.keySet();

        set.addAll(store.keySet());
        return set;
    }

    public Props<E> distinct() {
        return new Props(store.copyOf(), new HashMap<>(stringToEnum), null);
    }

    public Props<E> addAll(Properties properties) {
        properties.stringPropertyNames().stream()
                .forEach(key -> {
                    if (stringToEnum.containsKey(key)) {
                        put(stringToEnum.get(key), properties.getProperty(key));
                    } else {
                        logger.warn("-- addAll() - unknown Property type: {}", key);
                    }
                });
        return this;
    }

    public Properties properties() {
        Properties properties = new Properties(parent == null ? null : parent.properties());
        keySet().stream()
                .filter(property -> get(property) != null)
                .forEach(property -> properties.setProperty(property.name(), get(property)));
        return properties;
    }

    public Props<E> parent() {
        return parent;
    }

    public String put(E property, Object value) {
        return put(property, value.toString());
    }

    public String put(E property, String value) {
        return store.put(property, value);
    }

    public String pull(E property) {
        return containsDistinct(property)
                ? get(property)
                : put(property, get(property));
    }

    public String get(E property) {
        return store.containsKey(property)
                ? store.get(property)
                : parent == null
                        ? null
                        : parent.get(property);
    }

    public <T> T get(E property, Function<String, T> function) {
        return function.apply(get(property));
    }

    public List<String> getList(E property) {
        return Arrays.asList(get(property).split("\\s"));
    }

    public <T> List<T> getList(E property, Function<String, T> function) {
        return getList(property).stream()
                .map(function::apply)
                .collect(Collectors.toList());
    }

    public String remove(E property) {
        return store.remove(property);
    }

    @Override
    public String toString() {
        return "Props{" + "stringToEnum=" + stringToEnum + ", parent=" + parent + ", store=" + store + '}';
    }
}
