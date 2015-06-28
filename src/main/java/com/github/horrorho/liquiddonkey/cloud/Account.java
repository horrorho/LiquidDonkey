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
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.printer.Printer;
import java.io.IOException;
import java.util.ArrayList;
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
    /**
     * Returns a new instance.
     *
     * @param http not null
     * @param client not null
     * @param printer not null
     * @return a new instance, not null
     * @throws FatalException
     */
    public static Account newInstance(Http http, Client client, Printer printer) {
        try {
            ICloud.MBSAccount account = client.account(http);

            List<Backup> backups = account.getBackupUDIDList().stream()
                    .map(udid -> Backup.newInstance(http, client, udid, printer))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return newInstance(account, backups);
        } catch (IOException ex) {
            throw new FatalException("Account.", ex);
        }
    }

    public static Account newInstance(ICloud.MBSAccount mbsAccount, List<Backup> backups) {
        return new Account(mbsAccount, backups);
    }

    private final ICloud.MBSAccount mbsAccount;
    private final List<Backup> backups;

    Account(ICloud.MBSAccount mbsAccount, List<Backup> backups) {
        this.mbsAccount = Objects.requireNonNull(mbsAccount);
        this.backups = Objects.requireNonNull(backups);
    }

    public ICloud.MBSAccount mbsAccount() {
        return mbsAccount;
    }

    public List<Backup> backups() {
        return new ArrayList<>(backups);
    }

    @Override
    public String toString() {
        return "Account{" + "mbsAccount=" + mbsAccount + ", backups=" + backups + '}';
    }
}
