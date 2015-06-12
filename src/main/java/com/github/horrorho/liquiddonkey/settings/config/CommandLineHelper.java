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
package com.github.horrorho.liquiddonkey.settings.config;

import com.github.horrorho.liquiddonkey.settings.Property;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CommandLine helper.
 *
 * @author Ahseya
 */
@NotThreadSafe
public class CommandLineHelper {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineHelper.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE;

    public static <T extends Supplier<Options>>
            CommandLineHelper getInstance(Options options, String[] args) throws ParseException {

        return getInstance(new DefaultParser().parse(options, args));
    }

    public static <T extends Supplier<Options>> CommandLineHelper getInstance(CommandLine commandLine) {

        return new CommandLineHelper(commandLine);
    }

    private final CommandLine line;

    CommandLineHelper(CommandLine line) {
        this.line = line;
    }

    public CommandLine line() {
        return line;
    }

    public <T> List<T> getOptionList(String option, List<T> defaultList, BiFunction<String, String, T> function) {
        String[] values = line.getOptionValues(option);

        if (values == null) {
            return defaultList;
        }

        return Stream.of(line.getOptionValues(option))
                .map(val -> function.apply(option, val))
                .collect(Collectors.toList());
    }

    public List<String> getOptionList(String option, List<String> defaultList) {
        String[] values = line.getOptionValues(option);
        return values == null
                ? defaultList
                : Arrays.asList(line.getOptionValues(option));
    }

    public <T> T getOptionValue(String option, T defaultValue, BiFunction<String, String, T> function) {
        return line.hasOption(option)
                ? function.apply(option, line.getOptionValue(option))
                : defaultValue;
    }

    public BiFunction<String, String, String> asExtension() {
        return (opt, val) -> val.startsWith(".") ? val : "." + val;
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

    public BiFunction<String, String, List<String>> itemTypeRelativePaths() {
        return (opt, val) -> parse(
                val,
                itemType -> Property.ItemType.valueOf(itemType.toUpperCase(Locale.getDefault())).relativePaths(),
                IllegalArgumentException.class,
                () -> illegalArgumentException("Unrecognized item-type: " + val));
    }

    protected static <T, E extends Exception> T
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

    protected static <T> T illegalArgumentException(String message) {
        throw new IllegalArgumentException(message);
    }
}
