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
package com.github.horrorho.liquiddonkey.http;

import com.github.horrorho.liquiddonkey.http.retryhandler.PersistentHttpRequestRetryHandler;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.settings.config.HttpConfig;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

/**
 * Http factory.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class HttpFactory {

    public static HttpFactory of(HttpConfig config) {
        return new HttpFactory(config);
    }

    private final HttpConfig config;

    HttpFactory(HttpConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    public Http newInstance(Printer printer) {

        PoolingHttpClientConnectionManager connectionManager = config.isRelaxedSSL()
                ? new PoolingHttpClientConnectionManager(relaxedSocketFactoryRegistry())
                : new PoolingHttpClientConnectionManager();

        connectionManager.setMaxTotal(config.maxConnections());
        connectionManager.setDefaultMaxPerRoute(config.maxConnections());
        connectionManager.setValidateAfterInactivity(config.validateAfterInactivityMs());

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(config.timeoutMs())
                .setConnectTimeout(config.timeoutMs())
                .setSocketTimeout(config.timeoutMs())
                .build();

        HttpRequestRetryHandler httpRequestRetryHandler = config.isPersistent()
                ? new PersistentHttpRequestRetryHandler(
                        config.retryCount(),
                        config.retryDelayMs(),
                        config.timeoutMs(),
                        true,
                        printer)
                : new DefaultHttpRequestRetryHandler(
                        config.retryCount(),
                        false);

        CloseableHttpClient client = HttpClients.custom()
                .setRetryHandler(httpRequestRetryHandler)
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent(config.userAgent())
                .build();

        return new Http(client, config.socketTimeoutRetryCount());
    }

    Registry<ConnectionSocketFactory> relaxedSocketFactoryRegistry() {
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register(
                        "http",
                        PlainConnectionSocketFactory.getSocketFactory())
                .register(
                        "https",
                        new SSLConnectionSocketFactory(
                                relaxedSSLContext(),
                                (hostname, session) -> true))
                .build();
    }

    SSLContext relaxedSSLContext() {
        try {
            return new SSLContextBuilder().loadTrustMaterial(null, (chain, authType) -> true).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
            throw new IllegalStateException("Unable to create relaxed SSL context.");
        }
    }
}
