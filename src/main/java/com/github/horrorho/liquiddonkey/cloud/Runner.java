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

import com.github.horrorho.liquiddonkey.cloud.engine.concurrent.Donkey;
import com.github.horrorho.liquiddonkey.cloud.engine.concurrent.Track;
import com.github.horrorho.liquiddonkey.util.pool.WorkPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runner.
 *
 * @author Ahseya
 */
public class Runner implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    static Runner newInstance(WorkPools<Track, Donkey> pools, Track track) {
        return new Runner(pools, track, 0);
    }

    private final WorkPools<Track, Donkey> pools;
    private final Track track;
    private int count;

    Runner(WorkPools<Track, Donkey> pools, Track track, int count) {
        this.pools = pools;
        this.track = track;
        this.count = count;
    }

    @Override
    public void run() {
        logger.trace("<< run() < track: {}", track);

        try {
            while (!pools.process(track, Donkey::process)) {
                count++;
            }
        } catch (RuntimeException | InterruptedException ex) {
            logger.warn("-- run() > exception: ", ex);
        }

        logger.trace(">> run() > track: {} count: {}", track, count);
    }

    public int getCount() {
        return count;
    }
}
