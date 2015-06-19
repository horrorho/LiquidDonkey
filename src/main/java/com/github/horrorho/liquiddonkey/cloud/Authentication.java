/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies from the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions from the Software.
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

import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.settings.old.Property;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig;
import com.github.horrorho.liquiddonkey.util.PropertyLists;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListFormatException;
import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.cloud.client.Headers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class Authentication {

    private static final Logger logger = LoggerFactory.getLogger(Authentication.class);

    // Thread safe.
    private static final ResponseHandler<byte[]> byteArrayResponseHandler = ResponseHandlerFactory.toByteArray();

    public static Authentication from(Http http, AuthenticationConfig config) {
        try {
            return doAuthenticate(http, config.id(), config.password());
        } catch (HttpResponseException ex) {
            logger.warn("-- authenticate() >  HttpResponseException", ex);
            if (ex.getStatusCode() == 401) {
                throw new AuthenticationException("Not an iCloud account or bad appleid/ password.", ex);
            }
            if (ex.getStatusCode() == 409) {
                throw new AuthenticationException("Is two-step authentication enabled?", ex);
            }
            throw new AuthenticationException(ex);
        } catch (IOException | BadDataException ex) {
            throw new AuthenticationException(ex);
        }
    }

    static Authentication doAuthenticate(Http http, String id, String password) throws IOException, BadDataException {

        try {
            String authBasic = authorization("Basic", id, password);
            logger.trace("-- authenticate() > authentication basic token: {}", authBasic);

            NSDictionary response = (NSDictionary) PropertyLists.parse(
                    http.executor("https://setup.icloud.com/setup/authenticate/$APPLE_ID$", byteArrayResponseHandler)
                    .headers(Headers.mmeClientInfo, Headers.authorization(authBasic))
                    .get());
            logger.trace("-- authenticate() >  response: {}", response.toASCIIPropertyList());

            String dsPrsID = PropertyLists.stringValue(response, "appleAccountInfo", "dsPrsID");
            logger.trace("-- authenticate() >  dsPrsID: {}", dsPrsID);
            String mmeAuthToken = PropertyLists.stringValue(response, "tokens", "mmeAuthToken");
            logger.trace("-- authenticate() >   mmeAuthToken: {}", mmeAuthToken);
            String auth = authorization("Basic", dsPrsID, mmeAuthToken);
            logger.trace("-- authenticate() >  authentication token: {}", auth);

            NSDictionary settings = (NSDictionary) PropertyLists.parse(
                    http.executor("https://setup.icloud.com/setup/get_account_settings", byteArrayResponseHandler)
                    .headers(Headers.mmeClientInfo, Headers.authorization(auth))
                    .get());
            logger.trace("-- authenticate() >  account settings: {}", settings.toASCIIPropertyList());

            String authMme = authorization("X-MobileMe-AuthToken", dsPrsID, mmeAuthToken);
            logger.trace("-- authenticate() >  authentication x-MobileMetoken token: {}", authMme);

            String fullName = PropertyLists.stringValue("Unknown", settings, "appleAccountInfo", "fullName");
            String appleId = PropertyLists.stringValue("Unknown", settings, "appleAccountInfo", "appleId");

            Client client = new Client(http, settings, dsPrsID, authMme, Property.Int.LIST_FILES_LIMIT.integer());

            return newInstance(client, appleId, fullName);

        } catch (PropertyListFormatException ex) {
            throw new BadDataException("Unexpected authentication data.", ex);
        }
    }

    static String authorization(String type, String left, String right) {
        return type + " " + Base64.getEncoder().encodeToString((left + ":" + right).getBytes(StandardCharsets.UTF_8));
    }

    public static Authentication newInstance(Client client, String appleId, String fullName) {
        return new Authentication(client, appleId, fullName);
    }

    private final Client client;
    private final String appleId;
    private final String fullName;

    Authentication(Client client, String appleId, String fullName) {
        this.client = client;
        this.appleId = appleId;
        this.fullName = fullName;
    }

    public Client client() {
        return client;
    }

    public String appleId() {
        return appleId;
    }

    public String fullName() {
        return fullName;
    }
}
