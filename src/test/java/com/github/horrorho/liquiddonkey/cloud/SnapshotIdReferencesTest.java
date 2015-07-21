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
package com.github.horrorho.liquiddonkey.cloud;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import static junitparams.JUnitParamsRunner.$;
import junitparams.Parameters;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SnapshotIdReferencesTest.
 *
 * @author Ahseya
 */
@RunWith(JUnitParamsRunner.class)
public class SnapshotIdReferencesTest {

    @Test
    @Parameters
    public void testReferences(ICloud.MBSBackup backup, Map<Integer, Integer> expected) {
        Map<Integer, Integer> references = SnapshotIdReferences.references(backup);
        assertThat(references, is(expected));
    }

    public static Object[] parametersForTestReferences() {
        return new Object[]{
            $(b(), map()),
            $(b(s(1)), map(p(0, 1), p(1, 1), p(-1, 1))),
            $(b(s(1), s(2)), map(p(0, 1), p(1, 1), p(2, 2), p(-1, 2), p(-2, 1))),
            $(b(s(1), s(9), s(10)), map(p(0, 1), p(1, 1), p(9, 9), p(10, 10), p(-1, 10), p(-2, 9), p(-3, 1))),
            $(b(s(5), s(10), s(11), i(12)), map(p(0, 5), p(5, 5), p(10, 10), p(11, 11), p(-1, 11), p(-2, 10), p(-3, 5)))
        };
    }

    @Test
    @Parameters
    public void testApplyAsInt(ICloud.MBSBackup backup, int id, int expected) {
        SnapshotIdReferences references = SnapshotIdReferences.from(backup);
        assertThat(references.applyAsInt(id), is(expected));
    }

    public static Object[] parametersForTestApplyAsInt() {
        return new Object[]{
            $(b(), 0, -1),
            $(b(), 1, -1),
            $(b(), -1, -1),
            $(b(i(1)), 0, -1),
            $(b(i(1)), 1, -1),
            $(b(i(1)), -1, -1),
            $(b(s(1)), 0, 1),
            $(b(s(1)), 1, 1),
            $(b(s(1)), 2, -1),
            $(b(s(1)), -1, 1),
            $(b(s(1)), -2, -1),
            $(b(s(5), s(10), s(11), i(12)), 0, 5),
            $(b(s(5), s(10), s(11), i(12)), 1, -1),
            $(b(s(5), s(10), s(11), i(12)), 5, 5),
            $(b(s(5), s(10), s(11), i(12)), 10, 10),
            $(b(s(5), s(10), s(11), i(12)), 11, 11),
            $(b(s(5), s(10), s(11), i(12)), 12, -1),
            $(b(s(5), s(10), s(11), i(12)), -1, 11),
            $(b(s(5), s(10), s(11), i(12)), -2, 10),
            $(b(s(5), s(10), s(11), i(12)), -3, 5),
            $(b(s(5), s(10), s(11), i(12)), -4, -1)
        };
    }

    public static ICloud.MBSBackup b(ICloud.MBSSnapshot... snapshots) {
        ICloud.MBSBackup.Builder builder = ICloud.MBSBackup.newBuilder();
        Stream.of(snapshots).forEach(builder::addSnapshot);
        return builder.build();
    }

    // Complete
    public static ICloud.MBSSnapshot s(int id) {
        return ICloud.MBSSnapshot.newBuilder().setSnapshotID(id).setCommitted(1).build();
    }

    // Incomplete
    public static ICloud.MBSSnapshot i(int id) {
        return ICloud.MBSSnapshot.newBuilder().setSnapshotID(id).build();
    }

    public static Map.Entry<Integer, Integer> p(int k, int v) {
        return new AbstractMap.SimpleImmutableEntry<>(k, v);
    }

    public static Map<Integer, Integer> map(Map.Entry<Integer, Integer>... pairs) {
        return Stream.of(pairs).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
