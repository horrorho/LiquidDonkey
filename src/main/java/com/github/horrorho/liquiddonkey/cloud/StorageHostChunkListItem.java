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
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer.StorageHostChunkList;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * StorageHostChunkListItem.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class StorageHostChunkListItem {

    public StorageHostChunkListItem newInstance(long container, StorageHostChunkList chunkList) {
        return new StorageHostChunkListItem(container, chunkList);
    }

    private final long container;
    private final ChunkServer.StorageHostChunkList chunkList;

    StorageHostChunkListItem(long container, StorageHostChunkList chunkList) {
        this.container = container;
        this.chunkList = chunkList;
    }

    public long getContainer() {
        return container;
    }

    public StorageHostChunkList getChunkList() {
        return chunkList;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (int) (this.container ^ (this.container >>> 32));
        hash = 37 * hash + Objects.hashCode(this.chunkList);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StorageHostChunkListItem other = (StorageHostChunkListItem) obj;
        if (this.container != other.container) {
            return false;
        }
        return Objects.equals(this.chunkList, other.chunkList);
    }

    @Override
    public String toString() {
        return "StorageHostChunkListItem{"
                + "container=" + container
                + ", chunkList=" + chunkList.getHostInfo().getUri()
                + '}';
    }
}
