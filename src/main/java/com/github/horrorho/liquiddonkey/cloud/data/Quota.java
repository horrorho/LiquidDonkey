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

import com.github.horrorho.liquiddonkey.cloud.client.DataClient;
import com.github.horrorho.liquiddonkey.exception.BadDataException; 
import java.io.IOException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quota.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Quota {

    // Incomplete
    public static void from(HttpClient client, Core core, String mmeAuthToken) throws BadDataException, IOException {
        logger.trace("<< from() < dsPrsID: {}", core.dsPrsID());

//        if (!core.quotaInfoURL().isEmpty()) {
//            SimplePropertyList quotaInfo = from(client, core.dsPrsID(), mmeAuthToken, core.quotaInfoURL());
//            logger.debug("<< from() > quotaInfo: {}", quotaInfo);
//        }
//
//        if (!core.quotaUpdateURL().isEmpty()) {
//            SimplePropertyList quotaUpdate = from(client, core.dsPrsID(), mmeAuthToken, core.quotaUpdateURL());
//            logger.debug("<< from() > quotaInfo: {}", quotaUpdate);
//
//        }
//
//        if (!core.storageInfoURL().isEmpty()) {
//            SimplePropertyList storageInfo = from(client, core.dsPrsID(), mmeAuthToken, core.storageInfoURL());
//            logger.debug("<< from() > storageInfo: {}", storageInfo);
//        }

        logger.trace("from()");
    }

//    static SimplePropertyList from(HttpClient client, String dsPrsID, String mmeAuthToken, String url) {
//        if (url.isEmpty()) {
//            return null;
//        }
//
//        try {
//            // May throw a service unavailable exception
//            return propertyListClient.get(client, dsPrsID, mmeAuthToken, url);
//        } catch (IOException ex) {
//            logger.warn("--from() > exception: {}", ex.getMessage());
//            return null;
//        }
//    }

    private static final Logger logger = LoggerFactory.getLogger(Quota.class);

    private static final DataClient propertyListClient = DataClient.create();

}
