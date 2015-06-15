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

import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.FatalException;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.github.horrorho.liquiddonkey.printer.Level;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Account.
 *
 * @author Ahseya
 */
public final class Account {

    private static final Logger logger = LoggerFactory.getLogger(Account.class);

    /**
     * Returns a new instance.
     * <p>
     * Thread safe if the supplied Client is thread safe.
     *
     * @param client not null
     * @param printer not null
     * @return a new instance, not null
     * @throws FatalException if an IOException occurs
     */
    public static Account newInstance(Client client, Printer printer) {
        try {
            return newInstance(client, printer, client.account());
        } catch (IOException ex) {
            throw new FatalException("Account.", ex);
        }
    }

    static Account newInstance(Client client, Printer printer, ICloud.MBSAccount mbsAccount) {
        return new Account(client, printer, mbsAccount);
    }

    private final ICloud.MBSAccount mbsAccount;
    private final Client client;
    private final Printer printer;

    Account(Client client, Printer printer, ICloud.MBSAccount mbsAccount) {
        this.client = Objects.requireNonNull(client);
        this.printer = Objects.requireNonNull(printer);
        this.mbsAccount = Objects.requireNonNull(mbsAccount);
    }

    /**
     * Returns a list of backups.
     * <p>
     * Queries the {@link Client} on each invocation, results of previous calls are not cached.
     *
     * @return a list of backups, not null
     */
    public List<Backup> backups() {
        return mbsAccount.getBackupUDIDList().stream()
                .map(this::backup)
                .filter(Objects::nonNull)
                .map(Backup::newInstance)
                .collect(Collectors.toList());
    }

    ICloud.MBSBackup backup(ByteString udid) {
        try {
            ICloud.MBSBackup backup = client.backup(udid);
            logger.debug("<< backup() < mbsBackup: {}", backup);
            return backup;
        } catch (IOException ex) {
            logger.warn("-- backup() > ", ex);
            printer.println(Level.WARN, "Unable to retrieve backup information: " + Bytes.hex(udid), ex);
            return null;
        }
    }
}
