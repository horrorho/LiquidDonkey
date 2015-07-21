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
package com.github.horrorho.liquiddonkey.cloud.clients;

import com.github.horrorho.liquiddonkey.cloud.data.Tokens;
import com.github.horrorho.liquiddonkey.cloud.data.Headers;
import com.github.horrorho.liquiddonkey.cloud.data.IdPassword;
import com.github.horrorho.liquiddonkey.cloud.data.Auth;
import com.github.horrorho.liquiddonkey.data.SimplePropertyList;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import com.github.horrorho.liquiddonkey.settings.Markers;
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
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Authenticator.
 * <p>
 * Supports re-authentication for time-expired tokens.
 *
 * @author Ahseya
 */
@ThreadSafe
public class Authenticator {

    public static Authenticator from(AuthenticationConfig config) {
        logger.trace("<< from() < config: {}", config);

        IdPassword idPassword = config.hasAppleIdPassword()
                ? IdPassword.of(config.appleId(), config.password())
                : null;

        Auth auth = config.hasAuthToken()
                ? Auth.from(config.dsPrsId(), config.mmeAuthToken())
                : null;

        Authenticator instance = new Authenticator(idPassword, auth);

        logger.trace(">> from() > {}", instance);
        return instance;
    }

    public static Authenticator from(IdPassword idPassword, Auth auth) {
        logger.trace("<< from() < idPassword: {} Auth dsPrsId:{}", idPassword, auth.dsPrsID());
        logger.debug(marker, "-- from() < auth: {}", auth);

        Authenticator instance = new Authenticator(idPassword, auth);

        logger.trace(">> from() > {}", instance);
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(Authenticator.class);
    private static final Marker marker = MarkerFactory.getMarker(Markers.CLIENT);

    private static final ResponseHandler<byte[]> byteArrayResponseHandler = ResponseHandlerFactory.toByteArray();

    private final Headers headers;
    private final IdPassword idPassword;
    private final Lock lock;
    private volatile Auth auth;
    private volatile AuthenticationException authenticationException;

    Authenticator(
            Headers headers,
            IdPassword idPassword,
            Lock lock,
            Auth auth,
            AuthenticationException authenticationException) {

        if (idPassword == null && auth == null) {
            throw new IllegalArgumentException("Null credentials");
        }

        this.headers = headers;
        this.idPassword = idPassword;
        this.lock = lock;
        this.auth = auth;
        this.authenticationException = authenticationException;
    }

    Authenticator(IdPassword idPassword, Auth token) {
        this(Headers.create(), idPassword, new ReentrantLock(), token, null);
    }

    public String token(Http http) throws AuthenticationException, BadDataException, IOException, InterruptedException {
        Auth local = auth(http);
        return local.dsPrsID() + ":" + local.mmeAuthToken();
    }

    public String dsPrsID(Http http) throws AuthenticationException, BadDataException, IOException, InterruptedException {
        Auth local = auth(http);
        return local.dsPrsID();
    }

    public <T> T process(Http http, String dsPrsID, IOFunction<Auth, T> function)
            throws AuthenticationException, BadDataException, IOException, InterruptedException {

        while (true) {
            Auth local = auth(http);

            if (!dsPrsID.equals(local.dsPrsID())) {
                // For now we'll just issue an error rather than throw an illegal state exception.
                logger.error("-- process() > mismatch, supplied: {} auth: {}", dsPrsID, local.dsPrsID());
            }

            try {
                return function.apply(local);
            } catch (HttpResponseException ex) {
                if (ex.getStatusCode() == 401) {
                    logger.warn("-- process() > exception: ", ex);
                    dispose(local);
                }
                throw ex;
            }
        }
    }

    Auth auth(Http http) throws AuthenticationException, BadDataException, IOException, InterruptedException {
        Auth local = auth;

        if (local != null) {
            return local;
        }

        if (authenticationException != null) {
            throw authenticationException;
        }

        lock.lockInterruptibly();
        try {
            if (auth == null) {
                authenticate(http);
            }
            return auth;
        } finally {
            lock.unlock();
        }
    }

    void dispose(Auth badToken) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            // Ignore attempts to discard stale tokens.
            if (!badToken.timestamp().isBefore(auth.timestamp())) {
                logger.warn("-- dispose() > disposed: {}", auth);
                auth = null;
            }
        } finally {
            lock.unlock();
        }
    }

    @GuardedBy("lock")
    void authenticate(Http http) throws AuthenticationException, BadDataException, IOException {
        logger.trace("<< authenticate() < {}", idPassword);

        if (idPassword == null) {
            throw new AuthenticationException("Unable to authenticate, no appleId/ password");
        }

        try {
            String authBasic = Tokens.create().basic(idPassword.getId(), idPassword.getPassword());
            logger.debug("-- authenticate() > token: {}", authBasic);

            byte[] data
                    = http.executor("https://setup.icloud.com/setup/authenticate/$APPLE_ID$", byteArrayResponseHandler)
                    .headers(headers.mmeClientInfo(), headers.authorization(authBasic))
                    .get();
            SimplePropertyList plist = SimplePropertyList.from(data);
            logger.debug("-- authenticate() >  plist: {}", plist);

            String dsPrsID = plist.value("appleAccountInfo", "dsPrsID");
            logger.debug("-- authenticate() >  dsPrsID: {}", dsPrsID);

            String mmeAuthToken = plist.value("tokens", "mmeAuthToken");
            logger.debug("-- authenticate() >   mmeAuthToken: {}", mmeAuthToken);

            auth = Auth.from(dsPrsID, mmeAuthToken);
            logger.debug("-- authenticate() > auth: {}", auth);
            logger.trace(">> authenticate()");

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
                + ", auth=" + auth
                + ", authenticationException=" + authenticationException
                + '}';
    }
}
