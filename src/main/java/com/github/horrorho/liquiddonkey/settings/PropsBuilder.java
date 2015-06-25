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

import com.github.horrorho.liquiddonkey.iofunction.IOSupplier;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PropsBuilder.
 *
 * @author Ahseya
 */
@NotThreadSafe
public class PropsBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PropsBuilder.class);

    public static PropsBuilder fromDefaults() {
        return new PropsBuilder(null)
                .defaults().resource(Property.PROPERTIES_JAR).path(Property.PROPERTIES_USER);
    }

    private Props props;

    PropsBuilder(Props props) {
        this.props = props;
    }

    public PropsBuilder commandLine(CommandLineOptions commandLineOptions, String[] args)
            throws ParseException {

        props = Props.newInstance(props);
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
        return this;
    }

    public PropsBuilder resource(Property url) {
        if (!props.contains(url)) {
            logger.warn("-- resource() > missing url property: {}", url);
            return this;
        }
        logger.debug("-- resource() > url: {} / {}", url, props.get(url));
        return inputStream(() -> this.getClass().getResourceAsStream(props.get(url)));
    }

    public PropsBuilder path(Property path) {
        if (!props.contains(path)) {
            logger.warn("-- path() > missing path property: {}", path);
            return this;
        }
        logger.debug("-- path() > path: {} / {}", path, props.get(path));
        return inputStream(() -> Files.newInputStream(Paths.get(props.get(path)), READ));
    }

    public PropsBuilder inputStream(IOSupplier<InputStream> supplier) {
        props = Props.newInstance(props);
        Properties properties = new Properties();

        try (InputStream inputStream = supplier.get()) {
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                logger.warn("-- inputStream() > null InputStream");
            }
        } catch (IOException ex) {
            logger.warn("-- inputStream() > exception: {}", ex);
        }
        return this;
    }

    public Props build() {
        return props;
    }

    PropsBuilder defaults() {
        props = Props.newInstance(props);
        Stream.of(Property.values())
                .filter(property -> property.getDefaultValue() != null)
                .forEach(property -> props.put(property, property.getDefaultValue()));
        return this;
    }

    String joined(String... list) {
        return list == null
                ? ""
                : Arrays.asList(list).stream().collect(Collectors.joining(" "));
    }
}
