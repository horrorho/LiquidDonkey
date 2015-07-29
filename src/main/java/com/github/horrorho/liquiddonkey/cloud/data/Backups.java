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
package com.github.horrorho.liquiddonkey.cloud.data;

import com.github.horrorho.liquiddonkey.cloud.client.BackupClient;
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBagManager;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backups.
 *
 * @author Ahseya
 */
public class Backups {

    public static List<Backup> from(HttpClient client, Core core, Account account)
            throws IOException, BadDataException {
        List<Backup> list = new ArrayList<>();
        for (ByteString udid : account.mbsAccount().getBackupUDIDList()) {
            list.add(backup(client, core, account, udid));
        }
        return list;
    }

    public static Backup from(HttpClient client, Core core, Account account, ByteString udid)
            throws IOException, BadDataException {

        final Backup backup;
        if (account.mbsAccount().getBackupUDIDList().contains(udid)) {
            backup = backup(client, core, account, udid);
        } else {
            backup = null;
        }
        return backup;
    }

    static Backup backup(HttpClient client, Core core, Account account, ByteString udid)
            throws IOException, BadDataException {

        logger.trace("<< from() < dsPrsID: {} udid: {}", core.dsPrsID(), Bytes.hex(udid));

        if (!core.dsPrsID().equals(account.dsPrsID())) {
            logger.error("-- from() > dsPrsID mismatch, core: {} account: {}", core.dsPrsID(), account.dsPrsID());
        }

        String udidString = Bytes.hex(udid);

        ICloud.MBSBackup mbsBackup = backupClient.mbsBackup(
                client,
                core.dsPrsID(),
                core.mmeAuthToken(),
                core.mobileBackupUrl(),
                udidString);

        ICloud.MBSKeySet mbsKeySet = backupClient.mbsKeySet(
                client,
                core.dsPrsID(),
                core.mmeAuthToken(),
                core.mobileBackupUrl(),
                udidString);

        KeyBagManager keyBagManager = KeyBagManager.from(mbsKeySet);
        Backup backup = new Backup(account, mbsBackup, mbsKeySet, keyBagManager);

        logger.trace(">> from() > backup: {}", backup);
        return backup;
    }

    private static final Logger logger = LoggerFactory.getLogger(Backups.class);

    private static final BackupClient backupClient = BackupClient.create();
}
