/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a flatCopy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, flatCopy, modify, merge, publish, distribute, sublicense, and/or sell
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

import com.github.horrorho.liquiddonkey.util.Props;
import com.github.horrorho.liquiddonkey.settings.Property;
import java.util.Properties;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * EngineConfig configuration.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class EngineConfig {

    public static EngineConfig from(Properties properties) {
        Props<Property> props = Props.from(properties);

        boolean isAggressive = props.getProperty(Property.ENGINE_AGGRESSIVE, props::asBoolean);

        return EngineConfig.from(
                isAggressive ? props.getProperty(Property.ENGINE_DOWNLOAD_RETRY_AGGRESSIVE, props::asInteger)
                        : props.getProperty(Property.ENGINE_DOWNLOAD_RETRY, props::asInteger),
                props.getProperty(Property.ENGINE_RETRY_DELAY_MS, props::asInteger),
                props.getProperty(Property.ENGINE_THREAD_STAGGER_DELAY_MS, props::asInteger),
                props.getProperty(Property.ENGINE_THREAD_COUNT, props::asInteger),
                props.getProperty(Property.ENGINE_TIMEOUT_MS, props::asInteger),
                isAggressive,
                props.getProperty(Property.ENGINE_FORCE_OVERWRITE, props::asBoolean),
                props.getProperty(Property.ENGINE_SET_LAST_MODIFIED_TIMESTAMP, props::asBoolean),
                props.getProperty(Property.ENGINE_DUMP_TOKEN, props::asBoolean),
                props.getProperty(Property.ENGINE_BATCH_SIZE_MINIMUM_BYTES, props::asLong)
        );
    }

    public static EngineConfig from(
            int retryCount,
            int retryDelayMs,
            int threadStaggerDelayMs,
            int threadCount,
            int timeoutMs,
            boolean isAggressive,
            boolean toForceOverwrite,
            boolean toSetLastModifiedTimestamp,
            boolean toDumpToken,
            long batchSizeMinimumBytes) {

        return new EngineConfig(retryCount,
                retryDelayMs,
                threadStaggerDelayMs,
                threadCount,
                timeoutMs,
                isAggressive,
                toForceOverwrite,
                toSetLastModifiedTimestamp,
                toDumpToken,
                batchSizeMinimumBytes);
    }

    private final int retryCount;
    private final int retryDelayMs;
    private final int threadStaggerDelayMs;
    private final int threadCount;
    private final int timeoutMs;
    private final boolean isAggressive;
    private final boolean toForceOverwrite;
    private final boolean toSetLastModifiedTimestamp;
    private final boolean toDumpToken;
    private final long batchSizeMinimumBytes;

    EngineConfig(
            int retryCount,
            int retryDelayMs,
            int threadStaggerDelayMs,
            int threadCount,
            int timeoutMs,
            boolean isAggressive,
            boolean toForceOverwrite,
            boolean toSetLastModifiedTimestamp,
            boolean toDumpToken,
            long batchSizeMinimumBytes) {

        this.retryCount = retryCount;
        this.retryDelayMs = retryDelayMs;
        this.threadStaggerDelayMs = threadStaggerDelayMs;
        this.threadCount = threadCount;
        this.timeoutMs = timeoutMs;
        this.isAggressive = isAggressive;
        this.toForceOverwrite = toForceOverwrite;
        this.toSetLastModifiedTimestamp = toSetLastModifiedTimestamp;
        this.toDumpToken = toDumpToken;
        this.batchSizeMinimumBytes = batchSizeMinimumBytes;
    }

    public boolean isAggressive() {
        return isAggressive;
    }

    public long batchSizeMinimumBytes() {
        return batchSizeMinimumBytes;
    }

    public int retryCount() {
        return retryCount;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public int retryDelayMs() {
        return retryDelayMs;
    }

    public int threadStaggerDelayMs() {
        return threadStaggerDelayMs;
    }

    public int threadCount() {
        return threadCount;
    }

    public boolean toDumpToken() {
        return toDumpToken;
    }

    public boolean toForceOverwrite() {
        return toForceOverwrite;
    }

    public boolean toSetLastModifiedTimestamp() {
        return toSetLastModifiedTimestamp;
    }

    @Override
    public String toString() {
        return "EngineConfig{"
                + "retryCount=" + retryCount
                + ", retryDelayMs=" + retryDelayMs
                + ", threadStaggerDelayMs=" + threadStaggerDelayMs
                + ", threadCount=" + threadCount
                + ", timeoutMs=" + timeoutMs
                + ", isAggressive=" + isAggressive
                + ", toForceOverwrite=" + toForceOverwrite
                + ", toSetLastModifiedTimestamp=" + toSetLastModifiedTimestamp
                + ", toDumpToken=" + toDumpToken
                + ", batchSizeMinimumBytes=" + batchSizeMinimumBytes
                + '}';
    }
}
