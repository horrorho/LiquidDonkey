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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.settings.config.FileFilterConfig;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * File filter.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class FileFilter
        implements Predicate<ICloud.MBSFile>, Function<Collection<ICloud.MBSFile>, Set<ICloud.MBSFile>> {

    public static FileFilter from(FileFilterConfig config) {

        return new FileFilter(
                config.domainContains(),
                config.relativePathContains(),
                config.extensions(),
                config.maxDate(),
                config.minDate(),
                config.maxSize(),
                config.minSize());
    }

    private final Set<String> domainContains;
    private final Set<String> relativePathContains;
    private final Set<String> extensions;
    private final long maxDate;
    private final long minDate;
    private final long maxSize;
    private final long minSize;

    private FileFilter(
            Collection<String> domainContains,
            Collection<String> relativePathContains,
            Collection<String> extensions,
            long maxDate,
            long minDate,
            long maxSize,
            long minSize) {

        this.domainContains = toLowerCase(domainContains);
        this.relativePathContains = toLowerCase(relativePathContains);
        this.extensions = toLowerCase(extensions);
        this.maxDate = maxDate;
        this.minDate = minDate;
        this.maxSize = maxSize;
        this.minSize = minSize;
    }

    Set<String> toLowerCase(Collection<String> collection) {
        return collection.stream().map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toSet());
    }

    @Override
    public Set<ICloud.MBSFile> apply(Collection<ICloud.MBSFile> files) {
        return files.stream().filter(this::test).collect(Collectors.toSet());
    }

    @Override
    public boolean test(ICloud.MBSFile file) {
        return testDomain(file.getDomain())
                && testSize(file)
                && testExtensions(file.getRelativePath())
                && testRelativePath(file.getRelativePath())
                && testTimestamp(file);
    }

    public boolean testTimestamp(ICloud.MBSFile file) {
        if (!file.hasAttributes()) {
            return true;
        }
        ICloud.MBSFileAttributes attributes = file.getAttributes();
        if (!attributes.hasLastModified()) {
            return true;
        }
        long timestamp = attributes.getLastModified();
        return timestamp >= minDate && timestamp <= maxDate;
    }

    public boolean testSize(ICloud.MBSFile file) {
        long size = file.getSize();
        return size >= minSize && size <= maxSize;
    }

    public boolean testDomain(String path) {
        return testList(String::contains, path.toLowerCase(Locale.getDefault()), domainContains);
    }

    public boolean testRelativePath(String path) {
        return testList(String::contains, path.toLowerCase(Locale.getDefault()), relativePathContains);
    }

    public boolean testExtensions(String path) {
        return testList(String::endsWith, path.toLowerCase(Locale.getDefault()), extensions);
    }

    boolean testList(BiPredicate<String, String> method, String string, Collection<String> list) {
        return list.isEmpty()
                ? true
                : list.stream().anyMatch(listItem -> method.test(string, listItem));
    }

    @Override
    public String toString() {
        return "FileFilter{"
                + "domainContains=" + domainContains
                + ", relativePathContains=" + relativePathContains
                + ", extensions=" + extensions
                + ", maxDate=" + maxDate
                + ", minDate=" + minDate + ", maxSize=" + maxSize
                + ", minSize=" + minSize
                + '}';
    }
}
