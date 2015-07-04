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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
public class Outcomes {

    public static Outcomes from(Snapshot snapshot) {
        logger.trace("<< from() < snapshot: {}", snapshot);

        Outcomes instance = new Outcomes(
                snapshot.signatures(),
                snapshot.signatures().keySet(),
                Collections.<ByteString>newSetFromMap(new ConcurrentHashMap<>()),
                Collections.<ByteString>newSetFromMap(new ConcurrentHashMap<>()),
                Collections.<ByteString>newSetFromMap(new ConcurrentHashMap<>()));

        logger.trace(">> from()");
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(Outcomes.class);

    private final ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures;
    private final Set<ByteString> pending;      // Concurrent Set required
    private final Set<ByteString> completed;    // Concurrent Set required
    private final Set<ByteString> failed;       // Concurrent Set required
    private final Set<ByteString> serverError;  // Concurrent Set required

    Outcomes(
            ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures,
            Set<ByteString> pending,
            Set<ByteString> completed,
            Set<ByteString> failed,
            Set<ByteString> badServer) {

        this.signatures = Objects.requireNonNull(signatures);
        this.pending = Objects.requireNonNull(pending);
        this.completed = Objects.requireNonNull(completed);
        this.failed = Objects.requireNonNull(failed);
        this.serverError = Objects.requireNonNull(badServer);
    }

    public Set<ICloud.MBSFile> fileSet(ByteString signature) {
        if (pending.contains(signature)) {
            return signatures.get(signature);
        } else {
            logger.warn("-- fileSet() > missing signature: {}", Bytes.hex(signature));
            return new HashSet<>();
        }
    }

    public void completed(ByteString signature) {
        if (!pending.remove(signature)) {
            logger.warn("-- completed() > missing pending signature: {}", Bytes.hex(signature));
        }

        if (!completed.add(signature)) {
            logger.warn("-- completed() > duplicate completed signature: {}", Bytes.hex(signature));
        }
    }

    public void failed(ByteString signature) {
        if (!pending.remove(signature)) {
            logger.warn("-- completed() > missing pending signature: {}", Bytes.hex(signature));
        }

        if (!failed.add(signature)) {
            logger.warn("-- completed() > duplicate failed signature: {}", Bytes.hex(signature));
        }
    }

    public void serverError(ByteString signature) {
        if (!pending.remove(signature)) {
            logger.warn("-- completed() > missing pending signature: {}", Bytes.hex(signature));
        }

        if (!serverError.add(signature)) {
            logger.warn("-- completed() > duplicate serverError signature: {}", Bytes.hex(signature));
        }
    }
}
