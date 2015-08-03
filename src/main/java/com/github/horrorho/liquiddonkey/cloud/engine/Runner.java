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
package com.github.horrorho.liquiddonkey.cloud.engine;

import com.github.horrorho.liquiddonkey.cloud.Outcome;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.util.SyncSupplier;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Donkey.
 *
 * @author Ahseya
 */
@NotThreadSafe
class Runner implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    private final SyncSupplier<ChunkServer.StorageHostChunkList> chunks;
    private final Consumer<Map<ICloud.MBSFile, Outcome>> outcomesConsumer;
    private final AtomicReference<Exception> fatal;
    private volatile boolean isAlive;
    private final Donkey donkey;

    Runner(
            SyncSupplier<ChunkServer.StorageHostChunkList> chunks,
            Consumer<Map<ICloud.MBSFile, Outcome>> outcomesConsumer,
            AtomicReference<Exception> fatal,
            Donkey donkey,
            boolean isAlive) {

        this.chunks = chunks;
        this.outcomesConsumer = outcomesConsumer;
        this.fatal = fatal;
        this.donkey = donkey;
        this.isAlive = isAlive;
    }

    Runner(
            SyncSupplier<ChunkServer.StorageHostChunkList> chunks,
            Consumer<Map<ICloud.MBSFile, Outcome>> outcomesConsumer,
            AtomicReference<Exception> fatal,
            Donkey donkey) {

        this(chunks, outcomesConsumer, fatal, donkey, true);
    }

    @Override
    public void run() {
        logger.trace("<< run()");

        try {
            while (isAlive) {
                Exception ex = fatal.get();
                if (ex != null) {
                    logger.warn("-- run() > fatal: {}", ex);
                    break;
                }

                ChunkServer.StorageHostChunkList chunkList = chunks.get();
                if (chunkList == null) {
                    logger.debug("-- run() > depleted");
                    break;
                }

                outcomesConsumer.accept(
                        donkey.process(chunkList));
            }
        } catch (IOException | InterruptedException | RuntimeException ex) {
            fatal.compareAndSet(null, ex);
            logger.warn("-- run() > exception: ", ex);
        }

        logger.trace(">> run() > fatal: {} isAlive: {}", fatal == null ? null : fatal.get().getMessage(), isAlive);
    }

    void kill() {
        isAlive = false;
        donkey.kill();
    }
}
// TODO aggression
