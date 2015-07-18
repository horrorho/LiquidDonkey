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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import static junitparams.JUnitParamsRunner.$;
import junitparams.Parameters;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * SelectorTest.
 *
 * @author Ahseya
 */
@RunWith(JUnitParamsRunner.class)
public class SelectorTest {

    public static final Logger logger = LoggerFactory.getLogger(SelectorTest.class);
    public static final Marker marker = MarkerFactory.getMarker("TEST");
    public static final String newline = System.getProperty("line.separator");

    public static final List<String> options = Collections.unmodifiableList(Arrays.asList("one", "two", "three"));

    @Test
    @Parameters
    public void testSelection(List<String> expected, String... inputs) throws IOException {

        String input = inputs == null
                ? null
                : Stream.of(inputs).collect(Collectors.joining(newline));

        try (InputStream inputStream = new ByteArrayInputStream(input.getBytes());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                PrintStream printStream = new PrintStream(outputStream)) {

            List<String> results;
            try {
                results = Selector.builder(options).build().selection(inputStream, printStream);
            } catch (NoSuchElementException ex) {
                logger.debug(marker, "-- testSelection() > input stream depleted");
                results = null;
            }
            String output = outputStream.toString();
            logger.debug(marker, "-- testSelection() > in: {} out: {} results: {}", input, output, results);

            assertThat(results, is(expected));
        }
    }

    public static Object[] parametersForTestSelection() {
        return new Object[]{
            $(list(), "q"),
            $(list(), newline),
            $(list("one"), "1"),
            $(list("two"), "2"),
            $(list("three"), "3"),
            $(list("one", "two"), "1 2"),
            $(list("one", "two", "three"), "1 2 3"),
            $(list("three", "two", "one"), "3 2 1"),
            $(list("one", "two", "three"), "1 2 3 2 1"),
            $(list("one", "two", "three"), "1,2,3"),
            $(list("one", "two", "three"), "1, 2, 3"),
            $(null, "0"),
            $(null, "4"),
            $(null, "0 1"),
            $(null, "3 4"),
            $(list("one"), "0 1", "1"),
            $(list("one"), "3 4", "1"),
            $(list("one"), "x", "1"),
            $(list("one"), "x 1", "1"),
            $(list("one"), "1 x", "1"),
            $(list("one", "two"), "1 x 2", "2 x 1", "1 2"),
            $(null, "x"),
            $(null, "")
        };
    }

    public static <T> List<T> list(T... t) {
        return Arrays.asList(t);
    }
}
