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

import com.github.horrorho.liquiddonkey.settings.Configuration;
import com.github.horrorho.liquiddonkey.settings.Property;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * DonkeyExecutor configuration.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class DonkeyExecutorConfig {

    public static DonkeyExecutorConfig newInstance(Configuration config) {
        return newInstance(
                config.get(Property.ENGINE_THREAD_COUNT, config::asInteger),
                config.get(Property.ENGINE_THREAD_STAGGER_DELAY, config::asInteger),
                config.get(Property.ENGINE_AGGRESSIVE, config::asBoolean)
                        ? 2 // TODO
                        : 1);
    }

    public static DonkeyExecutorConfig newInstance(int threads, int staggerDelayMs, int retryCount) {
        return new DonkeyExecutorConfig(threads, staggerDelayMs, retryCount);
    }

    private final int threads;
    private final int staggerDelayMs;
    private final int retryCount;

    DonkeyExecutorConfig(int threads, int staggerDelayMs, int retryCount) {
        this.threads = threads;
        this.staggerDelayMs = staggerDelayMs;
        this.retryCount = retryCount;
    }

    public int threads() {
        return threads;
    }

    public int staggerDelayMs() {
        return staggerDelayMs;
    }

    public int retryCount() {
        return retryCount;
    }

    @Override
    public String toString() {
        return "DonkeyExecutorConfig{" + "threads=" + threads + ", staggerDelayMs=" + staggerDelayMs
                + ", retryCount=" + retryCount + '}';
    }
}
