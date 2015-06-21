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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CommandLine helper.
 *
 * @author Ahseya
 */
@NotThreadSafe
public class Configuration extends Properties {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    public static Configuration newInstance() {
        return new Configuration(DateTimeFormatter.ISO_DATE);
    }

    private final DateTimeFormatter dateTimeFormatter;

    Configuration(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = Objects.requireNonNull(dateTimeFormatter);
    }

    public Configuration addAll(Properties properties) {
        properties.forEach((key, value) -> setProperty(key.toString(), value.toString()));
        return this;
    }

    public boolean contains(Property property) {
        return containsKey(property.key());
    }

    public <T> T get(Property property, Function<String, T> function) {
        return function.apply(get(property));
    }

    public String get(Property property) {
        String value = getProperty(property.key());
        if (value == null) {
            illegalArgumentException("missing property: " + property);
        }
        return value;
    }

    public <T> List<T> getList(Property property, Function<String, T> function) {
        return getList(property).stream()
                .map(function::apply)
                .collect(Collectors.toList());
    }

    public List<String> getList(Property property) {
        return Arrays.asList(get(property).split("\\s"));
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
                date -> LocalDate.parse(date, dateTimeFormatter).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(),
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
}
