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
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import java.io.IOException;
import java.util.Objects;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SettingsClient.
 *
 * @author Ahseya
 */
public class SettingsClient {

    public static SettingsClient from(Authenticator authenticator) {
        return new SettingsClient(defaultByteArrayResponseHandler, Headers.create(), authenticator);
    }

    private static final Logger logger = LoggerFactory.getLogger(SettingsClient.class);

    private static final ResponseHandler<byte[]> defaultByteArrayResponseHandler = ResponseHandlerFactory.toByteArray();

    private final ResponseHandler<byte[]> byteArrayResponseHandler;
    private final Headers headers;
    private final Authenticator authenticator;

    SettingsClient(ResponseHandler<byte[]> byteArrayResponseHandler, Headers headers, Authenticator authenticator) {
        this.byteArrayResponseHandler = Objects.requireNonNull(byteArrayResponseHandler);
        this.headers = Objects.requireNonNull(headers);
        this.authenticator = Objects.requireNonNull(authenticator);
    }

    /**
     * Queries the server and returns settings.
     *
     * @param http, not null
     * @return settings, not null
     * @throws IOException
     * @throws BadDataException
     * @throws AuthenticationException
     * @throws InterruptedException
     */
    public Settings get(Http http)
            throws AuthenticationException, BadDataException, InterruptedException, IOException {

        logger.trace("<< from()");

        byte[] data = authenticator.process(http, auth -> {

            String authToken = Tokens.create().basic(auth.dsPrsId(), auth.mmeAuthToken());
            return http.executor("https://setup.icloud.com/setup/get_account_settings", byteArrayResponseHandler)
                    .headers(headers.mmeClientInfo(), headers.authorization(authToken))
                    .get();
        });

        SimplePropertyList propertyList = SimplePropertyList.from(data);

        Settings settings = Settings.from(propertyList);

        logger.trace(">> from() > {}", settings);
        return settings;
    }
}
