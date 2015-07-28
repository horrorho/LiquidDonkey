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
package com.github.horrorho.liquiddonkey.cloud.engine.concurrent;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.util.pool.ToDo;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Donkey. Base work unit.
 *
 * @author Ahseya
 */
@NotThreadSafe
public abstract class Donkey {

    private static final Logger logger = LoggerFactory.getLogger(Donkey.class);

    private final StoreManager manager;
    private final ChunkServer.StorageHostChunkList chunkList;
    private final List<Exception> exceptions;
    private final int retryCount;
    private final AtomicReference<Exception> fatal;
    private final Consumer<Set<ByteString>> failures;

    public Donkey(
            StoreManager manager,
            ChunkServer.StorageHostChunkList chunkList,
            List<Exception> exceptions,
            int retryCount,
            AtomicReference<Exception> fatal,
            Consumer<Set<ByteString>> failures) {

        this.manager = Objects.requireNonNull(manager);
        this.chunkList = Objects.requireNonNull(chunkList);
        this.exceptions = new ArrayList<>(exceptions);
        this.retryCount = retryCount;
        this.fatal = Objects.requireNonNull(fatal);
        this.failures = Objects.requireNonNull(failures);
    }

    public ToDo<Track, Donkey> process() {
        logger.trace("<< process()");

        ToDo<Track, Donkey> toDo;
        try {
            Exception ex = fatal.get();
            toDo = ex == null
                    ? toProcess()
                    : abort(ex);

        } catch (IOException | InterruptedException | RuntimeException ex) {
            logger.warn("-- process() > fatal: {}", ex);
            exceptions.add(ex);
            fatal.compareAndSet(null, ex);
            toDo = abort(ex);
        }

        logger.trace(">> process() > release: {}", toDo);
        return toDo;
    }

    protected boolean isExceptionLimit() {
        return exceptions.size() > retryCount;
    }

    protected ToDo complete() {
        logger.debug("-- complete() > chunkList: {}", chunkList.getHostInfo().getUri());
        return ToDo.dispose();
    }

    protected ToDo requeue(Track track, Donkey donkey) {
        return ToDo.requeue(track, donkey);
    }

    protected ToDo retry(Exception ex) {
        exceptions.add(ex);
        return isExceptionLimit()
                ? fail()
                : ToDo.requeue(this);
    }

    protected ToDo retry(Exception ex, Track track, Donkey donkey) {
        exceptions.add(ex);
        return isExceptionLimit()
                ? fail()
                : ToDo.requeue(track, donkey);
    }

    protected ToDo abort(Exception ex) {
        exceptions.add(ex);
        return fail();
    }

    protected ToDo fail() {
        logger.debug("-- fail() > chunkList: {}", chunkList.getHostInfo().getUri());
        Set<ByteString> signatures = manager.fail(chunkList);
        failures.accept(signatures);
        return ToDo.dispose();
    }

    ChunkServer.StorageHostChunkList chunkList() {
        return chunkList;
    }

    List<Exception> exceptions() {
        return new ArrayList<>(exceptions);
    }

    StoreManager manager() {
        return manager;
    }

    protected abstract ToDo<Track, Donkey> toProcess() throws IOException, InterruptedException;

    @Override
    public String toString() {
        return "Donkey{" + "chunkList=" + chunkList.getHostInfo().getUri() + '}';
    }
}
