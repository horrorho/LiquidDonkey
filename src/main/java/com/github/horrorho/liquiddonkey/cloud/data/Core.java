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
package com.github.horrorho.liquiddonkey.cloud.data;

import com.github.horrorho.liquiddonkey.cloud.client.SettingsClient;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.util.SimplePropertyList;
import java.io.IOException;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core.
 * <p>
 * Core settings.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public class Core {

    public static Core from(HttpClient client, Auth auth) throws BadDataException, IOException {
        logger.trace("<< from() < auth: {}", auth);

        SimplePropertyList propertyList = settingsClient.get(client, auth.dsPrsID(), auth.mmeAuthToken());
        Core instance = Core.from(propertyList);

        if (!instance.dsPrsID().equals(auth.dsPrsID())) {
            logger.error("-- from() > dsPrsID mismatch, settings: {}, auth: {}", instance.dsPrsID(), auth.dsPrsID());
        }

        logger.trace(">> from() > core: {}", instance);
        return instance;
    }

    public static Core from(SimplePropertyList settings) throws BadDataException {
        logger.trace("<< from() < property list : {}", settings);

        String dsPrsID = settings.value("appleAccountInfo", "dsPrsID");
        String mmeAuthToken = settings.value("tokens", "mmeAuthToken");
        String fullName = settings.defaultOr("Unknown", "appleAccountInfo", "fullName");
        String appleId = settings.defaultOr("Unknown", "appleAccountInfo", "appleId");
        String mobileBackupUrl = settings.value("com.apple.mobileme", "com.apple.Dataclass.Backup", "url");
        String contentUrl = settings.value("com.apple.mobileme", "com.apple.Dataclass.Content", "url");

        Core instance = new Core(dsPrsID, mmeAuthToken, contentUrl, mobileBackupUrl, appleId, fullName);

        logger.trace(">> from() > core: {}", instance);
        return instance;
    }

    public static Core from(Core core, String mmeAuthToken) {
        logger.trace("<< from() < mmeAuthToken: {}", mmeAuthToken);

        Core instance = new Core(
                core.dsPrsID(),
                mmeAuthToken,
                core.contentUrl(),
                core.mobileBackupUrl(),
                core.appleId(),
                core.fullName());

        logger.trace(">> frome() > core: {}", instance);
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(Core.class);

    private static final SettingsClient settingsClient = SettingsClient.create();

    private final String dsPrsID;
    private final String mmeAuthToken;
    private final String contentUrl;
    private final String mobileBackupUrl;
    private final String appleId;
    private final String fullName;

    Core(
            String dsPrsID,
            String mmeAuthToken,
            String contentUrl,
            String mobileBackupUrl,
            String appleId,
            String fullName) {

        this.dsPrsID = Objects.requireNonNull(dsPrsID);
        this.mmeAuthToken = Objects.requireNonNull(mmeAuthToken);
        this.contentUrl = Objects.requireNonNull(contentUrl);
        this.mobileBackupUrl = Objects.requireNonNull(mobileBackupUrl);
        this.appleId = Objects.requireNonNull(appleId);
        this.fullName = Objects.requireNonNull(fullName);
    }

    Core(Core settings) {
        this(
                settings.dsPrsID(),
                settings.mmeAuthToken(),
                settings.contentUrl(),
                settings.mobileBackupUrl(),
                settings.appleId(),
                settings.fullName());
    }

    public String dsPrsID() {
        return dsPrsID;
    }

    public String mmeAuthToken() {
        return mmeAuthToken;
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
        return "Core{"
                + "dsPrsID=" + dsPrsID
                + ", mmeAuthToken=" + mmeAuthToken
                + ", contentUrl=" + contentUrl
                + ", mobileBackupUrl=" + mobileBackupUrl
                + ", appleId=" + appleId
                + ", fullName=" + fullName
                + '}';
    }
}
