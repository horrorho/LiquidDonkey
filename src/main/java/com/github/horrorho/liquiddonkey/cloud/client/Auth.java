/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies from the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions from the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.liquiddonkey.cloud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.Header;

/**
 * Auth.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Auth {

    public static Auth of(String dsPrsId, String mmeAuthToken) {
        return of(dsPrsId, mmeAuthToken, 0);
    }

    public static Auth of(String dsPrsId, String mmeAuthToken, long revision) {
        String authMme = Tokens.create().mobilemeAuthToken(dsPrsId, mmeAuthToken);
        Headers headers = Headers.create();
        return new Auth(
                dsPrsId,
                mmeAuthToken,
                headers.mobileBackupHeaders(authMme),
                headers.contentHeaders(dsPrsId),
                revision);
    }

    private final String dsPrsId;
    private final String mmeAuthToken;
    private final List<Header> mobileBackupHeaders;
    private final List<Header> contentHeaders;

    private final long revision;

    public Auth(
            String dsPrsId,
            String mmeAuthToken,
            List<Header> mobileBackupHeaders,
            List<Header> contentHeaders,
            long revision) {

        this.dsPrsId = Objects.requireNonNull(dsPrsId);
        this.mmeAuthToken = Objects.requireNonNull(mmeAuthToken);
        this.mobileBackupHeaders = Objects.requireNonNull(mobileBackupHeaders);
        this.contentHeaders = Objects.requireNonNull(contentHeaders);
        this.revision = revision;
    }

    public String mmeAuthToken() {
        return mmeAuthToken;
    }

    public String dsPrsId() {
        return dsPrsId;
    }

    public String token() {
        return dsPrsId + ":" + mmeAuthToken;
    }

    public List<Header> mobileBackupHeaders() {
        return new ArrayList<>(mobileBackupHeaders);
    }

    public List<Header> contentHeaders() {
        return new ArrayList<>(contentHeaders);
    }

    public long revision() {
        return revision;
    }

    @Override
    public String toString() {
        return "Auth{"
                + "dsPrsId=" + dsPrsId
                + ", mmeAuthToken=" + mmeAuthToken
                + ", mobileBackupHeaders=" + mobileBackupHeaders
                + ", contentHeaders=" + contentHeaders
                + ", revision=" + revision
                + '}';
    }
}
