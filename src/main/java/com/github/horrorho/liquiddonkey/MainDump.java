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
package com.github.horrorho.liquiddonkey;

import com.github.horrorho.liquiddonkey.settings.Property; 
import com.github.horrorho.liquiddonkey.settings.props.Props;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dump out properties files.
 *
 * @author Ahseya
 */
public class MainDump {

    private static final Logger logger = LoggerFactory.getLogger(MainDump.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        logger.trace("<< main() < args: {}", Arrays.asList(args));

        Props<Property> props = Property.defaultProps();

        Path path = Paths.get("liquiddonkey.properties");
        try (OutputStream outputStream = Files.newOutputStream(path, CREATE, WRITE, TRUNCATE_EXISTING)) {

            // Ordered store
            Properties properties = new Properties() {
                @Override
                public synchronized Enumeration<Object> keys() {
                    return Collections.enumeration(new TreeSet<Object>(super.keySet()));
                }
            };

            properties.putAll(props.distinct().properties());
            properties.store(outputStream, "liquiddonkey");

            logger.info("-- main() > properties written to: {}", path.toAbsolutePath());
        } catch (IOException ex) {
            logger.warn("-- main() > exception: ", ex);
        }

        logger.trace(">> main");
    }
}
