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

import com.github.horrorho.liquiddonkey.cloud.clients.*;
import com.github.horrorho.liquiddonkey.data.SimplePropertyList;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import java.io.IOException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Settings {

    public static Settings from(SimplePropertyList settings) throws BadDataException {
        logger.trace("<< from() < settings: {}", settings);

        String fullName = settings.valueOr("Unknown", "appleAccountInfo", "fullName");
        String appleId = settings.valueOr("Unknown", "appleAccountInfo", "appleId");
        String mobileBackupUrl = settings.value("com.apple.mobileme", "com.apple.Dataclass.Backup", "url");
        String contentUrl = settings.value("com.apple.mobileme", "com.apple.Dataclass.Content", "url");

        Settings instance = new Settings(contentUrl, mobileBackupUrl, appleId, fullName);

        logger.trace(">> from() > {}", instance);
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
