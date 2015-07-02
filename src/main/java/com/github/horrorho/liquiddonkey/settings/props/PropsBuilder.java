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
package com.github.horrorho.liquiddonkey.settings.props;

import com.github.horrorho.liquiddonkey.iofunction.IOSupplier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.READ;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PropsBuilder.
 *
 * @author Ahseya
 * @param <E> Enum type
 */
public class PropsBuilder<E extends Enum<E>> {

    public static <E extends Enum<E>> PropsBuilder<E> from(Class<E> type) {
        return from(type, new EnumMap<>(type), stringToEnum(type));
    }

    static <E extends Enum<E>> PropsBuilder<E> from(
            Class<E> type,
            Map<E, String> map,
            Map<String, E> stringToEnum) {

        return new PropsBuilder<>(type, map, stringToEnum);
    }

    static <E extends Enum<E>> Map<String, E> stringToEnum(Class<E> type) {
        return Stream.of(type.getEnumConstants()).collect(Collectors.toMap(Enum::name, Function.identity()));
    }

    private static final Logger logger = LoggerFactory.getLogger(PropsBuilder.class);

    private final Map<E, String> map;
    private final Map<String, E> stringToEnum;
    private final Class<E> type;

    private Class<?> c = null;
    private Props<E> parent = null;

    PropsBuilder(Class<E> type, Map<E, String> map, Map<String, E> stringToEnum) {
        this.type = Objects.requireNonNull(type);
        this.stringToEnum = Objects.requireNonNull(stringToEnum);
        this.map = Objects.requireNonNull(map);
    }

    public PropsBuilder<E> computeAbsent(Function<E, String> mappingFunction) {
        Stream.of(type.getEnumConstants())
                .forEach(key -> {
                    map.computeIfAbsent(key, mappingFunction);
                });
        return this;
    }

    public PropsBuilder<E> load(String resource) {
        return load(() -> this.getClass().getResourceAsStream(resource));
    }

    public PropsBuilder<E> load(Path path) {
        return load(() -> Files.newInputStream(path, READ));
    }

    public PropsBuilder<E> load(IOSupplier<InputStream> supplier) {
        Properties properties = new Properties();
        try (InputStream inputStream = supplier.get()) {
            if (inputStream != null) {
                properties.load(inputStream);

                properties.forEach((k, v) -> {
                    if (stringToEnum.containsKey(k.toString())) {
                        map.put(stringToEnum.get(k.toString()), v.toString());
                    } else {
                        logger.warn("-- load() > unknown property key: {}", k);
                    }
                });

            } else {
                logger.warn("-- load() > null InputStream");
            }
        } catch (NoSuchFileException ex) {
            logger.warn("-- load() > no such file: {}", ex.getFile());
        } catch (IOException ex) {
            logger.warn("-- load() > exception: {}", ex);
        }
        return this;
    }

    public PropsBuilder<E> parent(Props<E> parent) {
        this.parent = parent;
        return this;
    }

    public PropsBuilder<E> persistent(Class<?> c) {
        this.c = c;
        return this;
    }

    public Props<E> build() {
        BackingStore<E> store = (c == null)
                ? new MapBackingStore(map)
                : new PersistentBackingStore(Preferences.userNodeForPackage(c), map);

        return new Props<>(store, stringToEnum, parent);
    }
}
