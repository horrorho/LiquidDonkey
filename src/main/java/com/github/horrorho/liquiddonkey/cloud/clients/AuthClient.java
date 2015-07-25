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
package com.github.horrorho.liquiddonkey.cloud.clients;

import com.github.horrorho.liquiddonkey.data.SimplePropertyList;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.ResponseHandlerFactory;
import java.io.IOException;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuthClient.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class AuthClient {

    public static AuthClient create() {
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(AuthClient.class);

    private static final AuthClient instance
            = new AuthClient(ResponseHandlerFactory.toByteArray(), Headers.create());

    private final ResponseHandler<byte[]> byteArrayResponseHandler;
    private final Headers headers;

    AuthClient(ResponseHandler<byte[]> byteArrayResponseHandler, Headers headers) {
        this.byteArrayResponseHandler = Objects.requireNonNull(byteArrayResponseHandler);
        this.headers = Objects.requireNonNull(headers);
    }

    /**
     * Queries the server and returns an Authenticate property list.
     *
     * @param client
     * @param id
     * @param password
     * @return Authenticate property list, not null
     * @throws AuthenticationException
     * @throws BadDataException
     * @throws IOException
     */
    public SimplePropertyList get(HttpClient client, String id, String password)
            throws AuthenticationException, BadDataException, IOException {

        logger.trace("<< get() < id: {} password: {}", id, password);

        try {
            String authBasic = headers.basicToken(id, password);
            logger.debug("-- get() > auth header: {}", authBasic);

            HttpGet get = new HttpGet("https://setup.icloud.com/setup/authenticate/$APPLE_ID$");
            get.addHeader(headers.mmeClientInfo());
            get.addHeader(headers.authorization(authBasic));
            byte[] data = client.execute(get, byteArrayResponseHandler);

            SimplePropertyList propertyList = SimplePropertyList.from(data);

            logger.trace(">> get() > {}", propertyList);
            return propertyList;

        } catch (HttpResponseException ex) {
            if (ex.getStatusCode() == 401) {
                throw new AuthenticationException("Bad appleId/ password or not an iCloud account");
            }
            throw ex;
        }
    }
}
