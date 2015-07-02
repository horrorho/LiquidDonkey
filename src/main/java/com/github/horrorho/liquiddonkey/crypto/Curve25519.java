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
package com.github.horrorho.liquiddonkey.crypto;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.bouncycastle.util.Arrays;

/**
 * Curve25519 elliptical curve.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class Curve25519 {

    private static final Curve25519 INSTANCE = new Curve25519();

    public static Curve25519 getInstance() {
        return INSTANCE;
    }

    private final org.whispersystems.curve25519.Curve25519 curve25519
            = org.whispersystems.curve25519.Curve25519.getInstance(org.whispersystems.curve25519.Curve25519.JAVA);

    Curve25519() {
    }

    public byte[] agreement(byte[] publicKey, byte[] privateKey) {
        // org.whispersystems.curve25519.BaseJavaCurve25591Provider#calculateAgreement appears to be thread safe.
        return curve25519.calculateAgreement(publicKey, clampPrivateKey(privateKey));
    }

    byte[] clampPrivateKey(byte[] privateKey) {
        byte[] copy = Arrays.copyOf(privateKey, privateKey.length);
        copy[0] &= 0xF8;
        copy[31] &= 0x7F;
        copy[31] |= 0x40;
        return copy;
    }
}
