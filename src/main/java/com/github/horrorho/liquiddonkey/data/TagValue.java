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
package com.github.horrorho.liquiddonkey.data;

import com.github.horrorho.liquiddonkey.exception.BadDataException;
import static com.github.horrorho.liquiddonkey.util.Bytes.hex;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Tag Value.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class TagValue {

    public static List<TagValue> parseTagLengthValues(ByteString tagLengthValues) throws BadDataException {
        List<TagValue> tagValues = new ArrayList<>();
        int i = 0;
        while (i + 8 <= tagLengthValues.size()) {
            String tag = tagLengthValues.substring(i, i + 4).toStringUtf8();
            // Signed 32 bit length. Limited to 2 GB
            int length = tagLengthValues.substring(i + 4, i + 8).asReadOnlyByteBuffer().getInt();

            int end = i + 8 + length;
            if (length < 0 || end > tagLengthValues.size()) {
                throw new BadDataException("Bad TagLengthValue length: " + length);
            }

            TagValue tagValue = new TagValue(tag, tagLengthValues.substring(i + 8, end));
            tagValues.add(tagValue);
            i += 8 + length;
        }
        return tagValues;
    }

    private final String tag;
    private final ByteString value;

    public TagValue(String tag, ByteString value) {
        this.tag = tag;
        this.value = value;
    }

    public String tag() {
        return tag;
    }

    public ByteString value() {
        return value;
    }

    @Override
    public String toString() {
        return "TagValue{" + "tag=" + tag + ", value=0x" + hex(value) + '}';
    }
}
