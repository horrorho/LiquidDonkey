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

import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.cli.CommandLine;

/**
 * Authentication configuration.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class AuthenticationConfig {

    public static AuthenticationConfig newInstance(CommandLine cmd) {
        return AuthenticationConfig.newInstance(
                cmd.getArgList().get(0),
                cmd.getArgList().get(1));
    }

    public static AuthenticationConfig newInstance(String id, String password) {
        return new AuthenticationConfig(id, password);
    }

    private final String id;
    private final String password;

    private AuthenticationConfig(String id, String password) {
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
        return "AuthenticationConfig{" + "id=" + id + ", password=" + password + '}';
    }
}
