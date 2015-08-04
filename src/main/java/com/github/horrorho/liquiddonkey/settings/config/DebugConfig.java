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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Printer configuration.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public class DebugConfig {

    public static DebugConfig from(Properties properties) {
        Props<Property> props = Props.from(properties);

        return from(props.getProperty(Property.DEBUG_PRINT_STACK_TRACE, props::asBoolean), logger.isDebugEnabled());
    }

    public static DebugConfig from(boolean toPrintStackTrace, boolean toMonitorMemory) {
        return new DebugConfig(toPrintStackTrace, toMonitorMemory);
    }

    private static final Logger logger = LoggerFactory.getLogger(DebugConfig.class);

    private final boolean toPrintStackTrace;
    private final boolean toMonitorMemory;

    DebugConfig(boolean toPrintStackTrace, boolean toMonitorMemory) {
        this.toPrintStackTrace = toPrintStackTrace;
        this.toMonitorMemory = toMonitorMemory;
    }

    public boolean toPrintStackTrace() {
        return toPrintStackTrace;
    }

    public boolean toMonitorMemory() {
        return toMonitorMemory;
    }

    @Override
    public String toString() {
        return "DebugConfig{" + "toPrintStackTrace=" + toPrintStackTrace + ", toMonitorMemory=" + toMonitorMemory + '}';
    }
}
