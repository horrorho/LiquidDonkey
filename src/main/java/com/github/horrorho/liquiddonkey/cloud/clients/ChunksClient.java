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
package com.github.horrorho.liquiddonkey.cloud.clients;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import java.io.IOException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChunksClient.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class ChunksClient {

    public static ChunksClient create() {
        return new ChunksClient(defaultByteArrayResponseHandler, Headers.create());
    }

    private static final Logger logger = LoggerFactory.getLogger(ChunksClient.class);

    private static final ResponseHandler<byte[]> defaultByteArrayResponseHandler = ResponseHandlerFactory.toByteArray();

    private final ResponseHandler<byte[]> byteArrayResponseHandler;
    private final Headers headers;

    ChunksClient(ResponseHandler<byte[]> byteArrayResponseHandler, Headers headers) {
        this.byteArrayResponseHandler = byteArrayResponseHandler;
        this.headers = headers;
    }

    /**
     * Queries the server and returns chunk data.
     *
     * @param http, not null
     * @param chunks, not null
     * @return chunk data, not null
     * @throws IOException
     */
    public byte[] get(Http http, ChunkServer.StorageHostChunkList chunks) throws IOException {
        logger.trace("<< chunks() < chunks count: {}", chunks.getChunkInfoCount());

        ChunkServer.HostInfo hostInfo = chunks.getHostInfo();
        String uri = hostInfo.getScheme() + "://" + hostInfo.getHostname() + "/" + hostInfo.getUri();

        byte[] data = http.executor(uri, byteArrayResponseHandler)
                .headers(headers.headers(hostInfo.getHeadersList()))
                .execute(hostInfo.getMethod());

        logger.trace(">> chunks() > size: {}", data.length);
        return data;
    }
}
