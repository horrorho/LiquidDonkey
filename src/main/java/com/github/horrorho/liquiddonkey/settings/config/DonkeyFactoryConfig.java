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
 * DonkeyFactory configuration.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class DonkeyFactoryConfig {

    public static DonkeyFactoryConfig newInstance(Configuration config) {

        return DonkeyFactoryConfig.newInstance(
                config.get(Property.ENGINE_AGGRESSIVE, config::asBoolean),
                config.get(Property.FILE_FORCE, config::asBoolean),
                config.get(Property.FILE_SET_LAST_MODIFIED_TIMESTAMP, config::asBoolean),
                config.get(Property.ENGINE_BATCH_SIZE_MINIMUM, config::asInteger));
    }

    public static DonkeyFactoryConfig newInstance(
            boolean isAggressive,
            boolean toForceOverwrite,
            boolean toSetLastModifiedTime,
            long batchSizeBytes) {

        return new DonkeyFactoryConfig(
                isAggressive,
                toForceOverwrite,
                toSetLastModifiedTime,
                batchSizeBytes);
    }

    private final boolean isAggressive;
    private final boolean toForceOverwrite;
    private final boolean toSetLastModifiedTime;
    private final long batchSizeBytes;

    DonkeyFactoryConfig(
            boolean isAggressive,
            boolean toForceOverwrite,
            boolean toSetLastModifiedTime,
            long batchSizeBytes) {

        this.isAggressive = isAggressive;
        this.toForceOverwrite = toForceOverwrite;
        this.toSetLastModifiedTime = toSetLastModifiedTime;
        this.batchSizeBytes = batchSizeBytes;
    }

    public boolean isAggressive() {
        return isAggressive;
    }

    public boolean toForceOverwrite() {
        return toForceOverwrite;
    }

    public boolean toSetLastModifiedTime() {
        return toSetLastModifiedTime;
    }

    public long batchSizeBytes() {
        return batchSizeBytes;
    }

    @Override
    public String toString() {
        return "DonkeyFactoryConfig{" + "isAggressive=" + isAggressive + ", toForceOverwrite=" + toForceOverwrite
                + ", toSetLastModifiedTime=" + toSetLastModifiedTime + ", batchSizeBytes=" + batchSizeBytes + '}';
    }
}
