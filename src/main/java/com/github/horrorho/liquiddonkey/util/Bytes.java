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

import com.google.protobuf.ByteString;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;

import net.jcip.annotations.ThreadSafe;
import org.apache.commons.codec.binary.Hex;

/**
 * Bytes. Bytes utilities.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class Bytes {

    private static final String[] units = new String[]{"kB", "MB", "GB", "TB", "PB", "EB"};

    public static <T> String hex(Map<ByteString, T> map, Function<T, String> function) {
        return map == null
                ? "null"
                : map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> function.apply(entry.getValue()))).toString();
    }

    public static String hex(Collection<ByteString> byteStrings) {
        return byteStrings == null
                ? "null"
                : byteStrings.stream().map(Bytes::hex).collect(Collectors.toList()).toString();
    }

    public static String hex(ByteString byteString) {
        if (byteString == null) {
            return "null";
        }
        return Hex.encodeHexString(byteString.toByteArray()).toLowerCase(Locale.US);
    }

    public static String hex(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        return Hex.encodeHexString(bytes).toLowerCase(Locale.US);
    }

    public static String humanize(long bytes) {
        return bytes < 0 ? "-" + doHumanize(-bytes) : doHumanize(bytes);
    }

    private static String doHumanize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        for (String unit : units) {
            if (bytes < 1048525) {
                // round up
                bytes += 51; // Minor rounding error < 1 byte
                return (bytes >> 10) + "." + (((bytes & 0x3FF) * 10) >> 10) + " " + unit;
            }
            bytes = bytes >> 10;
        }
        // Error
        return bytes + " X";
    }
}
