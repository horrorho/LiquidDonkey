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
package com.github.horrorho.liquiddonkey.cloud.data;

import com.github.horrorho.liquiddonkey.cloud.client.SettingsClient;
import com.github.horrorho.liquiddonkey.util.SimplePropertyList;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import java.io.IOException;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cores.
 *
 * @author Ahseya
 */
public class Cores {

    public static Core from(HttpClient client, Auth auth) throws BadDataException, IOException {
        logger.trace("<< from() < auth: {}", auth);

        SimplePropertyList propertyList = settingsClient.get(client, auth.dsPrsID(), auth.mmeAuthToken());
        Core settings = from(propertyList);

        if (!settings.dsPrsID().equals(auth.dsPrsID())) {
            logger.error("-- from() > dsPrsID mismatch, settings: {}, auth: {}", settings.dsPrsID(), auth.dsPrsID());
        }

        logger.trace(">> from() > {}", settings);
        return settings;
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

        logger.trace(">> from() > {}", instance);
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(Cores.class);

    private static final SettingsClient settingsClient = SettingsClient.create();
}
