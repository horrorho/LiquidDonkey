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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CSV writer.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class CSVWriter {

    public static CSVWriter create() {
        return new CSVWriter(CSVFormat.RFC4180);
    }

    public static CSVWriter from(CSVFormat format) {
        return new CSVWriter(format);
    }

    private static final Logger logger = LoggerFactory.getLogger(CSVWriter.class);

    private static final Object[] HEADER = {"mode", "size", "last_modified_timestamp", "domain", "relative_path"};

    private final CSVFormat csvFormat;

    CSVWriter(CSVFormat csvFormat) {
        this.csvFormat = csvFormat;
    }

    public void files(Collection<ICloud.MBSFile> files, Path path) throws IOException {
        logger.trace("<< write() < files: {} path: {}", files.size(), path);
 
        Files.createDirectories(path.getParent());
        
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(path.toFile()), csvFormat)) {
            printer.printRecord(HEADER);

            for (ICloud.MBSFile file : files) {
                String mode = file.getAttributes().hasMode()
                        ? "0x" + Integer.toString(file.getAttributes().getMode(), 16)
                        : "";
                String size = file.hasSize()
                        ? Long.toString(file.getSize())
                        : "";
                String lastModified = file.getAttributes().hasLastModified()
                        ? Long.toString(file.getAttributes().getLastModified())
                        : "";

                printer.print(mode);
                printer.print(size);
                printer.print(lastModified);
                printer.print(file.getDomain());
                printer.print(file.getRelativePath());
                printer.println();
            }
        }
        logger.trace(">> write()");
    }
}
