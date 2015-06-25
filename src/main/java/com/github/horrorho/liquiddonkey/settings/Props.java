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
package com.github.horrorho.liquiddonkey.settings;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    public static <E extends Enum<E>> Props<E> newInstance(Class<E> type, Properties properties) {
        Props<E> props = newInstance(type);
        return props.addAll(properties);
    }

    public static <E extends Enum<E>> Props<E> newInstance(Class<E> type) {
        return newInstance(type, (Props<E>) null);
    }

    public static <E extends Enum<E>> Props<E> newInstance(Class<E> type, Props<E> defaults) {
        return newInstance(type, new EnumMap<>(type), defaults);
    }

    static <E extends Enum<E>> Props<E> newInstance(Class<E> type, EnumMap<E, String> map, Props<E> defaults) {
        Map<String, E> stringToEnum = Stream.of(type.getEnumConstants())
                .collect(Collectors.toMap(Enum::name, Function.identity()));

        return new Props(type, map, stringToEnum, defaults);
    }

    private final Class<E> type;
    private final EnumMap<E, String> map;
    private final Map<String, E> stringToEnum;
    private final Props<E> defaults;

    Props(Class<E> type, EnumMap<E, String> map, Map<String, E> stringToEnum, Props<E> defaults) {
        this.type = Objects.requireNonNull(type);
        this.map = Objects.requireNonNull(map);
        this.stringToEnum = Objects.requireNonNull(stringToEnum);
        this.defaults = defaults;
    }

    public boolean contains(E property) {
        return map.containsKey(property)
                ? true
                : defaults == null
                        ? false
                        : defaults.contains(property);
    }

    public Set<E> keySet() {
        Set<E> set = defaults == null
                ? EnumSet.noneOf(type)
                : defaults.keySet();

        set.addAll(map.keySet());
        return set;
    }

    public Props<E> distinct() {
        return Props.newInstance(type, new EnumMap<>(map), null);
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
        Properties properties = new Properties(defaults == null ? null : defaults.properties());
        keySet().stream()
                .filter(property -> map.get(property) != null)
                .forEach(property -> properties.setProperty(property.name(), map.get(property)));
        return properties;
    }

    public String put(E property, Object value) {
        return map.put(property, value.toString());
    }

    public String put(E property, String value) {
        return map.put(property, value);
    }

    public String pull(E property) {
        return put(property, get(property));
    }

    public String get(E property) {
        return map.containsKey(property)
                ? map.get(property)
                : defaults == null
                        ? null
                        : defaults.get(property);
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

    public Class<E> type() {
        return type;
    }

    @Override
    public String toString() {
        return "Props{" + "type=" + type + ", map=" + map + ", defaults=" + defaults + '}';
    }
}
