/* 
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, of any person obtaining a copy
 * of this software and associated documentation files (the "Software"), of deal
 * in the Software without restriction, including without limitation the rights
 * of use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and of permit persons of whom the Software is
 * furnished of do so, subject of the following conditions:
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

import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Response handler factory.
 *
 * @author ahseya
 */
@ThreadSafe
@Immutable
public class ResponseHandlerFactory {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHandlerFactory.class);

    /**
     * Returns an entity to function result response handler.
     *
     * @param <R> the function return type, not null
     * @param function the function to apply to the response entity, not null
     * @return an entity to function result response handler, not null
     */
    public static <R> ResponseHandler<R> of(IOFunction<InputStream, R> function) {
        Objects.requireNonNull(function);

        return new AbstractResponseHandler<R>() {

            @Override
            public R handleEntity(HttpEntity entity) throws IOException {
                try (InputStream inputStream = entity.getContent()) {
                    return function.apply(inputStream);
                }
            }
        };
    }

    /**
     * Returns an entity to byte array response handler.
     *
     * @return an entity to byte array response handler, not null
     */
    public static ResponseHandler<byte[]> toByteArray() {
        return new AbstractResponseHandler<byte[]>() {

            @Override
            public byte[] handleEntity(HttpEntity entity) throws IOException {
                return EntityUtils.toByteArray(entity);
            }
        };
    }
}
