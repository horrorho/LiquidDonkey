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
package com.github.horrorho.liquiddonkey.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pool.
 *
 * @author Ahseya
 * @param <T> item type
 */
public class Pool<T> {

    public static <T> Pool<T> from(Supplier<T> supplier, int count) {
        logger.trace("<< from() < supplier: {} count: {}", supplier, count);

        ArrayBlockingQueue queue = new ArrayBlockingQueue(count, true);
        Stream.generate(supplier).limit(count).forEach(queue::add);

        Pool<T> instance = new Pool<>(queue);

        logger.trace(">> from()");
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(Pool.class);
    private final BlockingQueue<T> queue;

    Pool(BlockingQueue queue) {
        this.queue = queue;
    }

    public T acquire() throws InterruptedException {
        return queue.take();
    }

    public void recycle(T item) {
        queue.add(item);
    }
}
