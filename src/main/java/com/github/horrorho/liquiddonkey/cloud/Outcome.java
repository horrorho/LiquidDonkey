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
package com.github.horrorho.liquiddonkey.cloud;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Outcome.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public enum Outcome {

    WRITTEN("Written(unencrypted)", true),
    WRITTEN_DECRYPT("Written(decrypted)", true),
    FAILED_DOWNLOAD("Failed(download)", false),
    FAILED_DECRYPT_ERROR("Failed(decrypt)", false),
    FAILED_DECRYPT_NO_FILE("Failed(internal)", false),
    FAILED_DECRYPT_NO_KEY("Failed(key)", false);

    private final String toString;
    private final boolean isSuccess;

    private Outcome(String toString, boolean isSuccess) {
        this.toString = toString;
        this.isSuccess = isSuccess;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    @Override
    public String toString() {
        return toString;
    }
}
