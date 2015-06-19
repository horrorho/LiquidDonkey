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

import java.util.Locale;

/**
 *
 * @author Ahseya
 */
public enum Property {

    CONFIG_PREFIX_ITEM_TYPE("ITEM_TYPE"),
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
    FILTER_DATE_MAX("3000-01-01"), // TODO figure out max date
    FILTER_DOMAIN(""),
    FILTER_EXTENSION(""),
    FILTER_ITEM_TYPES(""),
    FILTER_RELATIVE_PATH(""),
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
    ITEM_TYPE_PHOTOS(".jpg,.jpeg"),
    ITEM_TYPE_MOVIES(".mov,.mp4,.avi"),
    ITEM_TYPE_PNG(".png"),
    ITEM_TYPE_SMS("sms.db"),
    ITEM_TYPE_VOICEMAILS("voicemail"),
    ITEM_TYPE_NOTES("notes"),
    PROJECT_VERSION(""),
    SELECTION_SNAPSHOT("1,-1,-2"),
    SELECTION_UDID("");

//    private static final Map<String, Property> optToSetting
//            = Stream.of(Property.values())
//            .map(setting
//                    -> Arrays.asList(
//                            new SimpleEntry<>(setting.option().getOpt(), setting),
//                            new SimpleEntry<>(setting.option().getLongOpt(), setting)))
//            .flatMap(List::stream)
//            .filter(entry -> entry.getKey() != null)
//            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//
//    private static final List<Setting> itemTypes
//            = Stream.of(Property.values())
//            .filter(setting -> setting.name().toLowerCase(Locale.US).startsWith("item_type"))
//            .collect(Collectors.toList());
//
//    public static List<Setting> itemTypes() {
//        return new ArrayList<>(itemTypes);
//    }
//
//    public static Options options() {
//        Options options = new Options();
//        Stream.of(Property.values())
//                .map(Property::option)
//                .filter(Objects::nonNull)
//                .forEach(options::addOption);
//        return options;
//    }
//    public static Property setting(String opt) {
//        return optToSetting.get(opt);
//    }
    private final String defaultValue;

    private Property(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String key() {
        return name();
    }
}
