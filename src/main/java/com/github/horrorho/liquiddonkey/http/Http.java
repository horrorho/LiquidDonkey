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
package com.github.horrorho.liquiddonkey.http;

import static com.github.horrorho.liquiddonkey.settings.Markers.http;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CloseableHttpClient helper.
 *
 * @author ahseya
 */
public final class Http implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(Http.class);

    private final ResponseHandler<String> defaultHandler = new BasicResponseHandler();
    private final CloseableHttpClient client;
    private final int socketTimeoutRetryCount;

    Http(CloseableHttpClient client, int socketTimeoutRetryCount) {
        this.client = Objects.requireNonNull(client);
        this.socketTimeoutRetryCount = socketTimeoutRetryCount;
    }

    public HttpExecutor<String> executor(String uri) {
        return executor(uri, defaultHandler);
    }

    public <T> HttpExecutor<T> executor(String uri, ResponseHandler<T> handler) {
        return new HttpExecutor<>(this::request, uri, handler);
    }

    /**
     * Request.
     *
     * @param <T> type
     * @param request not null
     * @param handler not null
     * @return result, may be null
     * @throws IOException
     */
    public <T> T request(HttpUriRequest request, ResponseHandler<T> handler) throws IOException {
        logger.trace(http, "<< request() < {}", request);

        int count = 0;
        while (true) {
            try {
                T response = client.execute(request, handler);
                return response;
            } catch (SocketTimeoutException ex) {
                // Not handled by the retry handler.
                if (count++ < socketTimeoutRetryCount) {
                    logger.trace("-- request() > retrying: ", ex);
                } else {
                    logger.trace(http, "-- request() > ", ex);
                    throw ex;
                }
            } catch (IOException ex) {
                logger.trace(http, "-- request() > ", ex);
                throw ex;
            }
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
