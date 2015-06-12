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

import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Configuration.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Config {

    public static Config newInstance(CommandLineHelper line) {
        return newInstance(
                AuthenticationConfig.newInstance(line),
                BackupDownloaderFactoryConfig.newInstance(line),
                DirectoryConfig.newInstance(line),
                DonkeyExecutorConfig.newInstance(line),
                DonkeyFactoryConfig.newInstance(line),
                FileFilterConfig.newInstance(line),
                HttpConfig.newInstance(line),
                PrintConfig.newInstance(line),
                SelectionConfig.newInstance(line));
    }

    public static Config newInstance(
            AuthenticationConfig authentication,
            BackupDownloaderFactoryConfig backupDownloaderFactory,
            DirectoryConfig directory,
            DonkeyExecutorConfig donkeyExecutor,
            DonkeyFactoryConfig donkeyFactory,
            FileFilterConfig filterFilter,
            HttpConfig http,
            PrintConfig printer,
            SelectionConfig selection) {

        return new Config(authentication,
                backupDownloaderFactory,
                directory,
                donkeyExecutor,
                donkeyFactory,
                filterFilter,
                http,
                printer,
                selection);
    }

    private final AuthenticationConfig authentication;
    private final BackupDownloaderFactoryConfig backupDownloaderFactory;
    private final DirectoryConfig directory;
    private final DonkeyExecutorConfig donkeyExecutor;
    private final DonkeyFactoryConfig donkeyFactory;
    private final FileFilterConfig fileFilter;
    private final HttpConfig http;
    private final PrintConfig printer;
    private final SelectionConfig selection;

    private Config(
            AuthenticationConfig authentication,
            BackupDownloaderFactoryConfig backupDownloaderFactory,
            DirectoryConfig directory,
            DonkeyExecutorConfig donkeyExecutor,
            DonkeyFactoryConfig donkeyFactory,
            FileFilterConfig filterFilter,
            HttpConfig http,
            PrintConfig printer,
            SelectionConfig selection) {

        this.authentication = Objects.requireNonNull(authentication);
        this.backupDownloaderFactory = Objects.requireNonNull(backupDownloaderFactory);
        this.directory = Objects.requireNonNull(directory);
        this.donkeyExecutor = Objects.requireNonNull(donkeyExecutor);
        this.donkeyFactory = Objects.requireNonNull(donkeyFactory);
        this.fileFilter = Objects.requireNonNull(filterFilter);
        this.http = Objects.requireNonNull(http);
        this.printer = Objects.requireNonNull(printer);
        this.selection = Objects.requireNonNull(selection);
    }

    public AuthenticationConfig authentication() {
        return authentication;
    }

    public BackupDownloaderFactoryConfig backupDownloaderFactory() {
        return backupDownloaderFactory;
    }

    public DirectoryConfig directory() {
        return directory;
    }

    public DonkeyExecutorConfig donkeyExecutor() {
        return donkeyExecutor;
    }

    public DonkeyFactoryConfig donkeyFactory() {
        return donkeyFactory;
    }

    public FileFilterConfig fileFilter() {
        return fileFilter;
    }

    public HttpConfig http() {
        return http;
    }

    public PrintConfig print() {
        return printer;
    }

    public SelectionConfig selection() {
        return selection;
    }

    @Override
    public String toString() {
        return "Config{" + "authentication=" + authentication + ", backupDownloaderFactory=" + backupDownloaderFactory
                + ", directory=" + directory + ", donkeyExecutor=" + donkeyExecutor + ", donkeyFactory=" + donkeyFactory
                + ", filterFilter=" + fileFilter + ", http=" + http + ", printer=" + printer + ", selection="
                + selection + '}';
    }
}
