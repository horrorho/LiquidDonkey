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

import com.github.horrorho.liquiddonkey.settings.Configuration;
import com.github.horrorho.liquiddonkey.settings.Property;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Authentication configuration.
 *
 * @author Ahseya
 */
public class AuthenticationConfig {

    public static AuthenticationConfig newInstance(Configuration configuration) {
        String id = configuration.getOrDefault(Property.AUTHENTICATION_APPLEID, null);
        String password = configuration.getOrDefault(Property.AUTHENTICATION_PASSWORD, null);
        String token = configuration.getOrDefault(Property.AUTHENTICATION_TOKEN, null);

        if (id != null && password != null && token != null) {
            throw new IllegalStateException("Expected authorization token or appleid/ password only.");
        }

        if (id != null && password != null) {
            return fromAppleIdPassword(id, password);
        }

        if (token != null) {
            try {
                return fromAuthorizationToken(token);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Missing password or bad authentication token.");
            }
        }

        return AuthenticationConfigNull.instance;
    }

    public static AuthenticationConfigAppleIdPassword fromAppleIdPassword(String id, String password) {
        return new AuthenticationConfigAppleIdPassword(id, password);
    }

    public static AuthenticationConfigAuthorizationToken fromAuthorization(String dsPrsID, String mmeAuthToken) {
        return new AuthenticationConfigAuthorizationToken(dsPrsID, mmeAuthToken);
    }

    public static AuthenticationConfigAuthorizationToken fromAuthorizationToken(String token) {
        String[] split = token.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException("Bad authentication token.");
        }
        return fromAuthorization(split[0], split[1]);
    }

    @Immutable
    @ThreadSafe
    public static final class AuthenticationConfigAppleIdPassword extends AuthenticationConfig {

        private final String id;
        private final String password;

        AuthenticationConfigAppleIdPassword(String id, String password) {
            this.id = id;
            this.password = password;
        }

        public String id() {
            return id;
        }

        public String password() {
            return password;
        }

        @Override
        public String toString() {
            return "AuthenticationConfigIdPassword{" + "id=" + id + ", password=" + password + '}';
        }
    }

    @Immutable
    @ThreadSafe
    public static final class AuthenticationConfigAuthorizationToken extends AuthenticationConfig {

        private final String dsPrsId;
        private final String mmeAuthToken;

        AuthenticationConfigAuthorizationToken(String dsPrsId, String mmeAuthToken) {
            this.dsPrsId = dsPrsId;
            this.mmeAuthToken = mmeAuthToken;
        }

        public String dsPrsId() {
            return dsPrsId;
        }

        public String mmeAuthToken() {
            return mmeAuthToken;
        }

        @Override
        public String toString() {
            return "AuthenticationConfigAuth{" + "dsPrsId=" + dsPrsId + ", mmeAuthToken=" + mmeAuthToken + '}';
        }
    }

    @Immutable
    @ThreadSafe
    public static final class AuthenticationConfigNull extends AuthenticationConfig {

        private final static AuthenticationConfigNull instance = new AuthenticationConfigNull();

        AuthenticationConfigNull() {
        }
    }
}
