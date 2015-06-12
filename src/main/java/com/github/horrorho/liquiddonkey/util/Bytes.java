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
import com.google.protobuf.ByteString;
import java.util.Locale;
import net.jcip.annotations.Immutable;

import net.jcip.annotations.ThreadSafe;
import org.apache.commons.codec.binary.Hex;

/**
 * Utilities.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class Bytes {

    private static final String[] units = new String[]{"kB", "MB", "GB", "TB", "PB", "EB"};

    public static int integerOrFail(ByteString byteString) throws BadDataException {
        if (byteString == null) {
            throw new BadDataException("Integer. Null ByteString");
        }
        if (byteString.size() != 4) {
            throw new BadDataException("Integer. Expected data size of 4 bytes. Got: " + byteString.size());
        }
        return byteString.asReadOnlyByteBuffer().getInt();
    }

    public static String hex(ByteString byteString) {
        if (byteString == null) {
            return "null";
        }
        return Hex.encodeHexString(byteString.toByteArray()).toLowerCase(Locale.getDefault());
    }

    public static String hex(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        return Hex.encodeHexString(bytes).toLowerCase(Locale.getDefault());
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
