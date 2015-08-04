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
package com.github.horrorho.liquiddonkey.cloud;

import com.github.horrorho.liquiddonkey.cloud.data.Auth;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authenticator.
 * <p>
 * Thread-safe authenticator.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class Authenticator {

    public static Authenticator from(HttpClient client, String id, String password) throws IOException {
        Auth auth = Auth.from(client, id, password);
        return from(id, password, auth);
    }

    public static Authenticator from(Auth auth) {
        return new Authenticator("", "", auth);
    }

    public static Authenticator from(String id, String password, Auth auth) {
        return new Authenticator(id, password, auth);
    }

    private static final Logger logger = LoggerFactory.getLogger(Authenticator.class);

    private final String id;
    private final String password;
    private final Lock lock;

    @GuardedBy("lock")
    private volatile Token token;
    @GuardedBy("lock")
    private volatile String invalid;

    Authenticator(String id, String password, Lock lock, Token token, String invalid) {
        this.id = id;
        this.password = password;
        this.token = token;
        this.lock = Objects.requireNonNull(lock);
        this.invalid = invalid;
    }

    Authenticator(String id, String password, Auth auth) {
        this(id, password, new ReentrantLock(false), Token.from(auth), null);
    }

    public Token get() throws HttpResponseException {
        testIsInvalid();
        return token;
    }

    public Token reauthenticate(HttpClient client, Token expired) throws IOException {
        logger.trace("<< reauthenticate() < dsPrsID: {} timestamp: {}", expired.auth().dsPrsID(), expired.timestamp());

        testIsInvalid();
        lock.lock();
        try {
            if (token.timestamp().isAfter(expired.timestamp())) {
                logger.debug("-- reauthenticate() > expired token");

            } else {
                logger.debug("-- reauthenticate() > reauthenticating");
                authenticate(client);
            }
            logger.trace(">> reauthenticate() > dsPrsID: {} timestamp: {}", token.auth().dsPrsID(), token.timestamp());
            return token;

        } finally {
            lock.unlock();
        }
    }

    @GuardedBy("lock")
    void authenticate(HttpClient client) throws IOException {
        if (id == null || id.isEmpty() || password == null || password.isEmpty()) {
            invalid = "Unable to authenticate. Missing id/ password.";

        } else {
            try {
                Auth auth = Auth.from(client, id, password);

                if (auth.dsPrsID().equals(token.auth().dsPrsID())) {
                    token = Token.from(auth);

                } else {
                    logger.error("-- reauthentication() > mismatched dsPrsID: {} > {}",
                            token.auth().dsPrsID(), auth.dsPrsID());
                    invalid = "Account mismatch for token and id/ password";
                }
            } catch (HttpResponseException ex) {
                if (ex.getStatusCode() == 401) {
                    invalid = "Authentication failed";
                }
            }
        }

        testIsInvalid();
    }

    void testIsInvalid() throws HttpResponseException {
        if (isInvalid()) {
            throw new HttpResponseException(401, invalid);
        }
    }

    public boolean isInvalid() {
        return invalid != null;
    }

    public String dsPrsID() {
        return token.auth().dsPrsID();
    }

    @Immutable
    @ThreadSafe
    public static final class Token {

        static Token from(Auth auth) {
            return new Token(auth, Instant.now());
        }

        private final Auth auth;
        private final Instant timestamp;

        Token(Auth auth, Instant timestamp) {
            this.auth = auth;
            this.timestamp = timestamp;
        }

        public Auth auth() {
            return auth;
        }

        public Instant timestamp() {
            return timestamp;
        }
    }
}
