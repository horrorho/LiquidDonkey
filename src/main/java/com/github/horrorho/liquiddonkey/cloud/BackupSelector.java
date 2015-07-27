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
 */
@ThreadSafe
public abstract class BackupSelector implements UnaryOperator<List<ICloud.MBSBackup>> {

    /**
     * Returns a new instance.
     * <p>
     * The from UDIDs will be fully or partially matched against the supplied UDIDs, case-insensitive. If the supplied
     * UDIDs list is empty, the user will be prompted for a selection.
     *
     * @param commandLineUdids the command line UDID/s, not null
     * @param formatter ICloud.MBSBackup user display formatter, not null
     * @param std the std PrintStream, not null
     * @return a new instance, not null
     */
    public static UnaryOperator<List<ICloud.MBSBackup>> from(
            Collection<String> commandLineUdids,
            Function<ICloud.MBSBackup, String> formatter,
            PrintStream std) {

        return commandLineUdids.isEmpty()
                ? new User(std, formatter)
                : new Udid(std, new ArrayList<>(commandLineUdids));
    }

    private static final Logger logger = LoggerFactory.getLogger(BackupSelector.class);

    protected final PrintStream std;

    BackupSelector(PrintStream std) {
        this.std = Objects.requireNonNull(std);
    }

    @Override
    public List<ICloud.MBSBackup> apply(List<ICloud.MBSBackup> available) {
        logger.trace("<< apply < available: {}", udids(available));

        if (available.isEmpty()) {
            std.println("No backups available.");
            return new ArrayList<>();
        }

        List<ICloud.MBSBackup> selected = doApply(available);
        String selectedStr = selected.isEmpty()
                ? "None"
                : selected.stream()
                .map(ICloud.MBSBackup::getBackupUDID)
                .map(Bytes::hex)
                .collect(Collectors.joining(" "));

        std.println("Selected backup/s: " + selectedStr);

        logger.trace(">> apply > selected: {}", udids(selected));
        return selected;
    }

    List<String> udids(List<ICloud.MBSBackup> backups) {
        return backups == null
                ? null
                : backups.stream().map(ICloud.MBSBackup::getBackupUDID).map(Bytes::hex).collect(Collectors.toList());
    }

    protected abstract List<ICloud.MBSBackup> doApply(List<ICloud.MBSBackup> backups);

    static final class Udid extends BackupSelector {

        private final List<String> commandLineUdids;

        Udid(PrintStream std, List<String> commandLineUdids) {
            super(std);
            this.commandLineUdids = commandLineUdids.stream().map(String::toLowerCase).collect(Collectors.toList());
        }

        @Override
        protected List<ICloud.MBSBackup> doApply(List<ICloud.MBSBackup> availableBackups) {
            return availableBackups.stream().filter(this::matches).collect(Collectors.toList());
        }

        boolean matches(ICloud.MBSBackup backup) {
            return commandLineUdids.stream()
                    .anyMatch(udid -> Bytes.hex(backup.getBackupUDID()).toLowerCase(Locale.US).contains(udid));
        }
    }

    static final class User extends BackupSelector {

        private final Function<ICloud.MBSBackup, String> formatter;

        User(PrintStream std, Function<ICloud.MBSBackup, String> formatter) {
            super(std);
            this.formatter = formatter;
        }

        @Override
        protected List<ICloud.MBSBackup> doApply(List<ICloud.MBSBackup> availableBackups) {
            return Selector.builder(availableBackups)
                    .header("Listed backups:\n")
                    .footer("Select backup/s to download (leave blank to select all, q to quit):")
                    .formatter(backup -> formatter.apply(backup))
                    .onLineIsEmpty(() -> availableBackups)
                    .onQuit(ArrayList::new)
                    .build()
                    .printOptions(std)
                    .selection();   // TODO inject in out
        }
    }
}
