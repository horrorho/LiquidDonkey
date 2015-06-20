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
package com.github.horrorho.liquiddonkey.settings.old;

import com.github.horrorho.liquiddonkey.settings.config.*;
import com.github.horrorho.liquiddonkey.settings.CommandLineOptions;
import com.github.horrorho.liquiddonkey.settings.Property;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
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
public final class PropertyConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PropertyConfiguration.class);

    public static PropertyConfiguration newInstance(CommandLineOptions commandLineOptions) {
        return new PropertyConfiguration(DateTimeFormatter.ISO_DATE, commandLineOptions);
    }

    public static PropertyConfiguration newInstance(
            DateTimeFormatter dateTimeFormatter,
            CommandLineOptions commandLineOptions) {

        return new PropertyConfiguration(dateTimeFormatter, commandLineOptions);
    }

    private final DateTimeFormatter dateTimeFormatter;
    private final CommandLineOptions commandLineOptions;

    private PropertyConfiguration(DateTimeFormatter dateTimeFormatter, CommandLineOptions commandLineOptions) {
        this.dateTimeFormatter = Objects.requireNonNull(dateTimeFormatter);
        this.commandLineOptions = Objects.requireNonNull(commandLineOptions);
    }

    public <T> T get(Property property, BiFunction<String, String, T> function) {
        String opt = commandLineOptions.opt(property); // TODO where was it specified command line or property list?

        return function.apply(opt, get(property));
    }

    public String get(Property property) {
        return getString(property.key());
    }

    public <T> List<T> getList(Property property, BiFunction<String, String, T> function) {
        String opt = commandLineOptions.opt(property);
        Function<String, T> parse = value -> function.apply(opt, value);

        return getList(property).stream()
                .map(parse::apply)
                .collect(Collectors.toList());
    }

    public List<String> getList(Property property) {
        return Arrays.asList(getStringArray(property.key()));
    }

    public BiFunction<String, String, String> asHex() {
        return (opt, val) -> val.matches("^[0-9a-fA-F]+$")
                ? val
                : illegalArgumentException("Bad hex value for " + opt + ": " + val);
    }

    public BiFunction<String, String, Double> asDouble() {
        return asNumber(Double::parseDouble);
    }

    public BiFunction<String, String, Integer> asInteger() {
        return asNumber(Integer::parseInt);
    }

    public BiFunction<String, String, Long> asLong() {
        return asNumber(Long::parseLong);
    }

    public <T extends Number> BiFunction<String, String, T> asNumber(Function<String, T> parser) {
        return (opt, val) -> parse(
                val,
                parser,
                NumberFormatException.class,
                () -> illegalArgumentException("Bad number for " + opt + ": " + val));
    }

    public BiFunction<String, String, Long> asTimestamp() {
        return (opt, val) -> parse(
                val,
                date -> LocalDate.parse(date, dateTimeFormatter).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(),
                DateTimeParseException.class,
                () -> illegalArgumentException("Bad date for " + opt + ": " + val));
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
