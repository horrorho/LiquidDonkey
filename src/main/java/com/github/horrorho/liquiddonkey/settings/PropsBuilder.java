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
 * values copies or substantial portions of the Software.
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
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.READ;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PropsBuilder.
 *
 * @author Ahseya
 * @param <E> enum type
 */
@NotThreadSafe
public class PropsBuilder<E extends Enum<E>> {

    private static final Logger logger = LoggerFactory.getLogger(PropsBuilder.class);

    public static <E extends Enum<E>> PropsBuilder<E> from(Class<E> type) {
        return new PropsBuilder(type, null);
    }

    public static <E extends Enum<E>> PropsBuilder<E> from(Class<E> type, Props<E> defaults) {
        return new PropsBuilder(type, defaults);
    }

    private final Class<E> type;
    private Props<E> props;

    PropsBuilder(Class<E> type, Props<E> props) {
        this.type = Objects.requireNonNull(type);
        this.props = props;
    }

    public PropsBuilder<E> resource(E urlProperty) {
        if (!props.contains(urlProperty)) {
            logger.warn("-- resource() > missing url property: {}", urlProperty);
            return this;
        }
        logger.debug("-- resource() > url: {} / {}", urlProperty, props.get(urlProperty));
        return inputStream(() -> this.getClass().getResourceAsStream(props.get(urlProperty)));
    }

    public PropsBuilder<E> path(E pathProperty) {
        if (!props.contains(pathProperty)) {
            logger.warn("-- path() > missing path property: {}", pathProperty);
            return this;
        }
        logger.debug("-- path() > property: {} path: {}", pathProperty, props.get(pathProperty));
        return inputStream(() -> Files.newInputStream(Paths.get(props.get(pathProperty)), READ));
    }

    public PropsBuilder<E> inputStream(IOSupplier<InputStream> supplier) {
        props = Props.newInstance(type, props);
        Properties properties = new Properties();

        try (InputStream inputStream = supplier.get()) {
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                logger.warn("-- inputStream() > null InputStream");
            }
        } catch (NoSuchFileException ex) {
            logger.warn("-- inputStream() > no such file: {}", ex.getFile());
        } catch (IOException ex) {
            logger.warn("-- inputStream() > exception: {}", ex);
        }
        props.addAll(properties);
        return this;
    }

    PropsBuilder<E> values(Function<E, String> value) {
        props = Props.newInstance(type, props);
        Stream.of(type.getEnumConstants())
                .filter(property -> value.apply(property) != null)
                .forEach(property -> props.put(property, value.apply(property)));
        return this;
    }

    public Props<E> build() {
        return props;
    }
}
