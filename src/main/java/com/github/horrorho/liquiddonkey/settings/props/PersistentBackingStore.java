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

import static com.github.horrorho.liquiddonkey.settings.Markers.PROPS;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;

/**
 * PersistentProps.
 *
 * Do not store user authentication credentials.
 *
 * @author Ahseya
 * @param <E> Enum type
 */
public class PersistentBackingStore<E extends Enum<E>> implements BackingStore<E> {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PersistentBackingStore.class);

    private final Preferences preferences;
    private final Map<String, E> stringToEnum;

    PersistentBackingStore(Preferences preferences, Map<String, E> stringToEnum) {
        this.preferences = preferences;
        this.stringToEnum = stringToEnum;
    }

    @Override
    public boolean containsKey(E key) {
        return preferences.get(key.name(), null) != null;
    }

    @Override
    public String get(E key) {
        return preferences.get(key.name(), null);
    }

    @Override
    public Set<E> keySet() {
        try {
            return Stream.of(preferences.keys())
                    .map(stringToEnum::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (BackingStoreException ex) {
            logger.warn(PROPS, "-- keySet() > exception: ", ex);
            return new HashSet<>();
        }
    }

    @Override
    public String put(E key, String value) {
        String previous = preferences.get(key.name(), null);
        preferences.put(key.name(), value);
        return previous;
    }

    @Override
    public String remove(E key) {
        String previous = preferences.get(key.name(), null);
        preferences.remove(key.name());
        return previous;
    }

    @Override
    public BackingStore<E> copyOf() {
        return new PersistentBackingStore<>(preferences, new HashMap<>(stringToEnum));
    }
}
