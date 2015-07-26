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

import java.util.Properties; 
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Config.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Config {

    public static Config newInstance(Properties properties) {
        return newInstance(
                AuthenticationConfig.from(properties),
                ClientConfig.newInstance(properties),
                EngineConfig.newInstance(properties),
                FileConfig.newInstance(properties),
                FileFilterConfig.newInstance(properties),
                HttpConfig.newInstance(properties),
                PrintConfig.newInstance(properties),
                SelectionConfig.newInstance(properties));
    }

    public static Config newInstance(
            AuthenticationConfig authentication,
            ClientConfig client,
            EngineConfig engine,
            FileConfig file,
            FileFilterConfig fileFilter,
            HttpConfig http,
            PrintConfig printer,
            SelectionConfig selection) {

        return new Config(authentication,
                client,
                engine,
                file,
                fileFilter,
                http,
                printer,
                selection);
    }

    private final AuthenticationConfig authentication;
    private final ClientConfig client;
    private final EngineConfig engine;
    private final FileConfig file;
    private final FileFilterConfig fileFilter;
    private final HttpConfig http;
    private final PrintConfig printer;
    private final SelectionConfig selection;

    Config(
            AuthenticationConfig authentication,
            ClientConfig client,
            EngineConfig engine,
            FileConfig file,
            FileFilterConfig fileFilter,
            HttpConfig http,
            PrintConfig printer,
            SelectionConfig selection) {

        this.authentication = authentication;
        this.client = client;
        this.engine = engine;
        this.file = file;
        this.fileFilter = fileFilter;
        this.http = http;
        this.printer = printer;
        this.selection = selection;
    }

    public AuthenticationConfig authentication() {
        return authentication;
    }

    public ClientConfig client() {
        return client;
    }

    public EngineConfig engine() {
        return engine;
    }

    public FileConfig file() {
        return file;
    }

    public FileFilterConfig fileFilter() {
        return fileFilter;
    }

    public HttpConfig http() {
        return http;
    }

    public PrintConfig printer() {
        return printer;
    }

    public SelectionConfig selection() {
        return selection;
    }

    @Override
    public String toString() {
        return "Config{"
                + "authentication=" + authentication
                + ", client=" + client
                + ", engine=" + engine
                + ", directory=" + file
                + ", fileFilter=" + fileFilter
                + ", http=" + http
                + ", printer=" + printer
                + ", selection=" + selection
                + '}';
    }
}
