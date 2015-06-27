/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies from the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * values copies or substantial portions from the Software.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javafx.concurrent.Task;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debouncer.
 *
 * @author Ahseya
 */
public class Debouncer extends Task<Void> {

    private static final Logger logger = LoggerFactory.getLogger(Debouncer.class);

    public static Debouncer newInstance(long timeMs, Node... nodes) {
        Debouncer debouncer = new Debouncer(timeMs, nodes);
        debouncer.setOnScheduled(e -> debouncer.set(true));
        debouncer.setOnCancelled(e -> debouncer.set(false));
        debouncer.setOnFailed(e -> debouncer.set(false));
        debouncer.setOnSucceeded(e -> debouncer.set(false));
        return debouncer;
    }

    private final long timeMs;
    private final List<Node> nodes;

    Debouncer(long timeMs, Node[] nodes) {
        this.timeMs = timeMs;
        this.nodes = nodes == null
                ? new ArrayList<>()
                : Arrays.asList(nodes);
    }

    void set(boolean value) {
        nodes.stream().forEach(node -> node.setDisable(value));
    }

    @Override
    protected Void call() throws Exception {
        logger.trace("<< call()");
        TimeUnit.MILLISECONDS.sleep(timeMs);
        logger.trace(">> call()");
        return null;
    }
}
