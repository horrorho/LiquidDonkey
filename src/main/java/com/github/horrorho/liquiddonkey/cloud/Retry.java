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
package com.github.horrorho.liquiddonkey.cloud;

import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.iofunction.IOSupplier;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retry.
 * <p>
 Provides execute functionality for streaming {@link java.net.SocketTimeoutException} and
 * {@link com.github.horrorho.liquiddonkey.exception.BadDataException}.
 *
 * @author Ahseya
 */
public class Retry {

    private static final Logger logger = LoggerFactory.getLogger(Retry.class);

    private final int retryCount;
    private final int retryDelayMs;

    Retry(int retryCount, int retryDelayMs) {
        this.retryCount = retryCount;
        this.retryDelayMs = retryDelayMs;
    }

    public <T> T execute(IOSupplier<T> supplier) throws IOException, InterruptedException {
        int count = 0;
        while (true) {
            try {
                return supplier.get();
            } catch (BadDataException | SocketTimeoutException ex) {
                if (count++ < retryCount) {
                    logger.warn("-- retry() > attempt: {} exception: {}", count, ex);
                } else {
                    throw ex;
                }
            }

            TimeUnit.MILLISECONDS.sleep(retryDelayMs);
        }
    }
}
