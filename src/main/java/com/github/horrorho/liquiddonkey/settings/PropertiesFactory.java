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
package com.github.horrorho.liquiddonkey.settings;

import com.github.horrorho.liquiddonkey.iofunction.IOSupplier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.READ;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PropertiesFactory.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class PropertiesFactory {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesFactory.class);

    private static final PropertiesFactory instance = new PropertiesFactory();

    public static PropertiesFactory create() {
        return instance;
    }

    PropertiesFactory() {
    }

    public Properties fromDefaults() {
        Properties properties = new Properties();

        Stream.of(Property.values())
                .filter(property -> property.getDefaultValue() != null)
                .forEach(property -> properties.put(property.toString(), property.getDefaultValue()));

        return properties;
    }

//    public Properties fromResource(String resource) {
//        return fromInputStream(() -> this.getClass().getResourceAsStream(resource));
//    }
//
//    public Properties fromFile(Path path) {
//        return fromInputStream(() -> Files.newInputStream(path, READ));
//    }
    public Properties fromInputStream(IOSupplier<InputStream> supplier) throws IOException {
        return fromInputStream(new Properties(), supplier);
    }

    public Properties fromInputStream(Properties properties, IOSupplier<InputStream> supplier) throws IOException {
        try (InputStream inputStream = supplier.get()) {
            if (inputStream != null) {
                properties.load(inputStream);

                properties.forEach((k, v) -> {
                    try {
                        Property property = Property.valueOf(k.toString());
                        logger.trace("-- fromInputStream() > property: {} key: {}", property, v);

                    } catch (IllegalArgumentException ex) {
                        logger.warn("-- fromInputStream() > unknown property key: {}", k);
                    }
                });
            } else {
                throw new IOException("Null input stream");
            }
        }
        return properties;
    } 
}
