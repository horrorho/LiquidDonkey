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
package com.github.horrorho.liquiddonkey.pipe;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipes the specified Iterator elements to the specified Consumer and returns a list of failed element/ Exception
 * pairs.
 *
 * @author Ahseya
 * @param <T> the input type
 */
public class Piper<T> implements Function<Iterator<T>, List<ArgumentExceptionPair<T>>> {

    private static final Logger logger = LoggerFactory.getLogger(Piper.class);

    public static <T> Piper<T> newInstance(Consumer<T> consumer, boolean isAggressive) {
        return new Piper<>(consumer, isAggressive);
    }

    private final Consumer<T> consumer;
    private final boolean isAggressive;

    Piper(Consumer<T> consumer, boolean isAggressive) {
        this.consumer = Objects.requireNonNull(consumer);
        this.isAggressive = isAggressive;
    }

    @Override
    public List<ArgumentExceptionPair<T>> apply(Iterator<T> iterator) {
        logger.trace("<< apply()");

        List<ArgumentExceptionPair<T>> failed = new ArrayList<>();

        while (iterator.hasNext()) {
            T t = iterator.next();

            try {
                consumer.accept(t);
            } catch (UncheckedIOException ex) {
                logger.warn("-- apply() > exception: ", ex);
                failed.add(ArgumentExceptionPair.newInstance(t, ex));

                if (!isAggressive) {
                    break;
                }

                Throwable cause = ex.getCause();
                if (cause instanceof HttpResponseException) {
                    int code = ((HttpResponseException) cause).getStatusCode();
                    // Unauthorized
                    if (code == 401) {
                        break;
                    }
                }
            }
        }
        logger.trace(">> apply() > failed: {}", failed.size());
        return failed;
    }
}
