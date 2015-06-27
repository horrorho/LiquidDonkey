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
package com.github.horrorho.liquiddonkey.gui.controller;

import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.HttpFactory;
import com.github.horrorho.liquiddonkey.printer.Printer;
import static com.github.horrorho.liquiddonkey.settings.Markers.GUI;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.concurrent.Task;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpTaskFactory.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class HttpTaskFactory<T> {

    public static <T> HttpTaskFactory<T> from(HttpFactory httpFactory, Printer printer) {
        return new HttpTaskFactory<>(httpFactory, printer);
    }

    private static final Logger logger = LoggerFactory.getLogger(HttpTaskFactory.class);

    private final HttpFactory httpFactory;
    private final Printer printer;

    HttpTaskFactory(HttpFactory httpFactory, Printer printer) {
        this.httpFactory = Objects.requireNonNull(httpFactory);
        this.printer = Objects.requireNonNull(printer);
    }

    public Task<T> newInstance(Function<Http, T> method, Consumer<Task<T>> post) {
        logger.trace(GUI, "<< newInstance()");
        final Http http = httpFactory.newInstance(printer);

        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                logger.trace(GUI, "<< call()");
                T t = method.apply(http);
                logger.trace(GUI, ">> call() > {}", t);
                return t;
            }
        };

        task.setOnCancelled(e -> {
            logger.trace(GUI, "<< onCancelled()");
            post.accept(task);
            close(http);
            logger.trace(GUI, ">> onCancelled()");
        });

        task.setOnSucceeded(e -> {
            logger.trace(GUI, "<< onSucceeded()");
            post.accept(task);
            close(http);
            logger.trace(GUI, ">> onSucceeded()");
        });

        task.setOnFailed(e -> {
            logger.trace(GUI, "<< onFailed()");
            post.accept(task);
            close(http);
            logger.trace(GUI, ">> onFailed()");
        });

        logger.trace(GUI, ">> newInstance()");
        return task;
    }

    void close(Http http) {
        try {
            http.close();
        } catch (IOException ex) {
            logger.warn(GUI, "-- close() > exception: ", ex);
        }
    }
}
