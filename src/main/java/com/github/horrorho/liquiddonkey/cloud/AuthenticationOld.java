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
import com.github.horrorho.liquiddonkey.cloud.client.Headers;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig.AuthenticationConfigAppleIdPassword;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig.AuthenticationConfigAuthorizationToken;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig.AuthenticationConfigNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
public final class AuthenticationOld {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationOld.class);

    // Thread safe.
    private static final ResponseHandler<byte[]> byteArrayResponseHandler = ResponseHandlerFactory.toByteArray();

    public static AuthenticationOld from(Http http, AuthenticationConfig config) {

        try {
            return doAuthentication(http, config);

        } catch (HttpResponseException ex) {
            logger.warn("-- authenticate() >  HttpResponseException", ex);
            if (ex.getStatusCode() == 401) {
                throw new AuthenticationException("Bad details or not an iCloud account.", ex);
            }
            if (ex.getStatusCode() == 409) {
                throw new AuthenticationException("Two-step authentication or not an iCloud account.", ex);
            }
            throw new AuthenticationException(ex);
        } catch (IOException | BadDataException ex) {
            throw new AuthenticationException(ex);
        }
    }

    static AuthenticationOld doAuthentication(Http http, AuthenticationConfig config)
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

    static AuthenticationOld doAuthentication(Http http, AuthenticationConfigAppleIdPassword config)
            throws IOException, BadDataException {

        return phaseOne(http, config.id(), config.password());
    }

    static AuthenticationOld doAuthentication(Http http, AuthenticationConfigAuthorizationToken config)
            throws IOException, BadDataException {

        return phaseTwo(http, config.dsPrsId(), config.mmeAuthToken());
    }

    static AuthenticationOld phaseOne(Http http, String id, String password) throws IOException, BadDataException {
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

    static AuthenticationOld phaseTwo(Http http, String dsPrsID, String mmeAuthToken) throws IOException, BadDataException {
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
            String mobileBackupUrl
                    = PropertyLists.stringValue(settings, "com.apple.mobileme", "com.apple.Dataclass.Backup", "url");
            String contentUrl
                    = PropertyLists.stringValue(settings, "com.apple.mobileme", "com.apple.Dataclass.Content", "url");

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

            AuthenticationOld authentication
                    = newInstance(appleId, fullName, dsPrsID, authMme, contentUrl, mobileBackupUrl);

            logger.trace(">> phaseTwo() > {}", authentication);
            return authentication;
            
        } catch (PropertyListFormatException ex) {
            throw new BadDataException("Unexpected authentication data.", ex);
        }
    }

    static String authorization(String type, String left, String right) {
        return type + " " + Base64.getEncoder().encodeToString((left + ":" + right).getBytes(StandardCharsets.UTF_8));
    }

    public static AuthenticationOld newInstance(
            String appleId,
            String fullName,
            String dsPrsID,
            String authMme,
            String contentUrl,
            String mobileBackupUrl) {

        return new AuthenticationOld(
                appleId,
                fullName,
                dsPrsID,
                authMme,
                contentUrl,
                mobileBackupUrl);
    }

    private final String appleId;
    private final String fullName;
    private final String dsPrsID;
    private final String authMme;
    private final String contentUrl;
    private final String mobileBackupUrl;

    AuthenticationOld(
            String appleId,
            String fullName,
            String dsPrsID,
            String authMme,
            String contentUrl,
            String mobileBackupUrl) {

        this.appleId = Objects.requireNonNull(appleId);
        this.fullName = Objects.requireNonNull(fullName);
        this.dsPrsID = Objects.requireNonNull(dsPrsID);
        this.authMme = Objects.requireNonNull(authMme);
        this.contentUrl = Objects.requireNonNull(contentUrl);
        this.mobileBackupUrl = Objects.requireNonNull(mobileBackupUrl);

        logger.trace("** Authentication() < appleId:", appleId);
        logger.trace("** Authentication() < fullName:", fullName);
        logger.trace("** Authentication() <  dsPrsID", dsPrsID);
        logger.trace("** Authentication() <  authMme", authMme);
        logger.trace("** Authentication() <  contentUrl", contentUrl);
        logger.trace("** Authentication() <  mobileBackupUrl", mobileBackupUrl);
    }

    public String appleId() {
        return appleId;
    }

    public String authMme() {
        return authMme;
    }

    public String contentUrl() {
        return contentUrl;
    }

    public String dsPrsID() {
        return dsPrsID;
    }

    public String fullName() {
        return fullName;
    }

    public String mobileBackupUrl() {
        return mobileBackupUrl;
    }

    @Override
    public String toString() {
        return "Authentication{" + "appleId=" + appleId + ", fullName=" + fullName + ", dsPrsID=" + dsPrsID
                + ", authMme=" + authMme + ", contentUrl=" + contentUrl + ", mobileBackupUrl=" + mobileBackupUrl + '}';
    }
}
