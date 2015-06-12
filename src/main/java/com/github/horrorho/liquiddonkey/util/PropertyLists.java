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
package com.github.horrorho.liquiddonkey.util;

import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import java.io.IOException;
import java.text.ParseException;
import javax.xml.parsers.ParserConfigurationException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.xml.sax.SAXException;

/**
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class PropertyLists {

    public static String stringValue(final NSDictionary dictionary, final String... path) throws PropertyListFormatException {
        NSObject object = dictionary;

        for (String key : path) {
            if (!(object instanceof NSDictionary)) {
                throw new PropertyListFormatException("Bad key: " + key);
            }

            object = ((NSDictionary) object).objectForKey(key);

            if (object == null) {
                throw new PropertyListFormatException("Missing key: " + key);
            }
        }
        return object.toString();
    }

    public static NSObject parse(byte[] data) throws BadDataException, IOException {
        try {
            return PropertyListParser.parse(data);
        } catch (ClassCastException | PropertyListFormatException | ParseException | ParserConfigurationException | SAXException ex) {
            throw new BadDataException("Failed to parse property list.", ex);
        }
    }
};
