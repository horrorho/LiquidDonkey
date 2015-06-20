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

import com.github.horrorho.liquiddonkey.settings.CommandLineConfiguration;
import com.github.horrorho.liquiddonkey.settings.CommandLineOptions;
import com.github.horrorho.liquiddonkey.settings.PropertiesConfiguration;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
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

            Configuration configuration = Configuration.newInstance();

            try {
                Properties file = PropertiesConfiguration.getInstance().properties();
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(ConfigFactory.class.getName()).log(Level.SEVERE, null, ex);
            }

            Properties properties = CommandLineConfiguration.newInstance().properties(CommandLineOptions.getInstance(), args, "1");

            properties.forEach((k, v) -> System.out.println(k + "=" + v));

            Config config = Config.newInstance(configuration);

            logger.trace(">> parse() > {}", config);
            return config;

        } catch (ParseException | IllegalArgumentException ex) {
            System.out.println(ex.getLocalizedMessage());
            System.out.println("Try '--help' for more information.");
            return null;
        }
    }
}
