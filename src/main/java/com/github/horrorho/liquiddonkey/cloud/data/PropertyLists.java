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
package com.github.horrorho.liquiddonkey.cloud.data;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import java.io.IOException;
import java.text.ParseException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * PropertyLists. Helper methods.
 *
 * @author Ahseya
 */
class PropertyLists {

    static <T> T parse(byte[] data) throws BadDataException {
        try {
            return (T) PropertyListParser.parse(data);

        } catch (ClassCastException |
                IOException |
                PropertyListFormatException |
                ParseException |
                ParserConfigurationException |
                SAXException ex) {

            throw new BadDataException("Failed to parse property list", ex);
        }
    }

    static <T> T get(NSDictionary dictionary, String key) throws BadDataException {
        NSObject object = dictionary.get(key);
        if (object == null) {
            throw new BadDataException("Missing key: " + key);
        }

        try {
            return (T) object;
        } catch (ClassCastException ex) {
            throw new BadDataException("Bad data type", ex);
        }
    }

    static String string(NSDictionary dictionary, String key) throws BadDataException {
        NSObject object = dictionary.get(key);
        if (object == null) {
            throw new BadDataException("Missing key: " + key);
        }
        return object.toString();
    }

    static String string(NSDictionary dictionary, String key, String defaultValue) {
        NSObject object = dictionary.get(key);
        return object == null
                ? defaultValue
                : object.toString();
    }
}
