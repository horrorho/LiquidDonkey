/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a flatCopy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, flatCopy, modify, merge, publish, distribute, sublicense, and/or sell
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
package com.github.horrorho.liquiddonkey.settings.config;

import com.github.horrorho.liquiddonkey.util.Props;
import com.github.horrorho.liquiddonkey.settings.Property; 
import java.util.Properties;

/**
 * Authentication configuration.
 *
 * @author Ahseya
 */
public class AuthenticationConfig {

    public static AuthenticationConfig from(Properties properties) {
        Props<Property> props = Props.from(properties);
        
        String appleId = props.getProperty(Property.AUTHENTICATION_APPLEID);
        String password = props.getProperty(Property.AUTHENTICATION_PASSWORD);
        String token = props.getProperty(Property.AUTHENTICATION_TOKEN);

        if (appleId != null && password != null && token != null) {
            throw new IllegalStateException("Expected authorization token or appleid/ password only.");
        }

        String dsPrsID;
        String mmeAuthToken;
        if (token != null) {
            String[] split = token.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Invalid authentication token format.");
            }
            dsPrsID = split[0];
            mmeAuthToken = split[1];
        } else {
            dsPrsID = null;
            mmeAuthToken = null;
        }

        return new AuthenticationConfig(appleId, password, dsPrsID, mmeAuthToken);
    }

    private final String id;
    private final String password;
    private final String dsPrsId;
    private final String mmeAuthToken;

    AuthenticationConfig(String id, String password, String dsPrsId, String mmeAuthToken) {
        this.id = id;
        this.password = password;
        this.dsPrsId = dsPrsId;
        this.mmeAuthToken = mmeAuthToken;
    }

    public boolean hasIdPassword() {
        return id != null && password != null;
    }

    public boolean hasAuthToken() {
        return dsPrsId != null && mmeAuthToken != null;
    }

    public boolean isNull() {
        return !hasIdPassword() && !hasAuthToken();
    }

    public String id() {
        return id;
    }

    public String password() {
        return password;
    }

    public String dsPrsId() {
        return dsPrsId;
    }

    public String mmeAuthToken() {
        return mmeAuthToken;
    }

    @Override
    public String toString() {
        return "AuthenticationConfig{"
                + "appleId=" + id
                + ", password=" + password
                + ", dsPrsID=" + dsPrsId
                + ", mmeAuthToken=" + mmeAuthToken
                + '}';
    }
}
