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
package com.github.horrorho.liquiddonkey.cloud.file;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFileAttributes;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * IOS file system type.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public enum Mode {

    DIRECTORY,
    FILE,
    SYMBOLIC_LINK,
    NO_ATTRIBUTES,
    NO_MODE,
    OTHER;

    /**
     * Returns the Mode type for the specified file.
     *
     * @param file
     * @return the Mode type for the specified file
     */
    public static Mode mode(MBSFile file) {
        if (file == null || !file.hasAttributes()) {
            return NO_ATTRIBUTES;
        }
        MBSFileAttributes attributes = file.getAttributes();
        if (!attributes.hasMode()) {
            return NO_MODE;
        }

        return mode(attributes.getMode());
    }

    static Mode mode(int mode) {

        switch (mode & 0xE000) {
            case 0x4000:
                return DIRECTORY;
            case 0x8000:
                return FILE;
            case 0xA000:
                return SYMBOLIC_LINK;
            default:
                return OTHER;
        }
    }

    static boolean isFile(MBSFile file) {
        return mode(file) == FILE;
    }

    static boolean isDirectory(MBSFile file) {
        return mode(file) == DIRECTORY;
    }

    static boolean isSymbolicLink(MBSFile file) {
        return mode(file) == SYMBOLIC_LINK;
    }
}
