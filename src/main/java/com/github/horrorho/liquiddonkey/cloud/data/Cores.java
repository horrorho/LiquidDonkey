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

import com.dd.plist.NSDictionary;
import com.github.horrorho.liquiddonkey.cloud.client.DataClient;
import static com.github.horrorho.liquiddonkey.cloud.data.PropertyLists.get;
import static com.github.horrorho.liquiddonkey.cloud.data.PropertyLists.parse;
import static com.github.horrorho.liquiddonkey.cloud.data.PropertyLists.string;
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

        byte[] data = propertyListClient.get(client, auth.dsPrsID(), auth.mmeAuthToken(), SETUP_URL);
        Core instance = from(data);

        if (!instance.dsPrsID().equals(auth.dsPrsID())) {
            logger.error("-- from() > dsPrsID mismatch, settings: {}, auth: {}", instance.dsPrsID(), auth.dsPrsID());
        }

        logger.trace(">> from() > core: {}", instance);
        return instance;
    }

    public static Core from(byte[] data) throws BadDataException {
        logger.trace("<< from() < bytes: {}", data.length);

        NSDictionary settings = parse(data);
        logger.debug("-- from() > settings: {}", settings.toASCIIPropertyList());

        NSDictionary appleAccountInfo = get(settings, "appleAccountInfo");
        String dsPrsID = string(appleAccountInfo, "dsPrsID");
        String fullName = string(appleAccountInfo, "fullName", "N/A");
        String appleId = string(appleAccountInfo, "appleId", "N/A");

        NSDictionary tokens = get(settings, "tokens");
        String mmeAuthToken = string(tokens, "mmeAuthToken");

        NSDictionary mobileMe = get(settings, "com.apple.mobileme");

        NSDictionary dataClassBackup = get(mobileMe, "com.apple.Dataclass.Backup");
        String mobileBackupUrl = string(dataClassBackup, "url");

        NSDictionary dataClassContent = get(mobileMe, "com.apple.Dataclass.Content");
        String contentUrl = string(dataClassContent, "url");

        NSDictionary dataClassQuota = get(mobileMe, "com.apple.Dataclass.Quota");
        String quotaInfoURL = string(dataClassQuota, "quotaInfoURL", "");
        String quotaUpdateURL = string(dataClassQuota, "quotaUpdateURL", "");
        String storageInfoURL = string(dataClassQuota, "storageInfoURL", "");

        Core instance = new Core(
                dsPrsID,
                mmeAuthToken,
                contentUrl,
                mobileBackupUrl,
                appleId,
                fullName,
                quotaInfoURL,
                quotaUpdateURL,
                storageInfoURL);

        logger.trace(">> from() > core: {}", instance);
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(Cores.class);

    private static final DataClient propertyListClient = DataClient.create();
    private static final String SETUP_URL = "https://setup.icloud.com/setup/get_account_settings";
}
