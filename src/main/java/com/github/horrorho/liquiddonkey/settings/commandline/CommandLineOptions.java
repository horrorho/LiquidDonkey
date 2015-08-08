/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a flatCopy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, flatCopy, modify, merge, publish, distribute, sublicense, and/or sell
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
package com.github.horrorho.liquiddonkey.settings.commandline;

import com.github.horrorho.liquiddonkey.settings.Property;
import static com.github.horrorho.liquiddonkey.settings.Property.*;
import com.github.horrorho.liquiddonkey.util.Props;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CommandLineOptions.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class CommandLineOptions {

    public static CommandLineOptions from(Properties properties) {
        LinkedHashMap<Property, Option> propertyToOption = propertyToOption(itemTypes(properties));

        return new CommandLineOptions(
                propertyToOption,
                optToProperty(propertyToOption));
    }

    static LinkedHashMap<Property, Option> propertyToOption(String itemTypes) {
        LinkedHashMap<Property, Option> options = new LinkedHashMap<>();

        options.put(FILE_OUTPUT_DIRECTORY,
                new Option("o", "output", true, "Output folder."));

        options.put(FILE_COMBINED,
                new Option("c", "combined", false, "Do not separate each snapshot into its own folder."));

        options.put(SELECTION_UDID,
                Option.builder("u").longOpt("udid")
                .desc("Download the backup/s with the specified UDID/s. "
                        + "Will match partial UDIDs. Leave empty to download all.")
                .argName("hex")
                .hasArgs().optionalArg(true).build());

        options.put(SELECTION_SNAPSHOT,
                Option.builder("s").longOpt("snapshot")
                .desc("Only download data in the snapshot/s specified.\n"
                        + "Negative numbers indicate relative positions from newest backup "
                        + "with -1 being the newest, -2 second newest, etc.")
                .argName("int")
                .hasArgs().build());

        options.put(FILTER_ITEM_TYPES,
                Option.builder(null).longOpt("item-types")
                .desc("Only download the specified item type/s:\n" + itemTypes)
                .argName("item_type")
                .hasArgs().build());

        options.put(FILTER_DOMAIN,
                Option.builder("d").longOpt("domain")
                .desc("Limit files to those within the specified application domain/s.")
                .argName("str")
                .hasArgs().build());

        options.put(FILTER_RELATIVE_PATH,
                Option.builder("r").longOpt("relative-path")
                .desc("Limit files to those with the specified relative path/s")
                .argName("str")
                .hasArgs().build());

        options.put(FILTER_EXTENSION,
                Option.builder("e").longOpt("extension")
                .desc("Limit files to those with the specified extension/s.")
                .argName("str")
                .hasArgs().build());

        options.put(FILTER_DATE_MIN,
                Option.builder().longOpt("min-date")
                .desc("Minimum last-modified timestamp, ISO format date. E.g. 2000-12-31.")
                .argName("date")
                .hasArgs().build());

        options.put(FILTER_DATE_MAX,
                Option.builder().longOpt("max-date")
                .desc("Maximum last-modified timestamp, ISO format date. E.g. 2000-12-31.")
                .argName("date")
                .hasArgs().build());

        options.put(FILTER_SIZE_MIN,
                Option.builder().longOpt("min-size")
                .desc("Minimum size in kilobytes.")
                .argName("Kb")
                .hasArgs().build());

        options.put(FILTER_SIZE_MAX,
                Option.builder().longOpt("max-size")
                .desc("Maximum size in kilobytes.")
                .argName("Kb")
                .hasArgs().build());

        options.put(ENGINE_FORCE_OVERWRITE,
                new Option("f", "force", false, "Download files regardless of whether a local version exists."));

        options.put(ENGINE_PERSISTENT,
                new Option("p", "persistent", false,
                        "More persistent in the handling of network errors, for unstable connections."));

        options.put(ENGINE_AGGRESSIVE,
                new Option("a", "aggressive", false, "Aggressive retrieval tactics."));

        options.put(ENGINE_THREAD_COUNT,
                Option.builder("t").longOpt("threads")
                .desc("The maximum number of concurrent threads.")
                .argName("int")
                .hasArgs().build());

        options.put(HTTP_RELAX_SSL,
                new Option(null, "relax-ssl", false, "Relaxed SSL verification, for SSL validation errors."));

        options.put(DEBUG_REPORT,
                new Option("w", "report", false, "Write out rudimentary reports."));

        options.put(DEBUG_PRINT_STACK_TRACE,
                new Option("x", "stack-trace", false, "Print stack trace on errors, useful for debugging."));

        options.put(ENGINE_DUMP_TOKEN,
                new Option(null, "token", false,
                        "Output authentication token and exit."));

        options.put(COMMAND_LINE_HELP,
                new Option(null, "help", false, "Display this help and exit."));

        options.put(COMMAND_LINE_VERSION,
                new Option(null, "version", false, "Output version information and exit."));

//        options.put(FILE_FLAT,
//                new Option("i", "--itunes-style", false, "Download files to iTunes style format."));
        return options;
    }

    static String itemTypes(Properties properties) {
        Props<Property> props = Props.from(properties);

        String prefix = props.getProperty(CONFIG_PREFIX_ITEM_TYPE);
        if (prefix == null) {
            logger.warn("-- itemTypes() > no item type prefix: {}", CONFIG_PREFIX_ITEM_TYPE);
            return "";
        }

        int substring = prefix.length();

        return Stream.of(Property.values())
                .filter(property -> property.name().startsWith(prefix))
                .filter(property -> !props.getProperty(property, props::asList).isEmpty())
                .map(property -> {
                    String type = property.name().substring(substring);
                    String paths = props.getProperty(property, props::asList).stream().collect(Collectors.joining(" "));
                    return type + "(" + paths + ")";
                }).collect(Collectors.joining(" "));
    }

    static Map<String, Property> optToProperty(Map<Property, Option> options) {
        return options.entrySet().stream()
                .map(set -> Arrays.asList(
                                new SimpleEntry<>(set.getValue().getOpt(), set.getKey()),
                                new SimpleEntry<>(set.getValue().getLongOpt(), set.getKey())))
                .flatMap(List::stream)
                .filter(set -> set.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static final Logger logger = LoggerFactory.getLogger(CommandLineOptions.class);

    private final LinkedHashMap<Property, Option> propertyToOption;
    private final Map<String, Property> optToProperty;

    CommandLineOptions(
            LinkedHashMap<Property, Option> propertyToOption,
            Map<String, Property> optToProperty) {
        // No defensive copies
        this.propertyToOption = Objects.requireNonNull(propertyToOption);
        this.optToProperty = Objects.requireNonNull(optToProperty);
    }

    public Options options() {
        Options options = new Options();

        propertyToOption.values().stream()
                .map(option -> (Option) option.clone())
                .forEach(options::addOption);

        return options;
    }

    public Option option(Property property) {
        return (Option) propertyToOption.get(property);
    }

    public String opt(Property property) {
        return opt(propertyToOption.get(property));
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
