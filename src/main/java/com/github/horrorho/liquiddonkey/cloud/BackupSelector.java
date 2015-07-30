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
 * all copies or substantial portions from the Software.
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
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.github.horrorho.liquiddonkey.util.Selector;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backup selector.
 *
 * @author Ahseya
 * @param <T> backup container
 */
@ThreadSafe
public abstract class BackupSelector<T> implements UnaryOperator<List<T>> {

    /**
     * Returns a new instance.
     * <p>
     * The from UDIDs will be fully or partially matched against the supplied UDIDs, case-insensitive. If the supplied
     * UDIDs list is empty, the user will be prompted for a selection.
     *
     * @param <T> item type
     * @param commandLineUdids the command line UDID/s, not null
     * @param mbsBackup get mbsBackup from item function, not null
     * @param formatter ICloud.MBSBackup user display formatter, not null
     * @param out output, not null
     * @param in input, not null
     * @return a new instance, not null
     */
    public static <T> UnaryOperator<List<T>> from(
            Collection<String> commandLineUdids,
            Function<T, ICloud.MBSBackup> mbsBackup,
            Function<ICloud.MBSBackup, String> formatter,
            PrintStream out,
            InputStream in) {

        return commandLineUdids.isEmpty()
                ? new User(mbsBackup, out, in, formatter)
                : new Udid(mbsBackup, out, in, new ArrayList<>(commandLineUdids));
    }

    private static final Logger logger = LoggerFactory.getLogger(BackupSelector.class);

    protected final Function<T, ICloud.MBSBackup> mbsBackup;
    protected final PrintStream out;
    protected final InputStream in;

    public BackupSelector(Function<T, ICloud.MBSBackup> mbsBackup, PrintStream out, InputStream in) {
        this.mbsBackup = Objects.requireNonNull(mbsBackup);
        this.out = Objects.requireNonNull(out);
        this.in = Objects.requireNonNull(in);
    }

    @Override
    public List<T> apply(List<T> available) {
        logger.trace("<< apply < available: {}", udids(available));

        if (available.isEmpty()) {
            out.println("No backups available.");
            return new ArrayList<>();
        }

        List<T> selected = doApply(available);
        String selectedStr = selected.isEmpty()
                ? "None"
                : selected.stream()
                .map(mbsBackup::apply)
                .map(ICloud.MBSBackup::getBackupUDID)
                .map(Bytes::hex)
                .collect(Collectors.joining(" "));

        out.println("Selected backup/s: " + selectedStr);

        logger.trace(">> apply > selected: {}", udids(selected));
        return selected;
    }

    List<String> udids(List<T> backups) {
        return backups == null
                ? null
                : backups.stream()
                .map(mbsBackup::apply)
                .map(ICloud.MBSBackup::getBackupUDID)
                .map(Bytes::hex)
                .collect(Collectors.toList());
    }

    protected abstract List<T> doApply(List<T> backups);

    static final class Udid<T> extends BackupSelector<T> {

        private final List<String> commandLineUdids;

        Udid(
                Function<T, ICloud.MBSBackup> mbsBackup,
                PrintStream out,
                InputStream in,
                List<String> commandLineUdids) {

            super(mbsBackup, out, in);
            this.commandLineUdids = commandLineUdids.stream().map(String::toLowerCase).collect(Collectors.toList());
        }

        @Override
        protected List<T> doApply(List<T> backups) {
            return backups.stream()
                    .filter(this::matches)
                    .collect(Collectors.toList());
        }

        boolean matches(T t) {
            return commandLineUdids.stream()
                    .anyMatch(udid -> Bytes.hex(mbsBackup.apply(t).getBackupUDID()).toLowerCase(Locale.US).contains(udid));
        }
    }

    static final class User<T> extends BackupSelector<T> {

        private final Function<ICloud.MBSBackup, String> formatter;

        User(
                Function<T, ICloud.MBSBackup> mbsBackup,
                PrintStream out,
                InputStream in,
                Function<ICloud.MBSBackup, String> formatter) {

            super(mbsBackup, out, in);
            this.formatter = formatter;
        }

        @Override
        protected List doApply(List<T> backups) {
            return Selector.builder(backups)
                    .header("Listed backups:\n")
                    .footer("Select backup/s to download (leave blank to select all, q to quit):")
                    .formatter(backup -> mbsBackup.andThen(formatter).apply(backup))
                    .onLineIsEmpty(() -> backups)
                    .onQuit(ArrayList::new)
                    .input(in)
                    .output(out)
                    .build()
                    .printOptions()
                    .selection();
        }
    }
}
