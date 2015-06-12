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

import com.github.horrorho.liquiddonkey.util.Bytes;
import com.github.horrorho.liquiddonkey.printer.Level;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.util.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
public abstract class BackupSelector implements UnaryOperator<List<Backup>> {

    private static final Logger logger = LoggerFactory.getLogger(BackupSelector.class);

    /**
     * Returns a new instance.
     * <p>
     * The backup UDIDs will be fully or partially matched against the supplied UDIDs, case-insensitive. If the supplied
     * UDIDs list is empty, the user will be prompted for a selection.
     *
     * @param commandLineUdids the command line UDID/s, not null
     * @param printer the Printer, not null
     * @return a new instance, not null
     * @throws NullPointerException if the printer or commandLineUdids arguments are null
     */
    public static UnaryOperator<List<Backup>> newInstance(List<String> commandLineUdids, Printer printer) {
        return commandLineUdids.isEmpty()
                ? new User(printer)
                : new Udid(printer, commandLineUdids);
    }

    protected final Printer printer;

    public BackupSelector(Printer printer) {
        this.printer = Objects.requireNonNull(printer);
    }

    @Override
    public List<Backup> apply(List<Backup> availableBackups) {
        logger.trace("<< apply < available: {}", availableBackups);
        if (availableBackups.isEmpty()) {
            printer.println(Level.WARN, "No backups available.");
            return new ArrayList<>();
        }

        List<Backup> selected = doApply(availableBackups);
        String selectedStr = selected.isEmpty()
                ? "None"
                : selected.stream()
                .map(Backup::udid)
                .map(Bytes::hex)
                .map(String::toUpperCase)
                .collect(Collectors.joining(" "));

        printer.println(Level.V, "Selected backups: " + selectedStr);
        logger.trace(">> apply > selected: {}", selected);
        return selected;
    }

    protected abstract List<Backup> doApply(List<Backup> backups);

    static final class Udid extends BackupSelector {

        private final List<String> commandLineUdids;

        Udid(Printer printer, List<String> commandLineUdids) {
            super(printer);
            this.commandLineUdids = commandLineUdids.stream().map(String::toLowerCase).collect(Collectors.toList());
        }

        @Override
        protected List<Backup> doApply(List<Backup> availableBackups) {
            return availableBackups.stream().filter(this::matches).collect(Collectors.toList());
        }

        private boolean matches(Backup backup) {
            return commandLineUdids.stream().anyMatch(udid -> backup.udidString().contains(udid));
        }
    }

    static final class User extends BackupSelector {

        User(Printer printer) {
            super(printer);
        }

        @Override
        protected List<Backup> doApply(List<Backup> availableBackups) {
            return Selector.builder(availableBackups)
                    .header("Listed backups:\n")
                    .footer("Select backup/s to download (leave blank to select all, q to quit):")
                    .formatter(Backup::format)
                    .onLineIsEmpty(() -> availableBackups)
                    .onQuit(ArrayList::new)
                    .build()
                    .printOptions()
                    .selection();
        }
    }
}
