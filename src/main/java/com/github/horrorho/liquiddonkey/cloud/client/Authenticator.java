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
package com.github.horrorho.liquiddonkey.cloud.client;

import com.github.horrorho.liquiddonkey.data.SimplePropertyList;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authenticator.
 * <p>
 * Supports re-authentication for time-expired tokens.
 *
 * @author Ahseya
 */
@ThreadSafe
public class Authenticator {

    public static Authenticator of(AuthenticationConfig config) {

        IdPassword idPassword = config.hasAppleIdPassword()
                ? IdPassword.of(config.appleId(), config.password())
                : null;

        Auth auth = config.hasAuthToken()
                ? Auth.of(config.dsPrsID(), config.mmeAuthToken())
                : null;

        return of(idPassword, auth);
    }

    public static Authenticator of(IdPassword idPassword, Auth auth) {
        logger.trace("<< of() < idPassword: {} Auth:{}", idPassword, auth);

        Authenticator instance = new Authenticator(idPassword, auth);

        logger.trace(">> of() > {}", instance);
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(Authenticator.class);
    // Thread safe.
    private static final ResponseHandler<byte[]> byteArrayResponseHandler = ResponseHandlerFactory.toByteArray();

    private final Headers headers;
    private final IdPassword idPassword;
    private final Lock lock;
    private Auth token;
    private volatile AuthenticationException authenticationException;

    Authenticator(
            Headers headers,
            IdPassword idPassword,
            Lock lock,
            Auth token,
            AuthenticationException authenticationException) {

        if (idPassword == null && token == null) {
            throw new IllegalArgumentException("Null credentials");
        }

        this.headers = headers;
        this.idPassword = idPassword;
        this.lock = lock;
        this.token = token;
        this.authenticationException = authenticationException;
    }

    Authenticator(IdPassword idPassword, Auth token) {
        this(Headers.create(), idPassword, new ReentrantLock(), token, null);
    }

    public Auth auth(Http http) throws AuthenticationException, BadDataException, IOException, InterruptedException {
        logger.trace("<< authToken()");

        if (authenticationException != null) {
            throw authenticationException;
        }

        lock.lockInterruptibly();
        try {
            if (token == null) {
                token = authenticate(http);
            }

            logger.trace(">> authToken() > token: {}", token);
            return token;

        } finally {
            lock.unlock();
        }
    }

    public boolean dispose(Auth badToken) throws InterruptedException {
        logger.trace("<< dispose() < bad token: {}", badToken);

        lock.lockInterruptibly();
        try {
            boolean isDisposed;

            if (badToken.timestamp().isBefore(token.timestamp())) {
                // Attempting to dispose stale token, discard
                isDisposed = false;
            } else {
                isDisposed = true;
                token = null;
            }

            logger.trace(">> dispose() > disposed: {}", isDisposed);
            return isDisposed;

        } finally {
            lock.unlock();
        }
    }

    @GuardedBy("lock")
    Auth authenticate(Http http) throws AuthenticationException, BadDataException, IOException {
        logger.trace("<< authenticate() < {}", idPassword);

        if (idPassword == null) {
            throw new AuthenticationException("Unable to authenticate, no appleId/ password");
        }

        try {
            String authBasic = Tokens.create().basic(idPassword.getId(), idPassword.getPassword());
            logger.trace("-- authenticate() > token: {}", authBasic);

            byte[] data
                    = http.executor("https://setup.icloud.com/setup/authenticate/$APPLE_ID$", byteArrayResponseHandler)
                    .headers(headers.mmeClientInfo(), headers.authorization(authBasic))
                    .get();
            SimplePropertyList plist = SimplePropertyList.from(data);
            logger.trace("-- authenticate() >  plist: {}", plist);

            String dsPrsID = plist.value("appleAccountInfo", "dsPrsID");
            logger.trace("-- authenticate() >  dsPrsID: {}", dsPrsID);

            String mmeAuthToken = plist.value("tokens", "mmeAuthToken");
            logger.trace("-- authenticate() >   mmeAuthToken: {}", mmeAuthToken);

            Auth newAuth = Auth.of(dsPrsID, mmeAuthToken);
            logger.trace(">> authenticate() > auth: {}", newAuth);
            return token;

        } catch (HttpResponseException ex) {
            if (ex.getStatusCode() == 401) {
                authenticationException = new AuthenticationException("Bad appleId/ password or not an iCloud account");
                throw ex;
            }
            throw ex;
        }
    }

    @Override
    public String toString() {
        return "Authenticator{"
                + "headers=" + headers
                + ", idPassword=" + idPassword
                + ", lock=" + lock + ", token="
                + token + ", authenticationException="
                + authenticationException
                + '}';
    }
}
