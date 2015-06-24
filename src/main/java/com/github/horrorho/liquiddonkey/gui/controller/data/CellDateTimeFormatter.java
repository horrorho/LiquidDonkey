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
package com.github.horrorho.liquiddonkey.gui.controller.data;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author Ahseya
 */
public class CellDateTimeFormatter {

    private static final DateTimeFormatter formatter;

    static {
        Map<Long, String> day = new HashMap<>();
        day.put(1L, "Mon");
        day.put(2L, "Tue");
        day.put(3L, "Wed");
        day.put(4L, "Thu");
        day.put(5L, "Fri");
        day.put(6L, "Sat");
        day.put(7L, "Sun");

        Map<Long, String> month = new HashMap<>();
        month.put(1L, "Jan");
        month.put(2L, "Feb");
        month.put(3L, "Mar");
        month.put(4L, "Apr");
        month.put(5L, "May");
        month.put(6L, "Jun");
        month.put(7L, "Jul");
        month.put(8L, "Aug");
        month.put(9L, "Sep");
        month.put(10L, "Oct");
        month.put(11L, "Nov");
        month.put(12L, "Dec");

        formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                .appendLiteral(' ')
                .appendText(MONTH_OF_YEAR, month)
                .appendLiteral('\n')
                .appendValue(YEAR, 4)
                .appendLiteral('\n')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .appendLiteral(' ')
                .appendOffset("+HHMM", "GMT")
                .toFormatter(Locale.getDefault());
    }

    public static DateTimeFormatter formatter() {
        return formatter;
    }
}
