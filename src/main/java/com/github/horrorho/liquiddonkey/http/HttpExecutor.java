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

import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.iofunction.IOBiFunction;
import com.github.horrorho.liquiddonkey.iofunction.IOSupplier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.NotThreadSafe;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

/**
 * Http request builder.
 *
 * @author ahseya
 * @param <T> Return type.
 */
@NotThreadSafe
public class HttpExecutor<T> {

    private final IOBiFunction<HttpUriRequest, ResponseHandler<T>, T> http;
    private final ResponseHandler<T> handler;
    private final String uri;
    private final List<Header> headers = new ArrayList<>();
    private final List<NameValuePair> parameters = new ArrayList<>();

    HttpExecutor(
            IOBiFunction<HttpUriRequest, ResponseHandler<T>, T> http,
            String uri,
            ResponseHandler<T> handler) {

        this.handler = Objects.requireNonNull(handler);
        this.http = Objects.requireNonNull(http);
        this.uri = Objects.requireNonNull(uri);
    }

    public HttpExecutor<T> header(String name, String value) {
        return HttpExecutor.this.headers(new BasicHeader(name, value));
    }

    public HttpExecutor<T> headers(Header... headers) {
        return HttpExecutor.this.headers(Arrays.asList(headers));
    }

    public HttpExecutor<T> headers(Collection<Header> headers) {
        this.headers.addAll(headers);
        return this;
    }

    public HttpExecutor<T> parameter(String name, String value) {
        return parameters(new BasicNameValuePair(name, value));
    }

    public HttpExecutor<T> parameters(NameValuePair... parameters) {
        this.parameters.addAll(Arrays.asList(parameters));
        return this;
    }

    public HttpExecutor<T> parameters(Collection<NameValuePair> parameters) {
        this.parameters.addAll(parameters);
        return this;
    }

    IOSupplier<T> execute(RequestBuilder builder) throws AuthenticationException, IOException {
        builder.setUri(uri);
        headers.stream().forEach(builder::addHeader);
        parameters.stream().forEach(builder::addParameter);
        HttpUriRequest request = builder.build();
        return () -> http.apply(request, handler);
    }

    /**
     * Get.
     *
     * @return IOSupplier, not null
     * @throws AuthenticationException, IOException
     * @throws IOException
     */
    public IOSupplier<T> get() throws AuthenticationException, IOException {
        return execute(RequestBuilder.get());
    }

    /**
     * Post.
     *
     * @return IOSupplier, not null
     * @throws AuthenticationException, IOException
     * @throws IOException
     */
    public IOSupplier<T> post() throws AuthenticationException, IOException {
        return execute(RequestBuilder.post());
    }

    /**
     * Post.
     *
     * @param postData post data, not null
     * @return IOSupplier, not null
     * @throws AuthenticationException, IOException
     * @throws IOException
     */
    public IOSupplier<T> post(byte[] postData) throws AuthenticationException, IOException {
        RequestBuilder builder = RequestBuilder.post();
        builder.setEntity(new ByteArrayEntity(postData));
        return execute(builder);
    }

    /**
     * Execute.
     *
     * @param method, not null
     * @return IOSupplier, not null
     * @throws AuthenticationException, IOException
     * @throws IOException
     */
    public IOSupplier<T> method(String method) throws AuthenticationException, IOException {
        return execute(RequestBuilder.create(method));
    }
}
