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

import com.github.horrorho.liquiddonkey.iofunction.IOSupplier;
import java.io.Closeable;
import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

/**
 * PersistentWrapper. Incomplete.
 * @author Ahseya
 * @param <T> wrapped HttpClient, Closeable
 */
public class PersistentWrapper<T extends HttpClient & Closeable> implements HttpClient, Closeable {

    public static <T extends HttpClient & Closeable> PersistentWrapper wrap(T closeableHttpClient) {
        return new PersistentWrapper(closeableHttpClient);
    }

    private final T client;

    PersistentWrapper(T client) {
        this.client = client;
    }

    private <T> T retry(IOSupplier<T> request) throws IOException, ClientProtocolException {
        // TODO implement for java.net.SocketTimeoutException
        return request.get();
    }

    @Override
    public HttpParams getParams() {
        return client.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return client.getConnectionManager();
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
        return retry(() -> client.execute(request));
    }

    @Override
    public HttpResponse execute(
            HttpUriRequest request,
            HttpContext context
    ) throws IOException, ClientProtocolException {

        return retry(() -> client.execute(request, context));
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
        return retry(() -> client.execute(target, request));
    }

    @Override
    public HttpResponse execute(
            HttpHost target,
            HttpRequest request,
            HttpContext context)
            throws IOException, ClientProtocolException {

        return retry(() -> client.execute(target, request, context));
    }

    @Override
    public <T> T execute(
            HttpUriRequest request,
            ResponseHandler<? extends T> responseHandler
    ) throws IOException, ClientProtocolException {

        return retry(() -> client.execute(request, responseHandler));
    }

    @Override
    public <T> T execute(
            HttpUriRequest request,
            ResponseHandler<? extends T> responseHandler,
            HttpContext context
    ) throws IOException, ClientProtocolException {

        return retry(() -> client.execute(request, responseHandler, context));
    }

    @Override
    public <T> T execute(
            HttpHost target,
            HttpRequest request,
            ResponseHandler<? extends T> responseHandler
    ) throws IOException, ClientProtocolException {

        return retry(() -> client.execute(target, request, responseHandler));
    }

    @Override
    public <T> T execute(
            HttpHost target,
            HttpRequest request,
            ResponseHandler<? extends T> responseHandler,
            HttpContext context
    ) throws IOException, ClientProtocolException {

        return retry(() -> client.execute(target, request, responseHandler, context));
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
