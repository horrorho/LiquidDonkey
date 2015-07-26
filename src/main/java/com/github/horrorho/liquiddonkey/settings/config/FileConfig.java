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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Directory configuration.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class FileConfig {

    public static FileConfig from(Properties properties) { 
        Props<Property> props = Props.from(properties);
        
        return from(
                Paths.get(props.getProperty(Property.FILE_OUTPUT_DIRECTORY)).toAbsolutePath(),
                props.getProperty(Property.FILE_COMBINED, props::asBoolean),
                props.getProperty(Property.FILE_FLAT, props::asBoolean),
                props.getProperty(Property.ENGINE_SET_LAST_MODIFIED_TIMESTAMP, props::asBoolean));
    }

    public static FileConfig from(
            Path base,
            boolean isCombined,
            boolean isFlat,
            boolean setLastModifiedTimestamp) {

        return new FileConfig(base,
                isCombined,
                isFlat,
                setLastModifiedTimestamp);
    }

    private final Path base;
    private final boolean isCombined;
    private final boolean isFlat;
    private final boolean setLastModifiedTimestamp;

    FileConfig(
            Path base,
            boolean isCombined,
            boolean isFlat,
            boolean setLastModifiedTimestamp) {

        this.base = base;
        this.isCombined = isCombined;
        this.isFlat = isFlat;
        this.setLastModifiedTimestamp = setLastModifiedTimestamp;
    }

    public Path base() {
        return base;
    }

    public boolean isCombined() {
        return isCombined;
    }

    public boolean isFlat() {
        return isFlat;
    }

    public boolean setLastModifiedTimestamp() {
        return setLastModifiedTimestamp;
    }

    @Override
    public String toString() {
        return "FileConfig{"
                + "base=" + base
                + ", isCombined=" + isCombined
                + ", isFlat=" + isFlat
                + ", setLastModifiedTimestamp=" + setLastModifiedTimestamp
                + '}';
    }
}
