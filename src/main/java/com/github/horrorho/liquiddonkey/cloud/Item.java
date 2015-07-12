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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.store.StoreManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.NotThreadSafe;

/**
 * Item.
 *
 * @author Ahseya
 */
@NotThreadSafe
public final class Item {

    static Item newInstance(
            ChunkServer.StorageHostChunkList chunkList,
            StoreManager storageManager) {

        return new Item(
                chunkList,
                storageManager,
                new ArrayList<>(),
                null,
                false);
    }

    private final ChunkServer.StorageHostChunkList chunkList;
    private final StoreManager storageManager;
    private final List<Exception> exceptions;
    private byte[] data;
    private boolean isCompleted;

    Item(
            ChunkServer.StorageHostChunkList chunkList,
            StoreManager storageManager,
            List<Exception> exceptions,
            byte[] data,
            boolean isCompleted) {

        this.chunkList = Objects.requireNonNull(chunkList);
        this.storageManager = Objects.requireNonNull(storageManager);
        this.exceptions = Objects.requireNonNull(exceptions);
        this.data = data;
        this.isCompleted = isCompleted;
    }

    public ChunkServer.StorageHostChunkList chunkList() {
        return chunkList;
    }

    public StoreManager storageManager() {
        return storageManager;
    }

    public Item addException(Exception ex) {
        exceptions.add(ex);
        return this;
    }

    public List<Throwable> errors() {
        return new ArrayList<>(exceptions);
    }

    public int exceptionCount() {
        return exceptions.size();
    }

    public byte[] data() {
        return data;
    }

    public Item setData(byte[] data) {
        this.data = data;
        return this;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public Item setCompleted() {
        isCompleted = true;
        return this;
    }

    @Override
    public String toString() {
        return "Item{"
                + "exceptions=" + exceptions
                + ", chunkList=" + chunkList.getHostInfo().getUri()
                + ", isCompleted=" + isCompleted
                + '}';
    }
}
// TODO ChunkServer.StorageHostChunkList for item
