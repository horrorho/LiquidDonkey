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

import com.github.horrorho.liquiddonkey.cloud.file.SignatureWriter;
import com.github.horrorho.liquiddonkey.cloud.file.WriterResult;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.store.DataWriter;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.printer.Level;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.util.pool.WorkPool;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorkerFetch.
 *
 * @author Ahseya
 */
public class WorkerWriter implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(WorkerWriter.class);

    private final WorkPool<Item> pool;
    private final Consumer<Item> fail;
    private final Consumer<Item> retry;
    private final Consumer<Item> success;
    private final SignatureWriter signatureWriter;
    private final Printer printer;
    private final AtomicReference<Exception> fatal;
    private final int retryCount;

    public WorkerWriter(
            WorkPool<Item> pool,
            Consumer<Item> fail,
            Consumer<Item> retry,
            Consumer<Item> success,
            SignatureWriter signatureWriter,
            Printer printer,
            AtomicReference<Exception> fatal,
            int retryCount) {

        this.pool = pool;
        this.fail = fail;
        this.retry = retry;
        this.success = success;
        this.signatureWriter = signatureWriter;
        this.printer = printer;
        this.fatal = fatal;
        this.retryCount = retryCount;
    }

    @Override
    public void run() {
        logger.trace("<< run()");

        try {
            while (!pool.operate(this::process)) {
            }
        } catch (InterruptedException ex) {
            logger.warn("-- run() >> interrupted: {}", ex);
        } catch (RuntimeException ex) {
            logger.warn("-- run() >> exception: ", ex);
        }

        logger.trace(">> run()");
    }

    Item process(Item item) {
        Consumer<Item> consumer;
        try {
            Map<ByteString, DataWriter> writers = item.storageManager().put(item.chunkList(), item.data());

            for (ByteString signature : writers.keySet()) {
                try (DataWriter dataWriter = writers.get(signature)) {
                    Map<ICloud.MBSFile, WriterResult> map = signatureWriter.write(signature, dataWriter);
                    map.keySet().stream().forEach(
                            file -> printer.println(Level.VV,
                                    "\t" + file.getDomain() + " " + file.getRelativePath() + " " + map.get(file)));
                }
            }

            consumer = success;
        } catch (BadDataException ex) {
            logger.warn("-- process() > exception: ", ex);

            if (item.addException(ex).exceptionCount() > retryCount) {
                consumer = fail;
            } else {
                retry.accept(item);
                consumer = retry;
            }
        } catch (IOException ex) {
            logger.warn("-- process() > exception: ", ex);

            fatal.compareAndSet(null, ex);
            consumer = fail;
        } catch (RuntimeException ex) {
            logger.warn("-- process() > exception: ", ex);

            consumer = fail;
        }
        consumer.accept(item);
        return null;
    }
}
