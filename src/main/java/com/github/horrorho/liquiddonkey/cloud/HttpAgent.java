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
import com.github.horrorho.liquiddonkey.iofunction.IOBiFunction;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpAgent.
 * <p>
 * Request helper.
 *
 * @author Ahseya
 */
@ThreadSafe
public class HttpAgent {

    public static HttpAgent from(HttpClient client, HttpAgent executor) {
        return from(client, executor.retryCount, executor.retryDelayMs, executor.authenticator);
    }

    public static HttpAgent from(HttpClient client, int retryCount, int retryDelayMs, Authenticator authenticator) {
        return new HttpAgent(client, retryCount, retryDelayMs, authenticator);
    }

    private static final Logger logger = LoggerFactory.getLogger(HttpAgent.class);

    private final HttpClient client;
    private final int retryCount;
    private final int retryDelayMs;
    private final Authenticator authenticator;

    HttpAgent(HttpClient client, int retryCount, int retryDelayMs, Authenticator authenticator) {
        this.client = Objects.requireNonNull(client);
        this.retryCount = retryCount;
        this.retryDelayMs = retryDelayMs;
        this.authenticator = Objects.requireNonNull(authenticator);
    }

    public <T> T execute(IOBiFunction<HttpClient, String, T> function) throws IOException {
        return execute(c -> {
            while (true) {
                Authenticator.Token token = authenticator.get();
                try {
                    return function.apply(c, token.auth().mmeAuthToken());
                } catch (HttpResponseException ex) {
                    if (ex.getStatusCode() == 401) {
                        logger.warn("-- execute() > exception: ", ex);
                        execute(cc -> authenticator.reauthenticate(cc, token));
                    } else {
                        throw ex;
                    }
                }
            }
        });
    }

    public <T> T execute(IOFunction<HttpClient, T> function) throws IOException {
        int count = 0;
        while (true) {
            try {
                return function.apply(client);
            } catch (BadDataException | SocketTimeoutException ex) {
                if (count++ < retryCount) {
                    logger.warn("-- execute() > attempt: {} exception: {}", count, ex);
                    delay();
                } else {
                    throw ex;
                }
            }
        }
    }

    public String dsPrsID() {
        return authenticator.dsPrsID();
    }

    public boolean authenticatorIsInvalid() {
        return authenticator.isInvalid();
    }

    void delay() {
        try {
            TimeUnit.MILLISECONDS.sleep(retryDelayMs);
        } catch (InterruptedException ex) {
            logger.warn("-- delay() > re-interrupted: ", ex);
            Thread.currentThread().interrupt();
        }
    }
}
