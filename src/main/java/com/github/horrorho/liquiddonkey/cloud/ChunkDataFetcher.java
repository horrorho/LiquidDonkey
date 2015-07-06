/* 
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation list (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies from the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions from the Software.
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
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.http.Http;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Function;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChunkDataFetcher. Download worker.
 *
 * @author ahseya
 */
@ThreadSafe
public final class ChunkDataFetcher implements Function<ChunkServer.StorageHostChunkList, byte[]> {

    private static final Logger logger = LoggerFactory.getLogger(ChunkDataFetcher.class);

    public static ChunkDataFetcher newInstance(Http http, Client client) {
        return new ChunkDataFetcher(http, client);
    }

    private final Http http;
    private final Client client;

    ChunkDataFetcher(Http http, Client client) {
        this.http = Objects.requireNonNull(http);
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public byte[] apply(ChunkServer.StorageHostChunkList chunks) {

        try {
            logger.trace("<< apply() < chunk count: {} host: {}",
                    chunks.getChunkInfoCount(), chunks.getHostInfo().getHostname());

            byte[] data = client.chunks(http, chunks);

            logger.trace(">> apply() > data size: {}", data.length);
            return data;
        } catch (IOException ex) {
            logger.warn("-- get() > exception: {}", ex);
            throw new UncheckedIOException(ex);
        }
    }
}
