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
package com.github.horrorho.liquiddonkey.util;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jcip.annotations.NotThreadSafe;

/**
 * User selector.
 *
 * @author Ahseya
 * @param <T> item type
 */
@NotThreadSafe
public class Selector<T> {

    /**
     * Returns a new Builder instance.
     *
     * @param <T> the item type
     * @param options the available options, not null and no null elements
     * @return a new Builder instance, not null
     */
    public static <T> Selector.Builder<T> builder(List<T> options) {
        return new Builder(options);
    }

    private final String prompt;
    private final String quit;
    private final String header;
    private final List<T> options;
    private final Function<T, String> formatter;
    private final String footer;
    private final Supplier<List<T>> onLineIsEmpty;
    private final Supplier<List<T>> onQuit;
    private final PrintStream output;

    Selector(
            String prompt,
            String quit,
            String header,
            List<T> options,
            Function<T, String> formatter,
            String footer,
            Supplier<List<T>> onLineIsEmpty,
            Supplier<List<T>> onQuit,
            PrintStream output) {

        this.prompt = prompt;
        this.quit = quit;
        this.header = header;
        this.options = Objects.requireNonNull(options);
        this.formatter = Objects.requireNonNull(formatter);
        this.footer = footer;
        this.onLineIsEmpty = onLineIsEmpty;
        this.onQuit = onQuit;
        this.output = Objects.requireNonNull(output);
    }

    /**
     * Prints options.
     *
     * @return this Selector, not null
     */
    public Selector printOptions() {
        if (header != null) {
            System.out.println(header);
        }
        for (int i = 0; i < options.size(); i++) {
            System.out.println(i + ":" + formatter.apply(options.get(i)));
        }
        if (footer != null) {
            System.out.println(footer);
        }
        return this;
    }

    /**
     *
     * @see Selector#selection(java.io.InputStream)
     *
     * @return the user's selection
     */
    public List<T> selection() {
        return selection(System.in);
    }

    /**
     * Prompts, acquires, validates and returns the user's selection/s with the ordering preserved.
     *
     * @param source, not null
     * @return the user's selection/s, not null
     */
    public List<T> selection(InputStream source) {
        Scanner console = new Scanner(source, StandardCharsets.UTF_8.name());
        while (true) {
            System.out.print(prompt);
            String line = console.nextLine();
            if (line == null || line.toLowerCase(Locale.getDefault()).equals(quit)) {
                return onQuit.get();
            }

            if (line.isEmpty()) {
                return onLineIsEmpty.get();
            }

            Set<Integer> numbers = parseLineToNumbers(line);
            if (numbers != null && validate(numbers, options)) {
                return numbers.stream()
                        .map(options::get)
                        .collect(Collectors.toList());
            }
        }
    }

    boolean validate(Set<Integer> selected, List<T> options) {
        for (Integer i : selected) {
            if (i < 0 || i >= options.size()) {
                System.out.println("Invalid selection: " + i);
                return false;
            }
        }
        return true;
    }

    Set<Integer> parseLineToNumbers(String line) {
        Scanner tokens = new Scanner(line).useDelimiter("[,\\s]+");
        // LinkedHashSet to preserve the input order and avoid duplicates
        Set<Integer> numbers = new LinkedHashSet<>();
        while (tokens.hasNext()) {
            if (tokens.hasNextInt()) {
                numbers.add(tokens.nextInt());
            } else {
                System.out.println("Bad number: " + tokens.next());
                return null;
            }
        }
        return numbers;
    }

    public static class Builder<T> {

        private final List<T> options;
        private String quit = "q";
        private String prompt = ": ";
        private String header = null;
        private Function<T, String> formatter = Object::toString;
        private String footer = null;
        private Supplier<List<T>> onLineIsEmpty = ArrayList::new;
        private Supplier<List<T>> onQuit = ArrayList::new;
        private PrintStream output = System.out;

        /**
         * Selector Builder.
         *
         * @param options the available options, not null and no null elements
         */
        public Builder(List<T> options) {
            this.options = options;
        }

        public Builder<T> prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder<T> quit(String quit) {
            this.quit = quit;
            return this;
        }

        public Builder<T> header(String header) {
            this.header = header;
            return this;
        }

        public Builder<T> formatter(Function<T, String> formatter) {
            this.formatter = formatter;
            return this;
        }

        public Builder<T> footer(String footer) {
            this.footer = footer;
            return this;
        }

        public Builder<T> onLineIsEmpty(Supplier<List<T>> onLineIsEmpty) {
            this.onLineIsEmpty = onLineIsEmpty;
            return this;
        }

        public Builder<T> onQuit(Supplier<List<T>> onQuit) {
            this.onQuit = onQuit;
            return this;
        }

        public Builder<T> output(PrintStream output) {
            this.output = output;
            return this;
        }

        public Selector<T> build() {
            return new Selector<>(prompt, quit, header, options, formatter, footer, onLineIsEmpty, onQuit, output);
        }
    }
}
