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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Ahseya
 */
@RunWith(JUnitParamsRunner.class)
public class ConfigFactoryTest {

    private final ConfigFactory factory = ConfigFactory.getInstance();

    @Test
    @Parameters
    public <T> void testFrom(String in, Function<Config, T> function, T expected) {
        String[] args = in.split("\\s");
        Config config = factory.from(args);
        T value = function.apply(config);
        assertThat(value, is(expected));
    }

    public static Object[] parametersForTestFrom() {
        return new Object[]{
            o("user password", config -> config.authentication().id(), "user"),
            o("user password", config -> config.authentication().password(), "password"),
            o("u p --output test/folder", config -> config.directory().base(), Paths.get("test/folder").toAbsolutePath()),
            o("u p --udid", config -> config.selection().udids(), set("")),
            o("u p --udid 1FfF", config -> config.selection().udids(), set("1FfF")),
            o("u p --udid 1fff 2FFF", config -> config.selection().udids(), set("1fff", "2FFF")),
            o("u p --udid 1fff 2FFF 2FFF", config -> config.selection().udids(), set("1fff", "2FFF")),
            o("u p --snapshot 1", config -> config.selection().snapshots(), set(1)),
            o("u p --snapshot 1 2", config -> config.selection().snapshots(), set(1, 2)),
            o("u p --snapshot 1 2 2", config -> config.selection().snapshots(), set(1, 2)),
            o("u p --snapshot -1", config -> config.selection().snapshots(), set(-1)),
            o("u p", config -> config.fileFilter().relativePathContains(), set("")),
            o("u p --relative-path first", config -> config.fileFilter().relativePathContains(), set("first")),
            o("u p --relative-path first second", config -> config.fileFilter().relativePathContains(), set("first", "second")),
            o("u p --relative-path first second second", config -> config.fileFilter().relativePathContains(), set("first", "second")),
            o("u p --item-types PHOTOS", config -> config.fileFilter().relativePathContains(), set(Property.ITEM_TYPE_PHOTOS)),
            o("u p --item-types photos", config -> config.fileFilter().relativePathContains(), set(Property.ITEM_TYPE_PHOTOS)),
            o("u p --item-types photos MOVIES", config -> config.fileFilter().relativePathContains(), set(Property.ITEM_TYPE_PHOTOS, Property.ITEM_TYPE_MOVIES)),
            o("u p --item-types photos MOVIES Movies", config -> config.fileFilter().relativePathContains(), set(Property.ITEM_TYPE_PHOTOS, Property.ITEM_TYPE_MOVIES)),
            o("u p", config -> config.fileFilter().extensions(), set("")),
            o("u p --extension Abc", config -> config.fileFilter().extensions(), set(".Abc")),
            o("u p --extension .Abc", config -> config.fileFilter().extensions(), set(".Abc")),
            o("u p --extension .Abc efg", config -> config.fileFilter().extensions(), set(".Abc", ".efg")),
            o("u p --extension .Abc efg efg", config -> config.fileFilter().extensions(), set(".Abc", ".efg")),
            o("u p", config -> config.fileFilter().domainContains(), set("")),
            o("u p --domain first", config -> config.fileFilter().domainContains(), set("first")),
            o("u p --domain first Second", config -> config.fileFilter().domainContains(), set("first", "Second")),
            o("u p --min-date 0000-01-01", config -> config.fileFilter().minDate(), -62167219125L),
            o("u p --max-date 9999-01-01", config -> config.fileFilter().maxDate(), 253370764800L),
            o("u p --min-size 0", config -> config.fileFilter().minSize(), 0L),
            o("u p --min-size 64", config -> config.fileFilter().minSize(), 65536L),
            o("u p --max-size 0", config -> config.fileFilter().maxSize(), 0L),
            o("u p --max-size 64", config -> config.fileFilter().maxSize(), 65536L)
        };
    }

    public static <T> Object[] o(String in, Function<Config, T> function, T expected) {
        return new Object[]{in, function, expected};
    }

    public static Set<String> set(Property... properties) {
        return set(
                Stream.of(properties)
                .map(property -> property.getDefaultValue().split("\\s"))
                .collect(Collectors.toList()));
    }

    public static <T> Set<T> set(T... t) {
        return Stream.of(t).collect(Collectors.toSet());
    }

    public static Set<String> set(List<String[]> strings) {
        return strings.stream().map(Arrays::asList).flatMap(List::stream).collect(Collectors.toSet());
    }

}
