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
package com.github.horrorho.liquiddonkey.settings.config;

import com.github.horrorho.liquiddonkey.settings.Property;
import static com.github.horrorho.liquiddonkey.settings.config.Args.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Args.
 *
 * @author Ahseya
 */
public class Args {

    static final String AGGRESSIVE = "aggressive";
    static final String COMBINED = "combined";
    static final String DOMAIN = "domain";
    static final String EXTENSION = "extension";
    static final String FORCE = "force";
    static final String HELP = "help";
    static final String IGNORE_EMPTY = "ignore-empty";
    static final String ITEM_TYPES = "item-types";
    static final String ITUNES_STYLE = "itunes-style";
    static final String MAX_DATE = "max-date";
    static final String MAX_SIZE = "max-size";
    static final String MIN_DATE = "min-date";
    static final String MIN_SIZE = "min-size";
    static final String OUTPUT = "output";
    static final String PERSISTENT = "persistent";
    static final String RELATIVE_PATH = "relative-path";
    static final String RELAX_SSL = "relax-ssl";
    static final String SNAPSHOT = "snapshot";
    static final String STACK_TRACE = "stack-trace";
    static final String THREADS = "threads";
    static final String UDID = "udid";
    static final String VERSION = "version";

    public static Args newInstance(Options options) {
        return new Args(options);
    }

    public static Args newStandardInstance() {
        String itemTypes = itemTypes();

        return newInstance(new Options()
                .addOption("o", OUTPUT, true, "Output folder.")
                .addOption("c", COMBINED, false, "Do not separate each snapshot into its own folder.")
                .addOption(Option.builder("u").longOpt(UDID)
                        .desc("Download the backup/s with the specified UDID/s. "
                                + "Will match partial UDIDs. Leave empty to download all.")
                        .hasArgs().optionalArg(true).build())
                .addOption(Option.builder("s").longOpt(SNAPSHOT)
                        .desc("Only download data in the snapshot/s specified.\n"
                                + "Negative numbers indicate relative positions from newest backup "
                                + "with -1 being the newest, -2 second newest, etc.")
                        .hasArgs().build())
                //.addOption("i", ITUNES_STYLE, false, "Download files to iTunes style format.")
                .addOption(Option.builder(null).longOpt(ITEM_TYPES)
                        .desc("Only download the specified item type/s:\n" + itemTypes)
                        .hasArgs().build())
                .addOption(Option.builder("d").longOpt(DOMAIN)
                        .desc("Limit files to those within the specified application domain/s.")
                        .hasArgs().build())
                .addOption(Option.builder("r").longOpt(RELATIVE_PATH)
                        .desc("Limit files to those with the specified relative path/s")
                        .hasArgs().build())
                .addOption(Option.builder("e").longOpt(EXTENSION)
                        .desc("Limit files to those with the specified extension/s.")
                        .hasArgs().build())
                //.addOption(null, IGNORE_EMPTY, false, "Only download non-empty files.")
                .addOption(null, MIN_DATE, true, "Minimum last-modified timestamp, ISO format date. E.g. 2000-12-31.")
                .addOption(null, MAX_DATE, true, "Maximum last-modified timestamp ISO format date. E.g. 2000-12-31.")
                .addOption(null, MIN_SIZE, true, "Minimum size in kilobytes.")
                .addOption(null, MAX_SIZE, true, "Maximum size in kilobytes.")
                .addOption("f", FORCE, false, "Download files regardless of whether a local version exists.")
                .addOption("p", PERSISTENT, false,
                        "More persistent in the handling of network errors, for unstable connections.")
                .addOption("a", AGGRESSIVE, false, "Aggressive retrieval tactics.")
                .addOption("t", THREADS, true, "The maximum number of concurrent threads.")
                .addOption(null, RELAX_SSL, false, "Relaxed SSL verification, for SSL validation errors.")
                .addOption("x", STACK_TRACE, false, "Prints stack trace on errors, useful for debugging.")
                .addOption(null, HELP, false, "Display this help and exit.")
                .addOption(null, VERSION, false, "Output version information and exit."));
    }

    private static String itemTypes() {
        return Arrays.asList(Property.ItemType.values()).stream()
                .filter(itemType -> !itemType.relativePaths().isEmpty())
                .map(itemType -> itemType.name() + " (" + itemType.relativePaths()
                        .stream().collect(Collectors.joining(" ")) + ")"
                ).collect(Collectors.joining(" "));
    }

    private final Options options;

    Args(Options options) {
        this.options = options;
    }

    public Options options() {
        return options;
    }
}
