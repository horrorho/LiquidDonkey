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

    public static enum Task {

        TO_FETCH,
        TO_WRITE,
        COMPLETED
    }

    public static enum Status {

        IN_PROGESS,
        FAILED,
        SUCCESS
    }

    static Item newInstance(ChunkServer.StorageHostChunkList chunkList) {

        return new Item(chunkList);
    }

    private final ChunkServer.StorageHostChunkList chunkList;
    private final List<Exception> exceptions;
    private byte[] data;
    private Task task;
    private Status status;

    Item(
            ChunkServer.StorageHostChunkList chunkList,
            List<Exception> exceptions,
            byte[] data,
            Task task,
            Status status) {

        this.chunkList = Objects.requireNonNull(chunkList);
        this.exceptions = Objects.requireNonNull(exceptions);
        this.data = data;
        this.task = Objects.requireNonNull(task);
        this.status = Objects.requireNonNull(status);
    }

    Item(ChunkServer.StorageHostChunkList chunkList) {
        this(chunkList, new ArrayList<>(), null, Task.TO_FETCH, Status.IN_PROGESS);
    }

    public ChunkServer.StorageHostChunkList chunkList() {
        return chunkList;
    }

    public Item addException(Exception ex) {
        exceptions.add(ex);
        return this;
    }

    public List<Throwable> exceptions() {
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

    public Task task() {
        return task;
    }

    public Item setTask(Task task) {
        this.task = task;
        return this;
    }

    public Status status() {
        return status;
    }

    public Item setStatus(Status status) {
        this.status = status;
        return this;
    }
}