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
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.util.pool.Release;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WriterDonkey.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class WriterDonkey extends Donkey {

    private static final Logger log = LoggerFactory.getLogger(WriterDonkey.class);

    private final StoreManager manager;
    private final Function<WriterDonkey, FetchDonkey> fetchDonkeys;
    private byte[] data;

    public WriterDonkey(
            StoreManager manager,
            Function<WriterDonkey, FetchDonkey> fetchDonkeys,
            byte[] data,
            ChunkServer.StorageHostChunkList chunkList,
            List<Exception> exceptions,
            int retryCount,
            AtomicReference<Exception> fatal) {

        super(chunkList, exceptions, retryCount, fatal);

        this.manager = Objects.requireNonNull(manager);
        this.fetchDonkeys = Objects.requireNonNull(fetchDonkeys);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    protected Release<Track, Donkey> toProcess() throws IOException {
        log.trace("<< toProcess()");

        Release<Track, Donkey> toDo;
        try {
            manager.put(chunkList, data);
            data = null;
            toDo = Release.dispose();
        } catch (BadDataException ex) {
            log.warn("-- toProcess() > exception: ", ex);
            toDo = isExceptionLimit(ex)
                    ? Release.dispose()
                    : Release.requeue(fetchDonkeys.apply(this));
        } catch (IOException | RuntimeException ex) {
            throw ex;
        } finally {
            data = null;
        }

        log.trace(">> toProcess() > release: {}", toDo);
        return toDo;
    }
}
