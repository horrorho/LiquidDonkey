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

import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.util.pool.WorkPool;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorkerFetcher.
 *
 * @author Ahseya
 */
public class WorkerFetcher implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(WorkerFetcher.class);

    public static WorkerFetcher newInstance(
            Http http,
            Client client,
            WorkPool<Item> pool,
            Consumer<Item> consumer,
            AtomicReference<Exception> fatal,
            int retryCount) {

        return new WorkerFetcher(http, client, pool, consumer, fatal, retryCount);
    }

    private final Http http;
    private final Client client;
    private final WorkPool<Item> pool;
    private final Consumer<Item> consumer;
    private final AtomicReference<Exception> fatal;
    private final int retryCount;

    WorkerFetcher(
            Http http,
            Client client,
            WorkPool<Item> pool,
            Consumer<Item> consumer,
            AtomicReference<Exception> fatal,
            int retryCount) {

        this.http = http;
        this.client = client;
        this.pool = pool;
        this.consumer = consumer;
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
        return fatal(item)
                ? null
                : fetchAndConsume(item);
    }

    boolean fatal(Item item) {
        Exception fatalEx = fatal.get();

        if (fatalEx == null) {
            // add to fail
            return false;
        } else {
            item.addException(fatalEx);
            return true;
        }
    }

    Item fetchAndConsume(Item item) {
        Item requeue;
        try {
            item.setData(client.chunks(http, item.chunkList()));
            consumer.accept(item);
            requeue = null;
        } catch (AuthenticationException ex) {
            logger.warn("-- fetchAndConsume() > exception: ", ex);

            fatal.compareAndSet(null, ex);
            item.addException(ex);
            requeue = null;
        } catch (HttpResponseException ex) {
            logger.warn("-- fetchAndConsume() > exception: ", ex);

            item.addException(ex);
            requeue = (item.exceptionCount() > retryCount) ? null : item;
        } catch (IOException ex) {
            logger.warn("-- fetchAndConsume() > exception: ", ex);

            fatal.compareAndSet(null, ex);
            requeue = null;
        } catch (RuntimeException ex) {
            logger.warn("-- fetchAndConsume() > exception: ", ex);
            // TODO ?aggressive
            requeue = null;
        }
        return requeue;
    }
}
