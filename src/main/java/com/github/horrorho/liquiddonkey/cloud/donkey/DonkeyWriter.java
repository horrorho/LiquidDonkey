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
package com.github.horrorho.liquiddonkey.cloud.donkey;

import com.github.horrorho.liquiddonkey.cloud.file.WriterResult;
import com.github.horrorho.liquiddonkey.cloud.file.SignatureWriter;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.store.StoreWriter;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DonkeyWriter.
 *
 * @author Ahseya
 */
@NotThreadSafe
public class DonkeyWriter implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(DonkeyWriter.class);

    private final SignatureWriter signatureWriter;
    private Map<ByteString, StoreWriter> writers;

    DonkeyWriter(SignatureWriter writer, Map<ByteString, StoreWriter> writers) {
        this.signatureWriter = Objects.requireNonNull(writer);
        this.writers = new HashMap<>(writers);
    }

    public Map<ICloud.MBSFile, WriterResult> write() throws IOException, InterruptedException {
        Map<ICloud.MBSFile, WriterResult> all = new HashMap<>();

        for (ByteString signature : writers.keySet()) {
            Map<ICloud.MBSFile, WriterResult> results = signatureWriter.write(signature, writers.get(signature));

            if (results == null) {
                logger.warn("-- write() > unreferenced signature: {}", Bytes.hex(signature));
            } else {
                all.putAll(results);
            }
        }
        return all;
    }

    @Override
    public void close() {
        if (writers == null) {
            return;
        }

        writers.values().stream().forEach((writer) -> {
            try {
                writer.close();
            } catch (IOException ex) {
                logger.warn("-- close() > exception: {}", ex);
            }
        });
        writers = null;
    }
}
