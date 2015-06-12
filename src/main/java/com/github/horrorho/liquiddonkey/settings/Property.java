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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.LoggerFactory;

/**
 * Global constants.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public class Property {

    @Immutable
    @ThreadSafe
    public enum Bool {

        SET_LAST_MODIFIED_TIMESTAMP(true);

        private final boolean value;

        private Bool(boolean defaultValue) {
            value = parseBoolean(name(), defaultValue);
        }

        public boolean bool() {
            return value;
        }
    }

    @Immutable
    @ThreadSafe
    public enum Int {

        BATCH_SIZE_MINIMUM(4194304),
        CHUNK_LIST_DOWNLOADER_AGGRESSIVE_RETRY(2),
        DONKEY_EXECUTOR_AGGRESSIVE_RETRY(2),
        DONKEY_RETRY_DELAY_MS(60000),
        HTTP_MAX_CONNECTIONS(32),
        HTTP_TIMEOUT_MS(60000),
        HTTP_VALID_AFTER_INACTIVITY_MS(60000),
        HTTP_RETRY_COUNT(3),
        HTTP_RETRY_DELAY_MS(100),
        HTTP_PERSISTENT_RETRY_COUNT(12),
        HTTP_PERSISTENT_RETRY_DELAY_MS(5000),
        HTTP_PERSISTENT_SOCKET_TIMEOUT_RETRY_COUNT(3),
        HTTP_SOCKET_TIMEOUT_RETRY_COUNT(1),
        THREAD_COUNT(4),
        THREAD_STAGGER_DELAY(100),
        LIST_FILES_LIMIT(4096);

        private final int value;

        private Int(int defaultValue) {
            value = parseInteger(name(), defaultValue);
        }

        public int integer() {
            return value;
        }
    }

    @Immutable
    @ThreadSafe
    public enum ItemType {

        ADDRESS_BOOK("addressbook.sqlitedb"),
        CALENDAR("calendar.sqlitedb"),
        CALL_HISTORY("call_history.db"),
        PHOTOS(".jpg", ".jpeg"),
        MOVIES(".mov", ".mp4", ".avi"),
        PNG(".png"),
        SMS("sms.db"),
        VOICEMAILS("voicemail"),
        NOTES("notes");

        private final List<String> relativePaths;

        private ItemType(String... defaultValues) {
            relativePaths = list("item_type", name(), defaultValues);
        }

        public List<String> relativePaths() {
            return new ArrayList<>(relativePaths);
        }
    }

    @Immutable
    @ThreadSafe
    public enum Str {

        HTTP_DEFAULT_USER_AGENT("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:37.0) Gecko/20100101 Firefox/37.0"),
        PROJECT_VERSION("N/A"),
        OUTPUT_DIRECTORY("");

        private final String value;

        private Str(String defaultValue) {
            value = property(name(), defaultValue);
        }

        public String string() {
            return value;
        }
    }

    static boolean parseBoolean(String property, boolean defaultValue) {
        String value = property(property, null);
        return value == null
                ? defaultValue
                : Boolean.parseBoolean(value);
    }

    static int parseInteger(String property, int defaultValue) {
        String value = property(property, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            LoggerFactory.getLogger(Property.class).warn("-- parseInteger() > bad integer, property: {}", property);
            return defaultValue;
        }
    }

    static List<String> list(String prefix, String name, String... defaultValues) {
        String values = property(prefix + "." + name, null);
        return values == null
                ? Arrays.asList(defaultValues)
                : Arrays.asList(values.split("\\s")).stream().collect(Collectors.toList());
    }

    static String property(String name, String defaultValue) {
        String value = SettingsProperties.INSTANCE.getProperty(name.toLowerCase(Locale.getDefault()));
        if (value == null) {
            LoggerFactory.getLogger(Property.class)
                    .warn("-- property() > missing property: {}", name.toLowerCase(Locale.getDefault()));
            return defaultValue;
        } else {
            return value;
        }
    }
}
