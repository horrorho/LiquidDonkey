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

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Ahseya
 */
public class CommandLinePropsFactory {

    public CommandLinePropsFactory instance = new CommandLinePropsFactory();

    public CommandLinePropsFactory getInstance() {
        return instance;
    }

    CommandLinePropsFactory() {
    }

    public Props<Property> fromCommandLine(
            Props<Property> defaults,
            CommandLineOptions commandLineOptions,
            String[] args)
            throws ParseException {

        Props<Property> props = Props.newInstance(Property.class, defaults);
        CommandLineParser parser = new DefaultParser();
        Options options = commandLineOptions.options();
        CommandLine cmd = parser.parse(options, args);

        switch (cmd.getArgList().size()) {
            case 0:
                // No authentication credentials
                break;
            case 1:
                // Authentication token
                props.put(Property.AUTHENTICATION_TOKEN, cmd.getArgList().get(0));
                break;
            case 2:
                // AppleId/ password pair
                props.put(Property.AUTHENTICATION_APPLEID, cmd.getArgList().get(0));
                props.put(Property.AUTHENTICATION_PASSWORD, cmd.getArgList().get(1));
                break;
            default:
                throw new ParseException(
                        "Too many non-optional arguments, expected appleid/ password or authentication token only.");
        }

        Iterator<Option> it = cmd.iterator();

        while (it.hasNext()) {
            Option option = it.next();
            String opt = commandLineOptions.opt(option);
            Property property = commandLineOptions.property(option);

            if (option.hasArgs()) {
                // String array
                props.put(
                        property,
                        joined(cmd.getOptionValues(opt)));
            } else if (option.hasArg()) {
                // String value
                props.put(
                        property,
                        cmd.getOptionValue(opt));
            } else {
                // String boolean
                props.put(
                        property,
                        Boolean.toString(cmd.hasOption(opt)));
            }
        }
        return props;
    }

    String joined(String... list) {
        return list == null
                ? ""
                : Arrays.asList(list).stream().collect(Collectors.joining(" "));
    }
}
