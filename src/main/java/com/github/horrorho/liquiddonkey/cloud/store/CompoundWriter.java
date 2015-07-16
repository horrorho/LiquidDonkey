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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CompoundWriter.
 *
 * @author Ahseya
 */
public class CompoundWriter implements StoreWriter {

    public static CompoundWriter from(List<StoreWriter> writers) {
        return new CompoundWriter(writers);
    }

    private List<StoreWriter> writers;

    CompoundWriter(List<StoreWriter> writers) {
        this.writers = Objects.requireNonNull(new ArrayList<>(writers));
    }

    @Override
    public Long apply(OutputStream outputStream) throws IOException {
        if (writers == null) {
            throw new IllegalStateException("Closed");
        }
        long total = 0;
        for (StoreWriter writer : writers) {
            total += writer.apply(outputStream);
        }
        return total;
    }

    @Override
    public void close() throws IOException {
        for (StoreWriter writer : writers) {
            writer.close();
        }
        writers = null;
    }
}
