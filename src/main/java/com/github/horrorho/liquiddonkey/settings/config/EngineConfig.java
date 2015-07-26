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
                isAggressive,
                props.getProperty(Property.ENGINE_BATCH_SIZE_MINIMUM_BYTES, props::asLong),
                isAggressive ? props.getProperty(Property.ENGINE_RETRY_AGGRESSIVE, props::asInteger)
                        : props.getProperty(Property.ENGINE_DOWNLOAD_RETRY, props::asInteger),
                props.getProperty(Property.ENGINE_PERSISTENT, props::asBoolean),
                props.getProperty(Property.ENGINE_RETRY_DELAY_MS, props::asInteger),
                props.getProperty(Property.ENGINE_THREAD_STAGGER_DELAY, props::asInteger),
                props.getProperty(Property.ENGINE_THREAD_COUNT, props::asInteger),
                props.getProperty(Property.ENGINE_FORCE_OVERWRITE, props::asBoolean),
                props.getProperty(Property.ENGINE_SET_LAST_MODIFIED_TIMESTAMP, props::asBoolean),
                props.getProperty(Property.ENGINE_DUMP_TOKEN, props::asBoolean));
    }

    static EngineConfig from(
            boolean isAggressive,
            long batchSizeMinimumBytes,
            int retryCount,
            boolean isPersistent,
            int retryDelay,
            int threadStaggerDelay,
            int threadCount,
            boolean toForceOverwrite,
            boolean toSetLastModifiedTimestamp,
            boolean toDumpToken) {

        return new EngineConfig(isAggressive,
                batchSizeMinimumBytes,
                retryCount,
                isPersistent,
                retryDelay,
                threadStaggerDelay,
                threadCount,
                toForceOverwrite,
                toSetLastModifiedTimestamp,
                toDumpToken);
    }

    private final boolean isAggressive;
    private final long batchSizeMinimumBytes;
    private final int retryCount;
    private final boolean isPersistent;
    private final int retryDelay;
    private final int threadStaggerDelay;
    private final int threadCount;
    private final boolean toForceOverwrite;
    private final boolean toSetLastModifiedTimestamp;
    private final boolean toDumpToken;

    EngineConfig(
            boolean isAggressive,
            long batchSizeMinimumBytes,
            int retryCount,
            boolean isPersistent,
            int retryDelay,
            int threadStaggerDelay,
            int threadCount,
            boolean toForceOverwrite,
            boolean toSetLastModifiedTimestamp,
            boolean toDumpToken) {

        this.isAggressive = isAggressive;
        this.batchSizeMinimumBytes = batchSizeMinimumBytes;
        this.retryCount = retryCount;
        this.isPersistent = isPersistent;
        this.retryDelay = retryDelay;
        this.threadStaggerDelay = threadStaggerDelay;
        this.threadCount = threadCount;
        this.toForceOverwrite = toForceOverwrite;
        this.toSetLastModifiedTimestamp = toSetLastModifiedTimestamp;
        this.toDumpToken = toDumpToken;
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

    public boolean isPersistent() {
        return isPersistent;
    }

    public int retryDelay() {
        return retryDelay;
    }

    public int threadStaggerDelay() {
        return threadStaggerDelay;
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
                + "isAggressive=" + isAggressive
                + ", batchSizeMinimumBytes=" + batchSizeMinimumBytes
                + ", chunkListDownloadRetry=" + retryCount
                + ", isPersistent=" + isPersistent
                + ", retryDelay=" + retryDelay
                + ", threadStaggerDelay=" + threadStaggerDelay
                + ", threadCount=" + threadCount
                + ", toForceOverwrite=" + toForceOverwrite
                + ", toSetLastModifiedTimestamp=" + toSetLastModifiedTimestamp
                + '}';
    }
}
