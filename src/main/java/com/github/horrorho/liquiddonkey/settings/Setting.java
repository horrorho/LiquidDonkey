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
import org.apache.commons.cli.Option;

/**
 *
 * @author Ahseya
 */
public enum Setting {

    ENGINE_BATCH_SIZE_MINIMUM("4194304"),
    ENGINE_CHUNK_LIST_DOWNLOADER_RETRY("1"),
    ENGINE_CHUNK_LIST_DOWNLOADER_RETRY_AGGRESSIVE("2"),
    DONKEY_RETRY_DELAY_MS("1000"),
    ENGINE_THREAD_STAGGER_DELAY("1000"),
    ENGINE_THREAD_COUNT("4",
            new Option("t", "threads", true, "The maximum number of concurrent threads.")),
    ENGINE_AGGRESSIVE("false",
            new Option("a", "aggressive", false, "Aggressive retrieval tactics.")),
    ENGINE_LIST_FILES_LIMIT("4096"),
    ENGINE_PERSISTENT("false",
            new Option("p", "persistent", false,
                    "More persistent in the handling of network errors, for unstable connections.")),
    ENGINE_PRINT_STACK_TRACE("false",
            new Option("x", "stack-trace", false, "Prints stack trace on errors, useful for debugging.")),
    FILE_COMBINED("false",
            new Option("c", "combined", false, "Do not separate each snapshot into its own folder.")),
    FILE_FORCE("false",
            new Option("f", "force", false, "Download files regardless of whether a local version exists.")),
    FILE_IGNORE_EMPTY("true"), // TODO
    FILE_FLAT("false",
            new Option("i", "--itunes-style", false, "Download files to iTunes style format.")),
    FILE_OUTPUT_DIRECTORY("output", new Option("o", "output", true, "Output folder.")),
    FILE_SET_LAST_MODIFIED_TIMESTAMP("true"),
    FILTER_DATE_MIN("0000-01-01",
            new Option(null, "min-date", true, "Minimum last-modified timestamp, ISO format date. E.g. 2000-12-31.")),
    FILTER_DATE_MAX("3000-01-01",
            new Option(null, "max-date", true, "Maximum last-modified timestamp ISO format date. E.g. 2000-12-31.")), // TODO figure out max date
    FILTER_DOMAIN("", Option.builder("d").longOpt("domain")
            .desc("Limit files to those within the specified application domain/s.")
            .hasArgs().build()),
    FILTER_EXTENSION("",
            Option.builder("e").longOpt("extension")
            .desc("Limit files to those with the specified extension/s.")
            .hasArgs().build()),
    FILTER_ITEM_TYPES("",
            Option.builder(null).longOpt("item-types")
            .desc("Only download the specified item type/s:\n")
            .hasArgs().build()),
    FILTER_RELATIVE_PATH("",
            Option.builder("r").longOpt("relative-path")
            .desc("Limit files to those with the specified relative path/s")
            .hasArgs().build()),
    FILTER_SIZE_MIN("0", new Option(null, "min-size", true, "Minimum size in kilobytes.")),
    FILTER_SIZE_MAX(Long.toString(Long.MAX_VALUE / 1024),
            new Option(null, "max-size", true, "Maximum size in kilobytes.")),
    HTTP_DEFAULT_USER_AGENT("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:37.0) Gecko/20100101 Firefox/37.0"),
    HTTP_MAX_CONNECTIONS("32"),
    HTTP_RETRY_COUNT("3"),
    HTTP_RETRY_COUNT_PERSISTENT("60"),
    HTTP_RETRY_DELAY_MS("100"),
    HTTP_RETRY_DELAY_MS_PERSISTENT("1000"),
    HTTP_RELAX_SSL("false",
            new Option(null, "relax-ssl", false, "Relaxed SSL verification, for SSL validation errors.")),
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
    SELECTION_SNAPSHOT("1,-1,-2",
            Option.builder("s").longOpt("snapshot")
            .desc("Only download data in the snapshot/s specified.\n"
                    + "Negative numbers indicate relative positions from newest backup "
                    + "with -1 being the newest, -2 second newest, etc.")
            .hasArgs().build()),
    SELECTION_UDID("",
            Option.builder("u").longOpt("udid")
            .desc("Download the backup/s with the specified UDID/s. "
                    + "Will match partial UDIDs. Leave empty to download all.")
            .hasArgs().optionalArg(true).build());

    private final String defaultValue;
    private final Option option;

    private Setting(String defaultValue, Option option) {
        this.defaultValue = defaultValue;
        this.option = option;
    }

    private Setting(String defaultValue) {
        this(defaultValue, null);
    }

    public Option option() {
        return (Option) option.clone();
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String property() {
        return name().toLowerCase(Locale.US);
    }
}
/*
 private static String itemTypes() {
 return Arrays.asList(Property.ItemType.values()).stream()
 .filter(itemType -> !itemType.relativePaths().isEmpty())
 .map(itemType -> itemType.name() + " (" + itemType.relativePaths()
 .stream().collect(Collectors.joining(" ")) + ")"
 ).collect(Collectors.joining(" "));
 }
 */
