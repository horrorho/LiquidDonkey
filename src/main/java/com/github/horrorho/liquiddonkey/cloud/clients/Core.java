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
package com.github.horrorho.liquiddonkey.cloud.clients;

import com.github.horrorho.liquiddonkey.cloud.data.Auth;
import com.github.horrorho.liquiddonkey.cloud.data.Settings;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.iofunction.IOBiFunction;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core.
 * <p>
 * Core Settings and Auth data for clients.
 *
 * @author Ahseya
 */
public class Core {

    public static Core from(Http http, Authenticator authenticator)
            throws AuthenticationException, BadDataException, IOException, InterruptedException {

        Settings settings = SettingsClient.from(authenticator).get(http);

        String settingsDsPrsID = settings.dsPrsID();
        String authenticatorDsPrsID = authenticator.dsPrsID(http);

        if (!settingsDsPrsID.equals(authenticatorDsPrsID)) {
            // Shouldn't happen
            logger.error("-- from() > mismatched dsPrsID settings: {} authenticator: {}",
                    settingsDsPrsID, authenticatorDsPrsID);
        }

        return new Core(settingsDsPrsID, settings, authenticator);
    }
    private static final Logger logger = LoggerFactory.getLogger(Core.class);

    private final String dsPrsID;
    private final Settings settings;
    private final Authenticator authenticator;

    Core(String dsPrsID, Settings settings, Authenticator authenticator) {
        this.dsPrsID = dsPrsID;
        this.settings = settings;
        this.authenticator = authenticator;
    }

    public String dsPrsID() {
        return dsPrsID;
    }

    public <T> T process(Http http, String dsPrsID, IOBiFunction<Auth, Settings, T> function)
            throws AuthenticationException, BadDataException, IOException, InterruptedException {

        if (!dsPrsID.equals(this.dsPrsID)) {
            // TODO should be an IllegalStateException
            logger.error("-- from() > mismatched dsPrsID core: {} requested: {}", dsPrsID, this.dsPrsID);
        }

        return authenticator.process(http, dsPrsID, auth -> function.apply(auth, settings));
    }

}
