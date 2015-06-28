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
package com.github.horrorho.liquiddonkey.cloud;

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListFormatException;
import com.github.horrorho.liquiddonkey.cloud.client.Headers;
import com.github.horrorho.liquiddonkey.cloud.client.Tokens;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.settings.config.ClientConfig;
import com.github.horrorho.liquiddonkey.util.PropertyLists;
import java.io.IOException;
import java.util.Objects;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Account.
 *
 * @author Ahseya
 */
public final class Account {

    public static Account from(Http http, Authentication authentication, ClientConfig config) throws IOException {
        try {
            logger.trace("<< from() < http: {} authentication: {}", http, authentication);

            Tokens tokens = Tokens.getInstance();
            String dsPrsID = authentication.dsPrsID();
            String mmeAuthToken = authentication.mmeAuthToken();

            String auth = tokens.basic(dsPrsID, mmeAuthToken);
            logger.trace("-- from() >  authentication token: {}", auth);

            NSDictionary settings = (NSDictionary) PropertyLists.parse(
                    http.executor("https://setup.icloud.com/setup/get_account_settings", byteArrayResponseHandler)
                    .headers(Headers.mmeClientInfo, Headers.authorization(auth))
                    .get());
            logger.trace("-- from() >  account settings: {}", settings.toASCIIPropertyList());

            String fullName = PropertyLists.stringValueOrDefault("Unknown", settings, "appleAccountInfo", "fullName");
            String appleId = PropertyLists.stringValueOrDefault("Unknown", settings, "appleAccountInfo", "appleId");
            String newDsPrsID = PropertyLists.stringValue(settings, "appleAccountInfo", "dsPrsID");
            String newMmeAuthToken = PropertyLists.stringValue(settings, "tokens", "mmeAuthToken");
            String mobileBackupUrl
                    = PropertyLists.stringValue(settings, "com.apple.mobileme", "com.apple.Dataclass.Backup", "url");
            String contentUrl
                    = PropertyLists.stringValue(settings, "com.apple.mobileme", "com.apple.Dataclass.Content", "url");

            if (!dsPrsID.equals(newDsPrsID)) {
                logger.warn("-- from() > dsPrsID overwritten {} > {}", dsPrsID, newDsPrsID);
                dsPrsID = newDsPrsID;
            }

            if (!mmeAuthToken.equals(newMmeAuthToken)) {
                logger.warn("-- from() > mmeAuthToken overwritten {} > {}", mmeAuthToken, newMmeAuthToken);
                mmeAuthToken = newMmeAuthToken;
            }

            Account account = newInstance(
                    Authentication.newInstance(dsPrsID, mmeAuthToken),
                    fullName,
                    appleId,
                    contentUrl,
                    mobileBackupUrl);

            logger.trace(">> from() > account: {}", account);
            return account;

        } catch (BadDataException | PropertyListFormatException ex) {
            throw new AuthenticationException(ex);
        }
    }

    public static Account newInstance(
            Authentication authentication,
            String fullName,
            String appleId,
            String contentUrl,
            String mobileBackupUrl) {

        return new Account(
                authentication,
                appleId,
                fullName,
                contentUrl,
                mobileBackupUrl);
    }

    private static final Logger logger = LoggerFactory.getLogger(Account.class);

    // Thread safe.
    private static final ResponseHandler<byte[]> byteArrayResponseHandler = ResponseHandlerFactory.toByteArray();

    private final Authentication authentication;
    private final String fullName;
    private final String appleId;
    private final String contentUrl;
    private final String mobileBackupUrl;

    Account(
            Authentication authentication,
            String fullName,
            String appleId,
            String contentUrl,
            String mobileBackupUrl) {

        this.authentication = authentication;
        this.fullName = Objects.requireNonNull(fullName);
        this.appleId = Objects.requireNonNull(appleId);
        this.contentUrl = Objects.requireNonNull(contentUrl);
        this.mobileBackupUrl = Objects.requireNonNull(mobileBackupUrl);
    }

    public Authentication authentication() {
        return authentication;
    }

    public String appleId() {
        return appleId;
    }

    public String contentUrl() {
        return contentUrl;
    }

    public String mobileBackupUrl() {
        return mobileBackupUrl;
    }

    @Override
    public String toString() {
        return "Account{" + "authentication=" + authentication + ", fullName=" + fullName + ", appleId=" + appleId
                + ", contentUrl=" + contentUrl + ", mobileBackupUrl=" + mobileBackupUrl + '}';
    }
}
