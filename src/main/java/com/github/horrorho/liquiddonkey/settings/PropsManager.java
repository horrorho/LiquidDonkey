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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ahseya
 */
@NotThreadSafe
public class PropsManager implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(PropsManager.class);

    public static PropsManager from(Property pathProperty) {
        return from(PropsBuilder.fromDefaults().path(pathProperty).build(), pathProperty);
    }

    public static PropsManager from(Props props, Property pathProperty) {
        return new PropsManager(props, pathProperty);
    }

    private final Props props;
    private final Property pathProperty;

    PropsManager(Props props, Property pathProperty) {
        this.props = props;
        this.pathProperty = pathProperty;
    }

    public Props props() {
        return props;
    }

    @Override
    public void close() throws IOException {
        Path path = Paths.get(props.get(pathProperty));
        try (OutputStream outputStream = Files.newOutputStream(path, CREATE, WRITE, TRUNCATE_EXISTING)) {
            props.distinct().properties().store(outputStream, "auto");
            logger.debug("-- close() > properties written to: {}", path);
        }
    }
}
