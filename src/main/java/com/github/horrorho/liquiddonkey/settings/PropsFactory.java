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

import com.github.horrorho.liquiddonkey.settings.CommandLineOptions;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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
 * PropsFactory.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public class PropsFactory {

    private static final Logger logger = LoggerFactory.getLogger(PropsFactory.class);

    private static final PropsFactory instance = new PropsFactory();

    public static PropsFactory getInstance() {
        return instance;
    }

    PropsFactory() {
    }

    public Props fromCommandLine(Props defaults, CommandLineOptions commandLineOptions, String[] args)
            throws ParseException {

        Props props = Props.newInstance(defaults);
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

    public Props fromUrl(Props defaults, String url) {
        Properties properties = new Properties();
        try (InputStream inputStream = this.getClass().getResourceAsStream(url)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ex) {
            logger.warn("-- fromUrl() > excepion: ", ex);
        }

        Props props = Props.newInstance(defaults);
        return props.addAll(properties);
    }

    public Props fromPropertyDefaults() {
        Props props = Props.newInstance();
        Stream.of(Property.values())
                .filter(property -> property.getDefaultValue() != null)
                .forEach(property -> props.put(property, property.getDefaultValue()));
        return props;
    }

    String joined(String... list) {
        return list == null
                ? ""
                : Arrays.asList(list).stream().collect(Collectors.joining(" "));
    }
}
