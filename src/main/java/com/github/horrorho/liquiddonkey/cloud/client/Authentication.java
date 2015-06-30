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
package com.github.horrorho.liquiddonkey.cloud.client;

import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.util.PropertyLists;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListFormatException;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import net.jcip.annotations.Immutable;
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
@Immutable
@ThreadSafe
public final class Authentication {

    /**
     * Returns a new Authentication.
     *
     * @param http, not null
     * @param config, not null
     * @return Authentication, not null
     * @throws IOException
     * @throws AuthenticationException
     * @throws UncheckedIOException
     */
    public static Authentication authenticate(Http http, AuthenticationConfig config) throws IOException {
        if (config instanceof AuthenticationConfig.AuthorizationToken) {
            return authenticate((AuthenticationConfig.AuthorizationToken) config);
        }

        if (config instanceof AuthenticationConfig.AppleIdPassword) {
            return authenticate(http, (AuthenticationConfig.AppleIdPassword) config);
        }

        if (config instanceof AuthenticationConfig.Null) {
            throw new AuthenticationException("No authorization data");
        }

        throw new AuthenticationException();
    }

    static Authentication authenticate(AuthenticationConfig.AuthorizationToken config)
            throws IOException {

        return newInstance(config.dsPrsId(), config.mmeAuthToken());
    }

    static Authentication authenticate(Http http, AuthenticationConfig.AppleIdPassword config)
            throws IOException {

        return authenticate(http, config.id(), config.password());
    }

    /**
     * Returns a new Authentication.
     *
     * @param http, not null
     * @param appleId, not null
     * @param password, not null
     * @return Authentication, not null
     * @throws AuthenticationException
     * @throws UncheckedIOException
     */
    public static Authentication authenticate(Http http, String appleId, String password) {
        try {
            logger.trace("<< authenticate() < id: {} password: {}", appleId, password);

            String authBasic = Tokens.getInstance().basic(appleId, password);
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

            Authentication authentication = newInstance(dsPrsID, mmeAuthToken);
            logger.trace(">> authenticate() > authentication: {}", authentication);

            return authentication;

        } catch (IOException | BadDataException | PropertyListFormatException ex) {
            throw new AuthenticationException("Unexpected server response", ex);
        } catch (UncheckedIOException ex) {
            IOException ioex = ex.getCause();
            if (ioex instanceof HttpResponseException) {
                if (((HttpResponseException) ioex).getStatusCode() == 401) {
                    throw new AuthenticationException("Bad appleId/ password or not an iCloud account", ex);
                }
            }
            throw ex;
        } catch (AuthenticationException ex) {
            throw new AuthenticationException("Bad appleId/ password or not an iCloud account", ex);
        }
    }

    /**
     * Returns a new Authentication.
     *
     * @param dsPrsID, not null
     * @param mmeAuthToken, not null
     * @return Authentication, not null
     */
    public static Authentication newInstance(String dsPrsID, String mmeAuthToken) {
        return new Authentication(dsPrsID, mmeAuthToken);
    }

    private static final Logger logger = LoggerFactory.getLogger(Authentication.class);

    // Thread safe.
    private static final ResponseHandler<byte[]> byteArrayResponseHandler = ResponseHandlerFactory.toByteArray();

    private final String dsPrsID;
    private final String mmeAuthToken;

    Authentication(String dsPrsID, String mmeAuthToken) {
        this.dsPrsID = Objects.requireNonNull(dsPrsID);
        this.mmeAuthToken = Objects.requireNonNull(mmeAuthToken);
    }

    public String mmeAuthToken() {
        return mmeAuthToken;
    }

    public String dsPrsID() {
        return dsPrsID;
    }

    @Override
    public String toString() {
        return "Authentication{" + "dsPrsID=" + dsPrsID + ", mmeAuthToken=" + mmeAuthToken + '}';
    }
}
