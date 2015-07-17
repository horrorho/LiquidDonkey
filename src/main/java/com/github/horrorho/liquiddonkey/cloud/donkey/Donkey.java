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
package com.github.horrorho.liquiddonkey.cloud.donkey;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.util.pool.Release;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Donkey.
 *
 * @author Ahseya
 */
@NotThreadSafe
public abstract class Donkey {

    private static final Logger logger = LoggerFactory.getLogger(Donkey.class);

    protected final ChunkServer.StorageHostChunkList chunkList;
    protected final List<Exception> exceptions;
    protected final int retryCount;
    protected final AtomicReference<Exception> fatal;

    protected Donkey(
            ChunkServer.StorageHostChunkList chunkList,
            List<Exception> exceptions,
            int retryCount,
            AtomicReference<Exception> fatal) {

        this.chunkList = Objects.requireNonNull(chunkList);
        this.exceptions = Objects.requireNonNull(exceptions);
        this.retryCount = retryCount;
        this.fatal = Objects.requireNonNull(fatal);
    }

    public Release<Track, Donkey> process() {
        logger.trace("<< process()");

        Release<Track, Donkey> toDo;
        try {
            Exception exception = fatal.get();
            if (exception == null) {
                toDo = toProcess();
            } else {
                exceptions.add(exception);
                toDo = Release.dispose();
            }
        } catch (IOException | InterruptedException | RuntimeException ex) {
            logger.warn("-- process() > exception: {}", ex);
            exceptions.add(ex);
            fatal.compareAndSet(null, ex);
            toDo = Release.dispose();
        }

        logger.trace(">> process() > release: {}", toDo);
        return toDo;
    }

    protected boolean isExceptionLimit(Exception exception) {
        exceptions.add(exception);
        if (exceptions.size() > retryCount) {
            logger.warn("-- isExceptionLimit() > retry exhausted, count: {} exception: {}",
                    exceptions.size(), exception);
            return true;
        } else {
            logger.warn("-- isExceptionLimit() > retry, count: {} exception: {}", exceptions.size(), exception);
            return false;
        }
    }

    List<Exception> exceptions() {
        return new ArrayList<>(exceptions);
    }

    ChunkServer.StorageHostChunkList chunkList() {
        return chunkList;
    }

    int retryCount() {
        return retryCount;
    }

    protected abstract Release<Track, Donkey> toProcess() throws IOException, InterruptedException;

    @Override
    public String toString() {
        return "Donkey{" + "chunkList=" + chunkList.getHostInfo().getUri() + '}';
    }
}
