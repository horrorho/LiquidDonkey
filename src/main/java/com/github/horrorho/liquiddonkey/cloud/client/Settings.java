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

import com.github.horrorho.liquiddonkey.data.SimplePropertyList;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import java.io.IOException;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings.
 *
 * @author Ahseya
 */
public class Settings {

    public static Settings of(Http http, Auth auth) throws IOException, BadDataException {

        String authToken = Tokens.create().basic(auth.dsPrsId(), auth.mmeAuthToken());
        Headers headers = Headers.create();

        byte[] data
                = http.executor("https://setup.icloud.com/setup/get_account_settings", byteArrayResponseHandler)
                .headers(headers.mmeClientInfo(), headers.authorization(authToken))
                .get();
        SimplePropertyList settings = SimplePropertyList.from(data);

        return of(settings);
    }

    public static Settings of(SimplePropertyList settings) throws BadDataException {
        logger.trace("<< of()");

        String fullName = settings.valueOr("Unknown", "appleAccountInfo", "fullName");
        String appleId = settings.valueOr("Unknown", "appleAccountInfo", "appleId");
        String mobileBackupUrl = settings.value("com.apple.mobileme", "com.apple.Dataclass.Backup", "url");
        String contentUrl = settings.value("com.apple.mobileme", "com.apple.Dataclass.Content", "url");

        Settings instance = new Settings(contentUrl, mobileBackupUrl, appleId, fullName);

        logger.trace(">> of() > {}", instance);
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);

    // Thread safe
    private static final ResponseHandler<byte[]> byteArrayResponseHandler = ResponseHandlerFactory.toByteArray();

    private final String contentUrl;
    private final String mobileBackupUrl;
    private final String appleId;
    private final String fullName;

    Settings(String contentUrl, String mobileBackupUrl, String appleId, String fullName) {
        this.contentUrl = contentUrl;
        this.mobileBackupUrl = mobileBackupUrl;
        this.appleId = appleId;
        this.fullName = fullName;
    }

    public String contentUrl() {
        return contentUrl;
    }

    public String mobileBackupUrl() {
        return mobileBackupUrl;
    }

    public String appleId() {
        return appleId;
    }

    public String fullName() {
        return fullName;
    }

    @Override
    public String toString() {
        return "Settings{"
                + "contentUrl=" + contentUrl
                + ", mobileBackupUrl=" + mobileBackupUrl
                + ", appleId=" + appleId
                + ", fullName=" + fullName
                + '}';
    }
}
