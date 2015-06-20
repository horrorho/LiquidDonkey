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
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Selection configuration.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class SelectionConfig {

    public static SelectionConfig newInstance(Configuration config) {

        return newInstance(
                config.getList(Property.SELECTION_UDID, config::asHex),
                config.getList(Property.SELECTION_SNAPSHOT, config::asInteger));
    }

    public static SelectionConfig newInstance(List<String> backups, List<Integer> snapshots) {
        return new SelectionConfig(backups, snapshots);
    }

    private final List<String> backups;
    private final List<Integer> snapshots;

    SelectionConfig(List<String> backups, List<Integer> snapshots) {
        this.backups = Objects.requireNonNull(backups);
        this.snapshots = Objects.requireNonNull(snapshots);
    }

    public List<String> backups() {
        return backups;
    }

    public List<Integer> snapshots() {
        return snapshots;
    }

    @Override
    public String toString() {
        return "SelectionConfig{" + "backups=" + backups + ", snapshots=" + snapshots + '}';
    }
}
