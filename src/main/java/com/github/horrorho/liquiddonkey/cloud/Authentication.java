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
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig;
import com.github.horrorho.liquiddonkey.util.PropertyLists;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListFormatException;
import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.cloud.client.Headers;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig.AuthenticationConfigAppleIdPassword;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig.AuthenticationConfigAuthorizationToken;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig.AuthenticationConfigNull;
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
            return doAuthentication(http, config);

        } catch (HttpResponseException ex) {
            logger.warn("-- authenticate() >  HttpResponseException", ex);
            if (ex.getStatusCode() == 401) {
                throw new AuthenticationException("Bad details/ not an iCloud account.", ex);
            }
            if (ex.getStatusCode() == 409) {
                throw new AuthenticationException("Is two-step authentication enabled?", ex);
            }
            throw new AuthenticationException(ex);
        } catch (IOException | BadDataException ex) {
            throw new AuthenticationException(ex);
        }
    }

    static Authentication doAuthentication(Http http, AuthenticationConfig config)
            throws IOException, BadDataException {

        if (config instanceof AuthenticationConfigNull) {
            throw new IllegalArgumentException("No appleid/ password or authentication token suppled.");
        }

        if (config instanceof AuthenticationConfigAppleIdPassword) {
            return doAuthentication(http, (AuthenticationConfigAppleIdPassword) config);

        }

        if (config instanceof AuthenticationConfigAuthorizationToken) {
            return doAuthentication(http, (AuthenticationConfigAuthorizationToken) config);
        }

        throw new IllegalArgumentException("Unknown configuration class: "
                + (config == null ? null : config.getClass()));
    }

    static Authentication doAuthentication(Http http, AuthenticationConfigAppleIdPassword config)
            throws IOException, BadDataException {

        return phaseOne(http, config.id(), config.password());
    }

    static Authentication doAuthentication(Http http, AuthenticationConfigAuthorizationToken config)
            throws IOException, BadDataException {

        return phaseTwo(http, config.dsPrsId(), config.mmeAuthToken());
    }

    static Authentication phaseOne(Http http, String id, String password) throws IOException, BadDataException {

        try {
            logger.trace("<< phaseOne() < id: {} password: {}", id, password);

            String authBasic = authorization("Basic", id, password);
            logger.trace("-- phaseOne() > authentication basic token: {}", authBasic);

            NSDictionary response = (NSDictionary) PropertyLists.parse(
                    http.executor("https://setup.icloud.com/setup/authenticate/$APPLE_ID$", byteArrayResponseHandler)
                    .headers(Headers.mmeClientInfo, Headers.authorization(authBasic))
                    .get());
            logger.trace("-- phaseOne() >  response: {}", response.toASCIIPropertyList());

            String dsPrsID = PropertyLists.stringValue(response, "appleAccountInfo", "dsPrsID");
            logger.trace("-- phaseOne() >  dsPrsID: {}", dsPrsID);
            String mmeAuthToken = PropertyLists.stringValue(response, "tokens", "mmeAuthToken");
            logger.trace("-- phaseOne() >   mmeAuthToken: {}", mmeAuthToken);

            logger.trace(">> phaseOne() >> phaseTwo. dsPrsID: {} mmeAuthToken: {}", dsPrsID, mmeAuthToken);
            return phaseTwo(http, dsPrsID, mmeAuthToken);

        } catch (PropertyListFormatException ex) {
            throw new BadDataException("Unexpected authentication data.", ex);
        }
    }

    static Authentication phaseTwo(Http http, String dsPrsID, String mmeAuthToken) throws IOException, BadDataException {
        try {
            logger.trace("<< phaseTwo() < dsPrsID: {} mmeAuthToken: {}", dsPrsID, mmeAuthToken);

            String auth = authorization("Basic", dsPrsID, mmeAuthToken);
            logger.trace("-- phaseTwo() >  authentication token: {}", auth);

            NSDictionary settings = (NSDictionary) PropertyLists.parse(
                    http.executor("https://setup.icloud.com/setup/get_account_settings", byteArrayResponseHandler)
                    .headers(Headers.mmeClientInfo, Headers.authorization(auth))
                    .get());
            logger.trace("-- phaseTwo() >  account settings: {}", settings.toASCIIPropertyList());

            String fullName = PropertyLists.stringValueOrDefault("Unknown", settings, "appleAccountInfo", "fullName");
            String appleId = PropertyLists.stringValueOrDefault("Unknown", settings, "appleAccountInfo", "appleId");

            String newDsPrsID = PropertyLists.stringValue(settings, "appleAccountInfo", "dsPrsID");
            String newMmeAuthToken = PropertyLists.stringValue(settings, "tokens", "mmeAuthToken");

            if (!dsPrsID.equals(newDsPrsID)) {
                logger.warn("-- phaseTwo() > dsPrsID overwritten {} > {}", dsPrsID, newDsPrsID);
                dsPrsID = newDsPrsID;
            }

            if (!mmeAuthToken.equals(newMmeAuthToken)) {
                logger.warn("-- phaseTwo() > mmeAuthToken overwritten {} > {}", mmeAuthToken, newMmeAuthToken);
                mmeAuthToken = newMmeAuthToken;
            }

            String authMme = authorization("X-MobileMe-AuthToken", dsPrsID, mmeAuthToken);
            logger.trace("-- phaseTwo() >  authentication x-MobileMetoken token: {}", authMme);

            Client client = new Client(http, settings, dsPrsID, authMme, 4096); //TODO
            Authentication authentication = newInstance(client, appleId, fullName);

            logger.trace(">> phaseTwo() > {}", authentication);
            return authentication;

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

    @Override
    public String toString() {
        return "Authentication{" + "client=" + client + ", appleId=" + appleId + ", fullName=" + fullName + '}';
    }
}
