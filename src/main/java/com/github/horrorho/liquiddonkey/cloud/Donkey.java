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
import com.github.horrorho.liquiddonkey.cloud.file.SnapshotFileWriter;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.store.ChunkManager;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import net.jcip.annotations.NotThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Donkey. Download worker.
 *
 * @author ahseya
 */
@NotThreadSafe
public final class Donkey implements Supplier<Map<ByteString, IOFunction<OutputStream, Long>>>{

    private static final Logger logger = LoggerFactory.getLogger(Donkey.class);

    private final Http http;
    private final Client client;
    private final ChunkManager manager;
    private final long containerIndex;
    private final int attempts;

    public Donkey(Http http, Client client, ChunkManager manager, long containerIndex, int attempts) {
        this.http = Objects.requireNonNull(http);
        this.client = Objects.requireNonNull(client);
        this.manager = Objects.requireNonNull(manager);
        this.containerIndex = containerIndex;
        this.attempts = attempts;
    }


    @Override
    public Map<ByteString, IOFunction<OutputStream, Long>> get() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    public Boolean call() throws Exception {
        logger.trace("<< call() < {}");

        while (iterator.hasNext()) {
            Long containerIndex = iterator.next();

            try {

                List<byte[]> data = downloadChunkList(manager.storageHostChunkList(containerIndex));
                Map<ByteString, IOFunction<OutputStream, Long>> writers = manager.put(containerIndex, data);
                if (writers == null || writers.isEmpty()) {
                    download(writers);
                }

            } catch (HttpResponseException | UnknownHostException ex) {
                logger.warn("-- call() > exception: ", ex);
                failures.add(containerIndex);

                if (!isAggressive) {
                    throw ex;
                }
            } catch (IOException ex) {
                logger.warn("-- call() > exception: ", ex);
                failures.add(containerIndex);

                throw ex;
            }
        }
        logger.trace(">> call()");
        return true;
    }

    void download(Map<ByteString, IOFunction<OutputStream, Long>> writers) throws IOException {
        try {
            for (ByteString signature : writers.keySet()) {
                Set<ICloud.MBSFile> fileSet = signatureToFileSet.remove(signature);
                if (signature == null) {
                    logger.warn("-- download() > null signature: {}", Bytes.hex(signature));
                } else {
                    IOFunction<OutputStream, Long> writer = writers.get(signature);
                    for (ICloud.MBSFile file : fileSet) {
                        fileWriter.write(file, writer);
                    }
                }
            }
        } finally {
            // Destroy containers.
            writers.keySet().stream().forEach(manager::destroy);
        }
    }

    List<byte[]> downloadChunkList(ChunkServer.StorageHostChunkList chunkList)
            throws AuthenticationException, IOException {

        // Recursive.
        return chunkList.getChunkInfoCount() == 0
                ? new ArrayList<>()
                : downloadChunkList(chunkList, 0);
    }

    List<byte[]> downloadChunkList(ChunkServer.StorageHostChunkList chunkList, int attempt)
            throws AuthenticationException, IOException {
        // Recursive.
        List<byte[]> decrypted = attempt++ == attempts
                ? new ArrayList<>()
                : decrypter.decrypt(chunkList, client.chunks(http, chunkList));

        return decrypted == null
                ? downloadChunkList(chunkList, attempt)
                : decrypted;
    }
}
// TODO async decryption file writing
