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

    public static HttpConfig newInstance(Properties properties) { 
        Props<Property> props = Props.from(properties);
        
        boolean isPersistent = props.getProperty(Property.ENGINE_PERSISTENT, props::asBoolean);
        boolean isRelaxedSSL = props.getProperty(Property.HTTP_RELAX_SSL, props::asBoolean);

        return newInstance(isPersistent,
                isRelaxedSSL,
                props.getProperty(Property.HTTP_MAX_CONNECTIONS, props::asInteger),
                isPersistent
                        ? props.getProperty(Property.HTTP_RETRY_COUNT_PERSISTENT, props::asInteger)
                        : props.getProperty(Property.HTTP_RETRY_COUNT, props::asInteger),
                isPersistent
                        ? props.getProperty(Property.HTTP_RETRY_DELAY_MS_PERSISTENT, props::asInteger)
                        : props.getProperty(Property.HTTP_RETRY_DELAY_MS, props::asInteger),
                isPersistent
                        ? props.getProperty(Property.HTTP_SOCKET_TIMEOUT_RETRY_COUNT_PERSISTENT, props::asInteger)
                        : props.getProperty(Property.HTTP_SOCKET_TIMEOUT_RETRY_COUNT, props::asInteger),
                props.getProperty(Property.HTTP_TIMEOUT_MS, props::asInteger),
                props.getProperty(Property.HTTP_VALID_AFTER_INACTIVITY_MS, props::asInteger),
                props.getProperty(Property.HTTP_DEFAULT_USER_AGENT));
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
        return "HttpConfig{"
                + "isPersistent=" + isPersistent
                + ", isRelaxedSSL=" + isRelaxedSSL
                + ", maxConnections=" + maxConnections
                + ", retryCount=" + retryCount
                + ", retryDelayMs=" + retryDelayMs
                + ", socketTimeoutRetryCount=" + socketTimeoutRetryCount
                + ", timeoutMs=" + timeoutMs
                + ", validateAfterInactivityMs=" + validateAfterInactivityMs
                + ", userAgent=" + userAgent
                + '}';
    }
}
