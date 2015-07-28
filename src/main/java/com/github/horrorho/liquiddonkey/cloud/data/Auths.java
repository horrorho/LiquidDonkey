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

import com.github.horrorho.liquiddonkey.cloud.client.AuthClient;
import com.github.horrorho.liquiddonkey.util.SimplePropertyList;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import java.io.IOException;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
public class Auths {

    public static Auth from(String dsPrsID, String mmeAuthToken) {
        logger.trace("<< from() < dsPrsID: {} mmeAuthToken: {}", dsPrsID, mmeAuthToken);

        Auth auth = new Auth(dsPrsID, mmeAuthToken);

        logger.trace(">> from > {}", auth);
        return auth;
    }

    public static Auth from(HttpClient client, String id, String password) throws BadDataException, IOException {
        logger.trace("<< from() < id: {} password: {}", id, password);

        SimplePropertyList propertyList = authClient.get(client, id, password);

        String dsPrsID = propertyList.value("appleAccountInfo", "dsPrsID");
        logger.debug("-- from() >  dsPrsID: {}", dsPrsID);

        String mmeAuthToken = propertyList.value("tokens", "mmeAuthToken");
        logger.debug("-- from() >   mmeAuthToken: {}", mmeAuthToken);

        Auth auth = new Auth(dsPrsID, mmeAuthToken);

        logger.trace(">> from > {}", auth);
        return auth;
    }

    private static final Logger logger = LoggerFactory.getLogger(Auths.class);
    private static final AuthClient authClient = AuthClient.create();

}
