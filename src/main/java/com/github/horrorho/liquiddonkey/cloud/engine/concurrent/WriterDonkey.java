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
import com.github.horrorho.liquiddonkey.cloud.store.DataWriter;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.iofunction.IOConsumer;
import com.github.horrorho.liquiddonkey.util.pool.ToDo;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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

    private final Function<WriterDonkey, FetchDonkey> fetchDonkeys;
    private final IOConsumer<Map<ByteString, DataWriter>> signaturesWriter;
    private byte[] data;

    public WriterDonkey(
            Function<WriterDonkey, FetchDonkey> fetchDonkeys,
            IOConsumer<Map<ByteString, DataWriter>> signaturesWriter,
            StoreManager manager,
            ChunkServer.StorageHostChunkList chunkList,
            List<Exception> exceptions,
            int retryCount,
            AtomicReference<Exception> fatal,
            Consumer<Set<ByteString>> failures) {

        super(manager, chunkList, exceptions, retryCount, fatal, failures);

        this.fetchDonkeys = Objects.requireNonNull(fetchDonkeys);
        this.signaturesWriter = Objects.requireNonNull(signaturesWriter);
    }

    @Override
    protected ToDo<Track, Donkey> toProcess() throws IOException, InterruptedException {
        log.trace("<< toProcess()");

        ToDo<Track, Donkey> toDo;
        try {
            Map<ByteString, DataWriter> signaturesToData = manager().put(chunkList(), data);
            signaturesWriter.accept(signaturesToData);
            toDo = complete();

        } catch (BadDataException ex) {
            log.warn("-- toProcess() > exception: ", ex);
            toDo = retry(ex, Track.FETCH, fetchDonkeys.apply(this));

        } finally {
            data = null;
        }

        log.trace(">> toProcess() > release: {}", toDo);
        return toDo;
    }

//    @Override
//    protected ToDo<Track, Donkey> toProcess() throws IOException, InterruptedException {
//        log.trace("<< toProcess()");
//
//        ToDo<Track, Donkey> toDo;
//
//        try (DonkeyWriter writer = donkeyWriters.apply(manager.put(chunkList(), data))) {
//            writer.write().entrySet().stream().forEach(entry -> results.accept(entry.getKey(), entry.getValue()));
//            toDo = complete();
//
//        } catch (BadDataException ex) {
//            log.warn("-- toProcess() > exception: ", ex);
//            toDo = retry(ex, Track.FETCH, fetchDonkeys.apply(this));
//            
//        } finally {
//            data = null;
//        }
//
//        log.trace(">> toProcess() > release: {}", toDo);
//        return toDo;
//    }
}
