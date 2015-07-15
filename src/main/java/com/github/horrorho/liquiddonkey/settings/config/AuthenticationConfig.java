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
package com.github.horrorho.liquiddonkey.settings.config;

import com.github.horrorho.liquiddonkey.settings.Property;
import com.github.horrorho.liquiddonkey.settings.props.Props;

/**
 * Authentication configuration.
 *
 * @author Ahseya
 */
public class AuthenticationConfig {

    public static AuthenticationConfig from(Props<Property> props) {
        String appleId = props.get(Property.AUTHENTICATION_APPLEID);
        String password = props.get(Property.AUTHENTICATION_PASSWORD);
        String token = props.get(Property.AUTHENTICATION_TOKEN);

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

    private final String appleId;
    private final String password;
    private final String dsPrsId;
    private final String mmeAuthToken;

    AuthenticationConfig(String appleId, String password, String dsPrsId, String mmeAuthToken) {
        this.appleId = appleId;
        this.password = password;
        this.dsPrsId = dsPrsId;
        this.mmeAuthToken = mmeAuthToken;
    }

    public boolean hasAppleIdPassword() {
        return appleId != null && password != null;
    }

    public boolean hasAuthToken() {
        return dsPrsId != null && mmeAuthToken != null;
    }

    public boolean isNull() {
        return !hasAppleIdPassword() && !hasAuthToken();
    }

    public String appleId() {
        return appleId;
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
                + "appleId=" + appleId
                + ", password=" + password
                + ", dsPrsID=" + dsPrsId
                + ", mmeAuthToken=" + mmeAuthToken
                + '}';
    }
}
