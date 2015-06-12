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

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CallableFunction. {@link java.lang.Runnable} that performs a function on the supplied input and returns the result.
 *
 * @author ahseya
 * @param <T> the input type
 * @param <R> the return type
 */
@NotThreadSafe
public final class CallableFunction<T, R> implements Callable<R> {

    private static final Logger logger = LoggerFactory.getLogger(CallableFunction.class);

    public static <T, R> CallableFunction<T, R> newInstance(T input, Function<T, R> function) {
        return new CallableFunction<>(input, function);
    }

    private final T input;
    private final Function<T, R> function;

    CallableFunction(T input, Function<T, R> function) {

        this.input = Objects.requireNonNull(input);
        this.function = Objects.requireNonNull(function);
    }

    @Override
    public R call() throws Exception {
        logger.trace("<< run() <");
        try {
            R result = function.apply(input);

            logger.trace(">> run() >");
            return result;
        } catch (Exception ex) {
            logger.debug(">> run() > exception:", ex);
            throw ex;
        }
    }
}
