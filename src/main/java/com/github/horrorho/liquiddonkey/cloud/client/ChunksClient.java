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
package com.github.horrorho.liquiddonkey.cloud.client;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.http.ResponseHandlerFactory;
import java.io.IOException;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
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
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(ChunksClient.class);

    private static final ChunksClient instance
            = new ChunksClient(ResponseHandlerFactory.toByteArray(), Headers.create());

    private final ResponseHandler<byte[]> byteArrayResponseHandler;
    private final Headers headers;

    ChunksClient(ResponseHandler<byte[]> byteArrayResponseHandler, Headers headers) {
        this.byteArrayResponseHandler = Objects.requireNonNull(byteArrayResponseHandler);
        this.headers = Objects.requireNonNull(headers);
    }

    /**
     * Queries the server and returns chunk data.
     *
     * @param client, not null
     * @param chunks, not null
     * @return chunk data, not null
     * @throws IOException
     */
    public byte[] get(HttpClient client, ChunkServer.StorageHostChunkList chunks) throws IOException {
        logger.trace("<< chunks() < chunks count: {}", chunks.getChunkInfoCount());

        HttpUriRequest request = get(chunks);
        byte[] data = client.execute(request, byteArrayResponseHandler);

        logger.trace(">> chunks() >  {}", data.length);
        return data;
    }

    public HttpUriRequest get(ChunkServer.StorageHostChunkList chunks) {
        logger.trace("<< request() < chunks count: {}", chunks.getChunkInfoCount());

        ChunkServer.HostInfo hostInfo = chunks.getHostInfo();
        String uri = hostInfo.getScheme() + "://" + hostInfo.getHostname() + "/" + hostInfo.getUri();

        HttpUriRequest request = RequestBuilder.create(hostInfo.getMethod()).setUri(uri).build();
        headers.headers(hostInfo.getHeadersList()).stream().forEach(request::addHeader);

        logger.trace(">> request()");
        return request;

    }

    public ResponseHandler<byte[]> responseHandler() {
        return byteArrayResponseHandler;
    }
}
