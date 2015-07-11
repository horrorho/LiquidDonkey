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
package com.github.horrorho.liquiddonkey.util.pool;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * FifoArrayQueueTest.
 *
 * @author Ahseya
 */
public class FifoArrayQueueTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testAddRemove() {
        SimpleQueue<String> instance = new FifoArrayQueue<>(2);

        instance.add("one");
        assertThat(instance.remove(), is("one"));
        instance.add("one");
        instance.add("two");
        assertThat(instance.remove(), is("one"));
        assertThat(instance.remove(), is("two"));
        instance.add("one");
        instance.add("two");
        assertThat(instance.remove(), is("one"));
        instance.add("three");
        assertThat(instance.remove(), is("two"));
        assertThat(instance.remove(), is("three"));
    }

    @Test
    public void testRemoveException() {
        SimpleQueue<String> instance = new FifoArrayQueue<>(1);

        exception.expect(NoSuchElementException.class);
        instance.remove();
    }

    @Test
    public void testAddException() {
        SimpleQueue<String> instance = new FifoArrayQueue<>(2);

        instance.add("one");
        instance.add("two");
        instance.remove();
        instance.add("three");
        exception.expect(IllegalStateException.class);
        instance.add("full");
    }

    @Test
    public void testAddRemoveException() {
        SimpleQueue<String> instance = new FifoArrayQueue<>(1);

        instance.add("one");
        instance.remove();
        exception.expect(NoSuchElementException.class);
        instance.remove();
    }

    @Test
    public void testSize() {
        SimpleQueue<String> instance = new FifoArrayQueue<>(1);

        assertThat(instance.size(), is(0));
        instance.add("one");
        assertThat(instance.size(), is(1));
        instance.remove();
        assertThat(instance.size(), is(0));
        instance.add("one");
        assertThat(instance.size(), is(1));
    }

    @Test
    public void testIsFull() {
        SimpleQueue<String> instance = new FifoArrayQueue<>(2);

        assertThat(instance.isFull(), is(false));
        instance.add("one");
        assertThat(instance.isFull(), is(false));
        instance.add("two");
        assertThat(instance.isFull(), is(true));
        instance.remove();
        assertThat(instance.isFull(), is(false));
    }

    @Test
    public void testIsEmpty() {
        SimpleQueue<String> instance = new FifoArrayQueue<>(1);

        assertThat(instance.isEmpty(), is(true));
        instance.add("one");
        assertThat(instance.isEmpty(), is(false));
        instance.remove();
        assertThat(instance.isEmpty(), is(true));
        instance.add("one");
        assertThat(instance.isEmpty(), is(false));
    }

    @Test
    public void testAddAll() {
        SimpleQueue<String> instance = new FifoArrayQueue<>(3);

        Collection<String> collection = Arrays.asList("one", "two", "three"); 
        instance.addAll(collection);

        assertThat(instance.size(), is(3));
        assertThat(instance.isFull(), is(true));
        assertThat(instance.remove(), is("one"));
        assertThat(instance.remove(), is("two"));
        assertThat(instance.remove(), is("three"));
        assertThat(instance.isEmpty(), is(true));
    }

    @Test
    public void testContains() {
        SimpleQueue<String> instance = new FifoArrayQueue<>(3);

        assertThat(instance.contains("one"), is(false));
        instance.add("one");
        assertThat(instance.contains("one"), is(true));
        instance.add("two");
        assertThat(instance.contains("one"), is(true));
        assertThat(instance.remove(), is("one")); 
        assertThat(instance.contains("one"), is(false));
        assertThat(instance.remove(), is("two"));
        assertThat(instance.isEmpty(), is(true));
    }
}
