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
import java.time.format.DateTimeFormatter;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigFactory.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class ConfigFactory {

    public static ConfigFactory getInstance() {
        return instance;
    }

    private static final ConfigFactory instance = new ConfigFactory();
    private static final Logger logger = LoggerFactory.getLogger(ConfigFactory.class);

    ConfigFactory() {
    }

    public Config from(String[] args) {
        logger.trace("<< parse() < {}", (Object) args);
        try {
            Args arguments = Args.newStandardInstance();
            CommandLineHelper helper = CommandLineHelper.getInstance(arguments.options(), args);

            if (helper.line().hasOption(HELP)) {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.setOptionComparator(null);
                helpFormatter.printHelp("DonkeyLooter appleid password [OPTION]...", arguments.options());
                return null;
            }

            if (helper.line().hasOption(VERSION)) {
                System.out.println(Property.Str.PROJECT_VERSION.string());
                return null;
            }

            switch (helper.line().getArgList().size()) {
                case 0:
                    throw new ParseException("Missing appleid and password.");
                case 1:
                    throw new ParseException("Missing password.");
                case 2:
                    break;
                default:
                    throw new ParseException("Too many non-optional arguments, expected appleid and password only.");
            }

            Config config = Config.newInstance(helper);
            logger.trace(">> parse() > {}", config);
            return config;

        } catch (ParseException | IllegalArgumentException ex) {
            System.out.println(ex.getLocalizedMessage());
            System.out.println("Try '--help' for more information.");
            return null;
        }
    }
}
