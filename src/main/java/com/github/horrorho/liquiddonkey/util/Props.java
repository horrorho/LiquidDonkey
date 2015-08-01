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
package com.github.horrorho.liquiddonkey.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Props. Lightweight properties helper.
 *
 * @author Ahseya
 * @param <E>
 */
@Immutable
@ThreadSafe
public final class Props<E extends Enum<E>> {

    public static <E extends Enum<E>> Props<E> from(Properties properties) {
        return new Props<>(properties, defaultDateTimeFormatter);
    }

    public static <E extends Enum<E>> Props<E> from(Properties properties, DateTimeFormatter dateTimeFormatter) {
        return new Props<>(properties, dateTimeFormatter);
    }

    public static Properties flatCopy(Properties properties) {
        if (properties == null) {
            return null;
        }

        Enumeration<?> propertyNames = properties.propertyNames();
        Properties copy = new Properties();

        while (propertyNames.hasMoreElements()) {
            Object propertyName = propertyNames.nextElement();
            copy.setProperty(propertyName.toString(), properties.getProperty(propertyName.toString()));
        }
        return copy;
    }

    private static final Logger logger = LoggerFactory.getLogger(Props.class);

    private static final DateTimeFormatter defaultDateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE;

    private final Properties properties;
    private final DateTimeFormatter dateTimeFormatter;

    Props(Properties properties, DateTimeFormatter dateTimeFormatter) {
        this.properties = Objects.requireNonNull(flatCopy(properties));
        this.dateTimeFormatter = Objects.requireNonNull(dateTimeFormatter);
    }

    Properties properties() {
        return flatCopy(properties);
    }

    public boolean containsProperty(E property) {
        return property == null
                ? false
                : properties.containsKey(property.name());
    }

    public String getProperty(E property) {
        return containsProperty(property)
                ? properties.getProperty(property.name())
                : null;
    }

    public <T> T getProperty(E property, Function<String, T> function) {
        return function.apply(getProperty(property));
    }

    public List<String> asList(String val) {
        return val == null
                ? null
                : Arrays.asList(val.split("\\s"));
    }

    public <T> List<T> asList(String val, Function<String, T> function) {
        List<String> vals = asList(val);
        return vals == null
                ? null
                : vals.stream()
                .map(function::apply)
                .collect(Collectors.toList());
    }

    public Boolean asBoolean(String val) {
        return Boolean.parseBoolean(val);
    }

    public String isHex(String val) {
        return val == null
                ? null
                : val.matches("^[0-9a-fA-F]+$")
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
                date -> LocalDate.parse(date, dateTimeFormatter).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(),
                DateTimeParseException.class,
                () -> illegalArgumentException("bad date: " + val));
    }

    private <T, E extends Exception> T
            parse(String value, Function<String, T> parser, Class<E> exceptionType, Supplier<T> exception) {

        try {
            return value == null
                    ? null
                    : parser.apply(value);
        } catch (RuntimeException ex) {
            if (!(exceptionType.isAssignableFrom(ex.getClass()))) {
                throw ex;
            }
            logger.warn("-- parse() > exception: ", ex);
            return exception.get();
        }
    }

    private <T> T illegalArgumentException(String message) {
        throw new IllegalArgumentException(message);
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
