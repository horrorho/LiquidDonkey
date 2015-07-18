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
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import junitparams.JUnitParamsRunner;
import static junitparams.JUnitParamsRunner.$;
import junitparams.Parameters;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SnapshotDirectoryTest.
 *
 * @author Ahseya
 */
@RunWith(JUnitParamsRunner.class)
public class SnapshotDirectoryTest {

    final static ICloud.MBSFile one = MBSFile.newBuilder().setDomain("domain").setRelativePath("one.txt").build();
    final static ICloud.MBSFile two = MBSFile.newBuilder().setDomain("domain").setRelativePath("one/two.db").build();

    @Test
    @Parameters
    public void testNewInstance(
            String base,
            String udid,
            String snapshotId,
            ICloud.MBSFile file,
            boolean isFlat,
            boolean isCombined,
            String expected) {

        Path baseBath = Paths.get(base);
        Path expectedPath = Paths.get(expected);

        SnapshotDirectory snapshotDirectory
                = SnapshotDirectory.from(baseBath, udid, snapshotId, isFlat, isCombined);

        Path result = snapshotDirectory.apply(file);
        assertThat(result, is(expectedPath));

    }

    public static Object[] parametersForTestNewInstance() {
        return new Object[]{
            $("base", "udid", "1", one, false, false, "base/udid/1/domain/one.txt"),
            $("base", "udid", "2", two, false, false, "base/udid/2/domain/one/two.db"),
            $("base", "udid", "3", one, false, true, "base/udid/domain/one.txt"),
            $("base", "udid", "4", two, false, true, "base/udid/domain/one/two.db"),
            $("base", "udid", "1", one, true, false, "base/udid/1/8b28fc3a3e71ee69369a06822d400babc5c850b4"),
            $("base", "udid", "2", two, true, false, "base/udid/2/5119a6cfa5210717a5597cb0f7298aac437f5ad0"),
            $("base", "udid", "3", one, true, true, "base/udid/8b28fc3a3e71ee69369a06822d400babc5c850b4"),
            $("base", "udid", "4", two, true, true, "base/udid/5119a6cfa5210717a5597cb0f7298aac437f5ad0")
        };
    }
}
