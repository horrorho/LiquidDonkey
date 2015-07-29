
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
package com.github.horrorho.liquiddonkey.cloud.client;

import com.github.horrorho.liquiddonkey.util.SimplePropertyList;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.ResponseHandlerFactory;
import java.io.IOException;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SettingsClient.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class PropertyListClient {

    public static PropertyListClient create() {
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(PropertyListClient.class);

    private static final PropertyListClient instance
            = new PropertyListClient(ResponseHandlerFactory.toByteArray(), Headers.create());

    private final ResponseHandler<byte[]> byteArrayResponseHandler;
    private final Headers headers;

    PropertyListClient(ResponseHandler<byte[]> byteArrayResponseHandler, Headers headers) {
        this.byteArrayResponseHandler = Objects.requireNonNull(byteArrayResponseHandler);
        this.headers = Objects.requireNonNull(headers);
    }

    /**
     * Queries the server and returns a property list.
     *
     * @param client, not null
     * @param dsPrsID, not null
     * @param mmeAuthToken, not null
     * @param url, not null
     * @return property list, not null
     * @throws IOException
     * @throws BadDataException
     */
    public SimplePropertyList get(HttpClient client, String dsPrsID, String mmeAuthToken, String url)
            throws BadDataException, IOException {

        logger.trace("<< get() < dsPrsID: {} mmeAuthToken: {}", dsPrsID, mmeAuthToken);

        String authToken = headers.basicToken(dsPrsID, mmeAuthToken);

        HttpGet get = new HttpGet(url);
        get.addHeader(headers.mmeClientInfo());
        get.addHeader(headers.authorization(authToken));
        byte[] data = client.execute(get, byteArrayResponseHandler);
        
        SimplePropertyList propertyList = SimplePropertyList.from(data);

        logger.trace(">> get() > propertyList bytes: {}", data.length);
        return propertyList;
    }
}
