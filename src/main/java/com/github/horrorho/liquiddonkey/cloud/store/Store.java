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
package com.github.horrorho.liquiddonkey.cloud.store;

import java.util.Collection;
import java.util.List;

/**
 * Container store. Implementations must be thread safe.
 *
 * @author Ahseya
 * @param <K> key type
 */
public interface Store<K> {

    /**
     * Returns a list of keys present in the Store.
     *
     * @return list of keys, not null
     */
    List<K> keys();

    /**
     * Creates the referenced container and copies over the specified data.
     *
     * @param key, not null
     * @param data, not null
     * @return true if the Store did not already contain the specified container
     */
    boolean put(K key, byte[] data);

    /**
     * Removes the referenced container from the Store.
     *
     * @param key, not null
     * @return true if the Store contained the referenced container
     */
    boolean remove(K key);

    /**
     * Returns whether the Store contains referenced container.
     *
     * @param key, not null
     * @return true is the referenced container is present
     */
    boolean contains(K key);

    /**
     * Returns the size of the referenced container in bytes.
     *
     * @param key, not null
     * @return size of the container in bytes
     * @throws NullPointerException if no such container exists
     */
    int size(K key);

    /**
     * Returns the size of the Store in bytes.
     *
     * @return size of the store in bytes
     */
    long size();

    /**
     * Returns a DataWriter that writes the referenced containers bytes to the specified output stream. Subsequent
     * modifications to the Store will not alter its output. This writer should be closed when no longer required to
     * release underlying resources.
     *
     * @param key, not null
     * @return immutable writer, not null
     * @throws NullPointerException if the referenced container does not exist
     */
    DataWriter writer(K key);

    /**
     * Returns whether the Store contains all referenced containers.
     *
     * @param keys, not null
     * @return true is all referenced containers are present
     */
    default boolean contains(Collection<K> keys) {
        return keys.stream().allMatch(this::contains);
    }
}
