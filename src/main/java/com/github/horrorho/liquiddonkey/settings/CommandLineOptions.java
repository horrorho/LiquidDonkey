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

import static com.github.horrorho.liquiddonkey.settings.Property.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.cli.Option;

/**
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class CommandLineOptions {

    public static final CommandLineOptions instance = newInstance();

    public CommandLineOptions getInstance() {
        return instance;
    }

    static CommandLineOptions newInstance() {
        Map<Property, Option> options = options();
        return new CommandLineOptions(options(), optToProperty(options));
    }

    static Map<Property, Option> options() {
        Map<Property, Option> options = new HashMap<>();

        options.put(ENGINE_AGGRESSIVE,
                new Option("a", "aggressive", false, "Aggressive retrieval tactics."));

        options.put(ENGINE_THREAD_COUNT,
                new Option("t", "threads", true, "The maximum number of concurrent threads."));

        options.put(ENGINE_PERSISTENT,
                new Option("p", "persistent", false,
                        "More persistent in the handling of network errors, for unstable connections."));

        options.put(ENGINE_PRINT_STACK_TRACE,
                new Option("x", "stack-trace", false, "Prints stack trace on errors, useful for debugging."));

        options.put(FILE_COMBINED,
                new Option("c", "combined", false, "Do not separate each snapshot into its own folder."));

        options.put(FILE_FORCE,
                new Option("f", "force", false, "Download files regardless of whether a local version exists."));

        options.put(FILE_FLAT,
                new Option("i", "--itunes-style", false, "Download files to iTunes style format."));

        options.put(FILE_OUTPUT_DIRECTORY,
                new Option("o", "output", true, "Output folder."));

        options.put(FILTER_DATE_MIN,
                new Option(null, "min-date", true,
                        "Minimum last-modified timestamp, ISO format date. E.g. 2000-12-31."));

        options.put(FILTER_DATE_MAX,
                new Option(null, "max-date", true,
                        "Maximum last-modified timestamp ISO format date. E.g. 2000-12-31."));

        options.put(FILTER_DOMAIN,
                Option.builder("d").longOpt("domain")
                .desc("Limit files to those within the specified application domain/s.")
                .hasArgs().build());

        options.put(FILTER_DATE_MIN,
                new Option(null, "min-date", true,
                        "Minimum last-modified timestamp, ISO format date. E.g. 2000-12-31."));

        options.put(FILTER_DATE_MAX,
                new Option(null, "max-date", true,
                        "Maximum last-modified timestamp ISO format date. E.g. 2000-12-31."));

        options.put(FILTER_DOMAIN,
                Option.builder("d").longOpt("domain")
                .desc("Limit files to those within the specified application domain/s.")
                .hasArgs().build());

        options.put(FILTER_EXTENSION,
                Option.builder("e").longOpt("extension")
                .desc("Limit files to those with the specified extension/s.")
                .hasArgs().build());

        options.put(FILTER_ITEM_TYPES,
                Option.builder(null).longOpt("item-types")
                .desc("Only download the specified item type/s:\n")
                .hasArgs().build());

        options.put(FILTER_RELATIVE_PATH,
                Option.builder("r").longOpt("relative-path")
                .desc("Limit files to those with the specified relative path/s")
                .hasArgs().build());

        options.put(FILTER_SIZE_MIN,
                new Option(null, "min-size", true, "Minimum size in kilobytes."));

        options.put(FILTER_SIZE_MAX,
                new Option(null, "max-size", true, "Maximum size in kilobytes."));

        options.put(HTTP_RELAX_SSL,
                new Option(null, "relax-ssl", false, "Relaxed SSL verification, for SSL validation errors."));

        options.put(SELECTION_SNAPSHOT,
                Option.builder("s").longOpt("snapshot")
                .desc("Only download data in the snapshot/s specified.\n"
                        + "Negative numbers indicate relative positions from newest backup "
                        + "with -1 being the newest, -2 second newest, etc.")
                .hasArgs().build());

        options.put(SELECTION_UDID,
                Option.builder("u").longOpt("udid")
                .desc("Download the backup/s with the specified UDID/s. "
                        + "Will match partial UDIDs. Leave empty to download all.")
                .hasArgs().optionalArg(true).build());

        return options;
    }

    static Map<String, Property> optToProperty(Map<Property, Option> options) {
        return Stream.of(Property.values())
                .map(property -> Arrays.asList(
                                new SimpleEntry<>(options.get(property).getOpt(), property),
                                new SimpleEntry<>(options.get(property).getLongOpt(), property)))
                .flatMap(List::stream)
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private final Map<Property, Option> options;
    private final Map<String, Property> optToProperty;

    public CommandLineOptions(Map<Property, Option> options, Map<String, Property> optToProperty) {
        // Defensive deep copy
        this.options
                = options.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Option) entry.getValue().clone()));
        this.optToProperty = new HashMap<>(optToProperty);
    }

    public Option option(Property property) {
        return (Option) options.get(property);
    }

    public String opt(Property property) {
        return opt(options.get(property));
    }

    public String opt(Option option) {
        return option.hasLongOpt()
                ? option.getLongOpt()
                : option.getOpt();
    }

    public Property property(Option option) {
        return optToProperty.get(opt(option));
    }

}