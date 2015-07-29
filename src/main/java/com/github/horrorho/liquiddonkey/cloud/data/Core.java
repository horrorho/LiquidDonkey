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
package com.github.horrorho.liquiddonkey.cloud.data;

import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Core.
 * <p>
 * Core settings.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public class Core {

    private final String dsPrsID;
    private final String mmeAuthToken;
    private final String contentUrl;
    private final String mobileBackupUrl;
    private final String appleId;
    private final String fullName;

    Core(
            String dsPrsID,
            String mmeAuthToken,
            String contentUrl,
            String mobileBackupUrl,
            String appleId,
            String fullName) {

        this.dsPrsID = Objects.requireNonNull(dsPrsID);
        this.mmeAuthToken = Objects.requireNonNull(mmeAuthToken);
        this.contentUrl = Objects.requireNonNull(contentUrl);
        this.mobileBackupUrl = Objects.requireNonNull(mobileBackupUrl);
        this.appleId = Objects.requireNonNull(appleId);
        this.fullName = Objects.requireNonNull(fullName);
    }

    Core(Core settings) {
        this(
                settings.dsPrsID,
                settings.mmeAuthToken,
                settings.contentUrl,
                settings.mobileBackupUrl,
                settings.appleId,
                settings.fullName);
    }

    public final Auth auth() {
        return Auth.from(dsPrsID, mmeAuthToken);
    }

    public final String dsPrsID() {
        return dsPrsID;
    }

    public final String contentUrl() {
        return contentUrl;
    }

    public final String mobileBackupUrl() {
        return mobileBackupUrl;
    }

    public final String appleId() {
        return appleId;
    }

    public final String fullName() {
        return fullName;
    }

    @Override
    public String toString() {
        return "Core{"
                + "dsPrsID=" + dsPrsID
                + ", mmeAuthToken=" + mmeAuthToken
                + ", contentUrl=" + contentUrl
                + ", mobileBackupUrl=" + mobileBackupUrl
                + ", appleId=" + appleId
                + ", fullName=" + fullName
                + '}';
    }
}
