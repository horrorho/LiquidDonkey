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
@Immutable
@ThreadSafe
public final class AuthenticationConfig {

    public static AuthenticationConfig newInstance(Configuration configuration) {
        return AuthenticationConfig.newInstance(
                configuration.getOrDefault(Property.AUTHENTICATION_APPLEID, null),
                configuration.getOrDefault(Property.AUTHENTICATION_PASSWORD, null),
                configuration.getOrDefault(Property.AUTHENTICATION_TOKEN, null));
    }

    public static AuthenticationConfig newInstance(String id, String password, String authenticationToken) {
        return new AuthenticationConfig(id, password, authenticationToken);
    }

    private final String id;
    private final String password;
    private final String authenticationToken;

    private AuthenticationConfig(String id, String password, String authenticationToken) {
        this.id = id;
        this.password = password;
        this.authenticationToken = authenticationToken;
    }

    public String id() {
        return id;
    }

    public String password() {
        return password;
    }

    public String authenticationToken() {
        return authenticationToken;
    }

    @Override
    public String toString() {
        return "AuthenticationConfig{" + "id=" + id + ", password=" + password + ", authenticationToken="
                + authenticationToken + '}';
    }
}
