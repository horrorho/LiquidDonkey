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
package com.github.horrorho.liquiddonkey.settings.old;

import com.github.horrorho.liquiddonkey.settings.config.ConfigFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public enum SettingsProperties {

    INSTANCE;

    private static final String PROP_FILE = "/settings.properties";
    private final Properties properties;

    private SettingsProperties() {
        Logger log = LoggerFactory.getLogger(SettingsProperties.class);
        properties = new Properties();
        try (InputStream inputStream = ConfigFactory.class.getResourceAsStream(PROP_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
                log.trace("** SettingProperties() < properties: {}", properties);
            } else {
                log.warn("** SettingProperties() > missing properties file: {}", PROP_FILE);
            }
        } catch (IOException ex) {
            log.trace("** SettingProperties() > unable to retrieve properties file: ", ex);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public Properties properties() {
        Properties copy = new Properties();
        Enumeration enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            Object next = enumeration.nextElement();
            copy.put(next, properties.get(next));
        }
        return copy;
    }
}
