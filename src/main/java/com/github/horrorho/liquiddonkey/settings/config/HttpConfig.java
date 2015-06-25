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

import com.github.horrorho.liquiddonkey.settings.Parsers;
import com.github.horrorho.liquiddonkey.settings.Property;
import com.github.horrorho.liquiddonkey.settings.Props;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Http configuration.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class HttpConfig {

    public static HttpConfig newInstance(Props<Property> props) {
        Parsers parsers = Property.parsers();

        boolean isPersistent = props.get(Property.ENGINE_PERSISTENT, parsers::asBoolean);
        boolean isRelaxedSSL = props.get(Property.HTTP_RELAX_SSL, parsers::asBoolean);

        return newInstance(isPersistent,
                isRelaxedSSL,
                props.get(Property.HTTP_MAX_CONNECTIONS, parsers::asInteger),
                isPersistent
                        ? props.get(Property.HTTP_RETRY_COUNT_PERSISTENT, parsers::asInteger)
                        : props.get(Property.HTTP_RETRY_COUNT, parsers::asInteger),
                isPersistent
                        ? props.get(Property.HTTP_RETRY_DELAY_MS_PERSISTENT, parsers::asInteger)
                        : props.get(Property.HTTP_RETRY_DELAY_MS, parsers::asInteger),
                isPersistent
                        ? props.get(Property.HTTP_SOCKET_TIMEOUT_RETRY_COUNT_PERSISTENT, parsers::asInteger)
                        : props.get(Property.HTTP_SOCKET_TIMEOUT_RETRY_COUNT, parsers::asInteger),
                props.get(Property.HTTP_TIMEOUT_MS, parsers::asInteger),
                props.get(Property.HTTP_VALID_AFTER_INACTIVITY_MS, parsers::asInteger),
                props.get(Property.HTTP_DEFAULT_USER_AGENT));
    }

    public static HttpConfig newInstance(
            boolean isPersistent,
            boolean isRelaxedSSL,
            int maxConnections,
            int retryCount,
            int retryDelayMs,
            int socketTimeoutRetryCount,
            int timeoutMs,
            int validateAfterInactivityMs,
            String userAgent) {

        return new HttpConfig(
                isPersistent,
                isRelaxedSSL,
                maxConnections,
                retryCount,
                retryDelayMs,
                socketTimeoutRetryCount,
                timeoutMs,
                validateAfterInactivityMs,
                userAgent);
    }

    private final boolean isPersistent;
    private final boolean isRelaxedSSL;
    private final int maxConnections;
    private final int retryCount;
    private final int retryDelayMs;
    private final int socketTimeoutRetryCount;
    private final int timeoutMs;
    private final int validateAfterInactivityMs;
    private final String userAgent;

    HttpConfig(
            boolean isPersistent,
            boolean isRelaxedSSL,
            int maxConnections,
            int retryCount,
            int retryDelayMs,
            int socketTimeoutRetryCount,
            int timeoutMs,
            int validateAfterInactivityMs,
            String userAgent) {

        this.isPersistent = isPersistent;
        this.isRelaxedSSL = isRelaxedSSL;
        this.maxConnections = maxConnections;
        this.retryCount = retryCount;
        this.retryDelayMs = retryDelayMs;
        this.socketTimeoutRetryCount = socketTimeoutRetryCount;
        this.timeoutMs = timeoutMs;
        this.validateAfterInactivityMs = validateAfterInactivityMs;
        this.userAgent = userAgent;
    }

    public boolean isPersistent() {
        return isPersistent;
    }

    public boolean isRelaxedSSL() {
        return isRelaxedSSL;
    }

    public int maxConnections() {
        return maxConnections;
    }

    public int retryCount() {
        return retryCount;
    }

    public int retryDelayMs() {
        return retryDelayMs;
    }

    public int socketTimeoutRetryCount() {
        return socketTimeoutRetryCount;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public int validateAfterInactivityMs() {
        return validateAfterInactivityMs;
    }

    public String userAgent() {
        return userAgent;
    }

    @Override
    public String toString() {
        return "HttpConfig{" + "isPersistent=" + isPersistent + ", isRelaxedSSL=" + isRelaxedSSL
                + ", maxConnections=" + maxConnections + ", retryCount=" + retryCount + ", retryDelayMs="
                + retryDelayMs + ", socketTimeoutRetryCount=" + socketTimeoutRetryCount + ", timeoutMs="
                + timeoutMs + ", validateAfterInactivityMs=" + validateAfterInactivityMs + ", userAgent="
                + userAgent + '}';
    }
}
