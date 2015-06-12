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

import com.github.horrorho.liquiddonkey.cloud.store.MemoryStore;
import com.github.horrorho.liquiddonkey.cloud.store.ChunkListStore;
import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.settings.Property;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chunk list download.
 *
 * @author Ahseya
 */
public class ChunkListDownloader {

    private static final Logger logger = LoggerFactory.getLogger(ChunkListDownloader.class);

    public static ChunkListDownloader newInstance(Client client, boolean isAggressive) {
        return new ChunkListDownloader(
                client,
                ChunkDecrypter.newInstance(),
                isAggressive
                        ? Property.Int.CHUNK_LIST_DOWNLOADER_AGGRESSIVE_RETRY.integer()
                        : 1);
    }

    private final ChunkDecrypter decrypter;
    private final Client client;
    private final int attempts;

    ChunkListDownloader(Client client, ChunkDecrypter decrypter, int attempts) {
        this.client = Objects.requireNonNull(client);
        this.decrypter = Objects.requireNonNull(decrypter);
        this.attempts = attempts;
    }

    public ChunkListStore download(ChunkServer.FileChecksumStorageHostChunkLists group) throws IOException {
        logger.trace("<< download() < group count : {}", group.getStorageHostChunkListCount());

        // TODO memory or disk based depending on size
        MemoryStore.Builder builder = MemoryStore.builder();
        for (ChunkServer.StorageHostChunkList chunkList : group.getStorageHostChunkListList()) {
            builder.add(ChunkListDownloader.this.download(chunkList));
        }
        ChunkListStore storage = builder.build();

        logger.trace(">> download() > container count : {}", storage.size());
        return storage;
    }

    List<byte[]> download(ChunkServer.StorageHostChunkList chunkList) throws IOException {
        return chunkList.getChunkInfoCount() == 0
                ? new ArrayList<>()
                : download(chunkList, 0);
    }

    List<byte[]> download(ChunkServer.StorageHostChunkList chunkList, int attempt) throws IOException {
        List<byte[]> decrypted = attempt++ == attempts
                ? new ArrayList<>()
                : decrypter.decrypt(chunkList, client.chunks(chunkList));

        return decrypted == null
                ? download(chunkList, attempt)
                : decrypted;
    }
}
