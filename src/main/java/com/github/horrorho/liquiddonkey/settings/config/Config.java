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

import com.github.horrorho.liquiddonkey.settings.Property;
import com.github.horrorho.liquiddonkey.settings.props.Props;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * PropertyAssistant.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Config {

    public static Config newInstance(Props<Property> props) {
        return newInstance(
                AuthenticationConfig.newInstance(props),
                ClientConfig.newInstance(props),
                DirectoryConfig.newInstance(props),
                DonkeyFactoryConfig.newInstance(props),
                FileFilterConfig.newInstance(props),
                HttpConfig.newInstance(props),
                PrintConfig.newInstance(props),
                SelectionConfig.newInstance(props),
                SnapshotDownloaderConfig.newInstance(props),
                SnapshotFactoryConfig.newInstance(props));
    }

    public static Config newInstance(
            AuthenticationConfig authentication,
            ClientConfig clientConfig,
            DirectoryConfig directory,
            DonkeyFactoryConfig donkeyFactory,
            FileFilterConfig fileFilter,
            HttpConfig http,
            PrintConfig printer,
            SelectionConfig selection,
            SnapshotDownloaderConfig snapshotDownloader,
            SnapshotFactoryConfig snapshotFactory) {

        return new Config(
                authentication,
                clientConfig,
                directory,
                donkeyFactory,
                fileFilter,
                http,
                printer,
                selection,
                snapshotDownloader,
                snapshotFactory);
    }

    private final AuthenticationConfig authentication;
    private final ClientConfig clientConfig;
    private final DirectoryConfig directory;
    private final DonkeyFactoryConfig donkeyFactory;
    private final FileFilterConfig fileFilter;
    private final HttpConfig http;
    private final PrintConfig printer;
    private final SelectionConfig selection;
    private final SnapshotDownloaderConfig snapshotDownloader;
    private final SnapshotFactoryConfig snapshotFactory;

    private Config(AuthenticationConfig authentication,
            ClientConfig clientConfig,
            DirectoryConfig directory,
            DonkeyFactoryConfig donkeyFactory,
            FileFilterConfig fileFilter,
            HttpConfig http,
            PrintConfig printer,
            SelectionConfig selection,
            SnapshotDownloaderConfig snapshotDownloader,
            SnapshotFactoryConfig snapshotFactory) {

        this.authentication = authentication;
        this.clientConfig = clientConfig;
        this.directory = directory;
        this.donkeyFactory = donkeyFactory;
        this.fileFilter = fileFilter;
        this.http = http;
        this.printer = printer;
        this.selection = selection;
        this.snapshotDownloader = snapshotDownloader;
        this.snapshotFactory = snapshotFactory;
    }

    public AuthenticationConfig authentication() {
        return authentication;
    }

    public ClientConfig clientConfig() {
        return clientConfig;
    }

    public DirectoryConfig directory() {
        return directory;
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

    public SnapshotDownloaderConfig snapshotDownloader() {
        return snapshotDownloader;
    }

    public SnapshotFactoryConfig snapshotFactory() {
        return snapshotFactory;
    }

    @Override
    public String toString() {
        return "Config{" + "authentication=" + authentication + ", clientConfig=" + clientConfig
                + ", directory=" + directory + ", donkeyFactory=" + donkeyFactory + ", fileFilter=" + fileFilter
                + ", http=" + http + ", printer=" + printer + ", selection=" + selection + ", donkeyExecutor="
                + snapshotDownloader + ", backupDownloaderFactory=" + snapshotFactory + '}';
    }
}
