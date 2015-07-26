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
package com.github.horrorho.liquiddonkey.settings.commandline;

import com.github.horrorho.liquiddonkey.iofunction.IOSupplier;
import com.github.horrorho.liquiddonkey.settings.Property;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.READ;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CommandLinePropertiesFactory.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class CommandLinePropertiesFactory {

    private static final Logger logger = LoggerFactory.getLogger(CommandLinePropertiesFactory.class);

    private static final CommandLinePropertiesFactory instance = new CommandLinePropertiesFactory();

    public static CommandLinePropertiesFactory create() {
        return instance;
    }

    CommandLinePropertiesFactory() {
    }

    public Properties from(Properties parent, CommandLineOptions commandLineOptions, String[] args)
            throws ParseException { 
        
        Properties properties = new Properties(parent); 
        CommandLineParser parser = new DefaultParser();
        Options options = commandLineOptions.options();
        CommandLine cmd = parser.parse(options, args);

        switch (cmd.getArgList().size()) {
            case 0:
                // No authentication credentials
                break;
            case 1:
                // Authentication token
                properties.put(Property.AUTHENTICATION_TOKEN.name(), cmd.getArgList().get(0));
                break;
            case 2:
                // AppleId/ password pair
                properties.put(Property.AUTHENTICATION_APPLEID.name(), cmd.getArgList().get(0));
                properties.put(Property.AUTHENTICATION_PASSWORD.name(), cmd.getArgList().get(1));
                break;
            default:
                throw new ParseException(
                        "Too many non-optional arguments, expected appleid/ password or authentication token only.");
        }

        Iterator<Option> it = cmd.iterator();

        while (it.hasNext()) {
            Option option = it.next();
            String opt = commandLineOptions.opt(option);
            String property = commandLineOptions.property(option).name();

            if (option.hasArgs()) {
                // String array
                properties.put(property, joined(cmd.getOptionValues(opt)));
            } else if (option.hasArg()) {
                // String value
                properties.put(property, cmd.getOptionValue(opt));
            } else {
                // String boolean
                properties.put(property, Boolean.toString(cmd.hasOption(opt)));
            }
        }
        return properties;
    }

    String joined(String... list) {
        return list == null
                ? ""
                : Arrays.asList(list).stream().collect(Collectors.joining(" "));
    }
}
