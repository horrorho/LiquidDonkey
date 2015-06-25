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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Props.
 *
 * Lightweight properties with parsers.
 *
 * @author Ahseya
 */
@NotThreadSafe
public class Props {

    private static final Logger logger = LoggerFactory.getLogger(Props.class);

    public static Props newInstance(Properties properties) {
        Props props = newInstance();
        return props.addAll(properties);
    }

    public static Props newInstance() {
        return newInstance((Props) null);
    }

    public static Props newInstance(Props defaults) {
        return newInstance(new EnumMap<>(Property.class), defaults);
    }

    static Props newInstance(Map<Property, String> map, Props defaults) {
        return new Props(map, defaults);
    }

    private final Map<Property, String> map;
    private final Props defaults;

    Props(Map<Property, String> map, Props defaults) {
        this.map = Objects.requireNonNull(map);
        this.defaults = defaults;
    }

    public boolean contains(Property property) {
        return map.containsKey(property)
                ? true
                : defaults == null
                        ? false
                        : defaults.contains(property);
    }

    public Set<Property> keySet() {
        Set<Property> set = defaults == null
                ? EnumSet.noneOf(Property.class)
                : defaults.keySet();

        set.addAll(map.keySet());
        return set;
    }

    public Props distinct() {
        return Props.newInstance(new EnumMap<>(map), null);
    }

    public Props addAll(Properties properties) {
        properties.stringPropertyNames().stream()
                .forEach(key -> {
                    try {
                        put(Property.valueOf(key), properties.getProperty(key));
                    } catch (IllegalArgumentException ex) {
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

    public String put(Property property, String value) {
        return map.put(property, value);
    }

    public String pull(Property property) {
        return put(property, get(property));
    }

    public String get(Property property) {
        return map.containsKey(property)
                ? map.get(property)
                : defaults == null
                        ? null
                        : defaults.get(property);
    }

    public <T> T get(Property property, Function<String, T> function) {
        return function.apply(get(property));
    }

    public List<String> getList(Property property) {
        return Arrays.asList(get(property).split("\\s"));
    }

    public <T> List<T> getList(Property property, Function<String, T> function) {
        return getList(property).stream()
                .map(function::apply)
                .collect(Collectors.toList());
    }

    public Boolean asBoolean(String val) {
        return Boolean.parseBoolean(val);
    }

    public String asHex(String val) {
        return val.matches("^[0-9a-fA-F]+$")
                ? val
                : illegalArgumentException("bad hex: " + val);
    }

    public Double asDouble(String val) {
        return asNumber(val, Double::parseDouble);
    }

    public Integer asInteger(String val) {
        return asNumber(val, Integer::parseInt);
    }

    public Long asLong(String val) {
        return asNumber(val, Long::parseLong);
    }

    public <T extends Number> T asNumber(String val, Function<String, T> parser) {
        return parse(
                val,
                parser,
                NumberFormatException.class,
                () -> illegalArgumentException("bad number: " + val));
    }

    public Long asTimestamp(String val) {
        return parse(
                val,
                date -> LocalDate.parse(date, Property.dateTimeFormatter()).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(),
                DateTimeParseException.class,
                () -> illegalArgumentException("bad date: " + val));
    }

    protected <T, E extends Exception> T
            parse(String value, Function<String, T> parser, Class<E> exceptionType, Supplier<T> exception) {

        try {
            return parser.apply(value);
        } catch (Exception ex) {
            if (!(exceptionType.isAssignableFrom(ex.getClass()))) {
                throw ex;
            }
            logger.warn("-- parse() > exception: ", ex);
            return exception.get();
        }
    }

    protected <T> T illegalArgumentException(String message) {
        throw new IllegalArgumentException(message);
    }

    @Override
    public String toString() {
        return "Props{" + "map=" + map + ", defaults=" + defaults + '}';
    }
}
