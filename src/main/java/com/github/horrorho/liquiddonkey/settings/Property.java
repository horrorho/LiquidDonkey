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

import java.time.format.DateTimeFormatter;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Configuration properties.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public enum Property {

    // Non-null values may be expected for certain prop
    APP_NAME("LiquidDonkey"),
    AUTHENTICATION_APPLEID(null),
    AUTHENTICATION_PASSWORD(null),
    AUTHENTICATION_TOKEN(null),
    COMMAND_LINE_HELP(null),
    COMMAND_LINE_VERSION(null),
    CONFIG_PREFIX_ITEM_TYPE("ITEM_TYPE_"),
    ENGINE_BATCH_SIZE_MINIMUM("4194304"),
    ENGINE_CHUNK_LIST_DOWNLOADER_RETRY("1"),
    ENGINE_CHUNK_LIST_DOWNLOADER_RETRY_AGGRESSIVE("2"),
    DONKEY_RETRY_DELAY_MS("1000"),
    ENGINE_THREAD_STAGGER_DELAY("1000"),
    ENGINE_THREAD_COUNT("4"),
    ENGINE_AGGRESSIVE("false"),
    ENGINE_PERSISTENT("false"),
    ENGINE_PRINT_STACK_TRACE("false"),
    FILE_COMBINED("false"),
    FILE_FORCE("false"), // TODO
    FILE_FLAT("false"),
    FILE_OUTPUT_DIRECTORY("ouput"),
    FILE_SET_LAST_MODIFIED_TIMESTAMP("true"),
    FILTER_DATE_MIN("0000-01-01"),
    FILTER_DATE_MAX("9999-01-01"),
    FILTER_DOMAIN(null),
    FILTER_EXTENSION(null),
    FILTER_ITEM_TYPES(null),
    FILTER_RELATIVE_PATH(null),
    FILTER_SIZE_MIN("0"),
    FILTER_SIZE_MAX(Long.toString(Long.MAX_VALUE / 1024)),
    HTTP_DEFAULT_USER_AGENT("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:37.0) Gecko/20100101 Firefox/37.0"),
    HTTP_MAX_CONNECTIONS("32"),
    HTTP_RETRY_COUNT("3"),
    HTTP_RETRY_COUNT_PERSISTENT("60"),
    HTTP_RETRY_DELAY_MS("100"),
    HTTP_RETRY_DELAY_MS_PERSISTENT("1000"),
    HTTP_RELAX_SSL("false"),
    HTTP_SOCKET_TIMEOUT_RETRY_COUNT("1"),
    HTTP_SOCKET_TIMEOUT_RETRY_COUNT_PERSISTENT("12"),
    HTTP_TIMEOUT_MS("30000"),
    HTTP_VALID_AFTER_INACTIVITY_MS("30000"),
    ITEM_TYPE_ADDRESS_BOOK("addressbook.sqlitedb"),
    ITEM_TYPE_CALENDAR("calendar.sqlitedb"),
    ITEM_TYPE_CALL_HISTORY("call_history.db"),
    ITEM_TYPE_PHOTOS(".jpg .jpeg"),
    ITEM_TYPE_MOVIES(".mov .mp4 .avi"),
    ITEM_TYPE_PNG(".png"),
    ITEM_TYPE_SMS("sms.db"),
    ITEM_TYPE_VOICEMAILS("voicemail"),
    ITEM_TYPE_NOTES("notes"),
    PROJECT_VERSION("N/A"),
    PROPERTIES_GUI_PATH("gui.properties"),
    PROPERTIES_JAR("/settings.properties"),
    PROPERTIES_USER("user.properties"),
    SELECTION_SNAPSHOT("1 -1 -2"),
    SELECTION_UDID(null);

    public static DateTimeFormatter dateTimeFormatter() {
        return DateTimeFormatter.ISO_DATE;
    }

    public static Props<Property> props() {
        return PropsBuilder.from(Property.class, defaultProps())
                .resource(PROPERTIES_JAR)
                .path(PROPERTIES_USER)
                .build();
    }

    public static Props<Property> defaultProps() {
        return PropsBuilder.from(Property.class)
                .values(Property::getDefaultValue)
                .build();
    }

    public static Parsers parsers() {
        return parsers;
    }

    private static final Parsers parsers = Parsers.newInstance(dateTimeFormatter());

    private final String defaultValue;

    private Property(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
