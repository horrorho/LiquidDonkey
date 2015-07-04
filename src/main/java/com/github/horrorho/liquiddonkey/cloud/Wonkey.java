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
import com.github.horrorho.liquiddonkey.cloud.file.LocalFileWriter;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.store.ChunkManager;
import com.github.horrorho.liquiddonkey.cloud.store.MemoryStore;
import com.github.horrorho.liquiddonkey.cloud.store.Store;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.iofunction.IOFunction;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
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
public final class Wonkey implements Callable<Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(Wonkey.class);

    private final Http http;
    private final Client client;
    private final ByteString backupUdid;
    private final int snapshot;
    private final Iterator<Map<Long, ChunkServer.StorageHostChunkList>> iterator;
    private final Outcomes outcomes;
    private final ChunkManager manager;
    private final ChunkDecrypter decrypter;
    private final LocalFileWriter writer;
    private final boolean isAggressive;
    private final int attempts;

    Wonkey(
            Http http,
            Client client,
            ByteString backupUdid,
            int snapshot,
            Iterator<Map<Long, ChunkServer.StorageHostChunkList>> iterator,
            Outcomes outcomes,
            ChunkManager manager,
            ChunkDecrypter decrypter,
            LocalFileWriter writer,
            boolean isAggressive,
            int attempts) {

        this.http = Objects.requireNonNull(http);
        this.client = Objects.requireNonNull(client);
        this.backupUdid = Objects.requireNonNull(backupUdid);
        this.snapshot = snapshot;
        this.iterator = Objects.requireNonNull(iterator);
        this.outcomes = Objects.requireNonNull(outcomes);
        this.manager = Objects.requireNonNull(manager);
        this.decrypter = Objects.requireNonNull(decrypter);
        this.writer = Objects.requireNonNull(writer);
        this.isAggressive = isAggressive;
        this.attempts = attempts;
    }

    @Override
    public Boolean call() throws Exception {
        logger.trace("<< call() < {}");

        while (iterator.hasNext()) {
            Map<Long, ChunkServer.StorageHostChunkList> signatureToChunkList = iterator.next();

            try {
                if (signatureToChunkList.isEmpty()) {
                    logger.warn("-- call() > empty signature/ chunk list map");
                } else {
                    downloadSignature(signature);
                    outcomes.completed(signature);
                }
            } catch (HttpResponseException | UnknownHostException ex) {
                logger.warn("-- call() > exception: ", ex);
                outcomes.serverError(signature);

                if (!isAggressive) {
                    throw ex;
                }
            } catch (BadDataException | IOException ex) {
                logger.warn("-- call() > exception: ", ex);
                outcomes.failed(signature);
                throw ex;
            }
        }
        logger.trace(">> call()");
        return true;
    }

    
    public void download(long containerIndex, ChunkServer.StorageHostChunkList chunkList) throws Exception {


            try {

                    doDownload(containerIndex, chunkList);
                    outcomes.completed(signature);

            } catch (HttpResponseException | UnknownHostException ex) {
                logger.warn("-- call() > exception: ", ex);
                outcomes.serverError(signature);

                if (!isAggressive) {
                    throw ex;
                }
            } catch (BadDataException | IOException ex) {
                logger.warn("-- call() > exception: ", ex);
                outcomes.failed(signature);
                throw ex;
            }
        
    }
    
    List<ByteString> doDownload(long containerIndex, ChunkServer.StorageHostChunkList chunkList)
            throws AuthenticationException, IOException {

        List<byte[]> data = downloadChunkList(chunkList);
        Map<ByteString, IOFunction<OutputStream, Long>> writers = manager.put(containerIndex, data);

        // TODO destroy
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
