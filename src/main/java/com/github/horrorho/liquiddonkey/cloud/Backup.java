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

import com.github.horrorho.liquiddonkey.cloud.clients.BackupClient;
import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBagManager;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backup.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class Backup {

    public static List<Backup> from(Http http, Account account)
            throws AuthenticationException, BadDataException, InterruptedException, IOException {

        List<Backup> backups = new ArrayList<>();
        for (ByteString udid : account.backupUdids()) {
            backups.add(Backup.from(http, account, udid));
        }

        return backups;
    }

    /**
     * Queries the server and returns a new Backup instance.
     *
     * @param http, not null
     * @param account, not null
     * @param backupUdid, not null
     * @return a new Backup instance, not null
     * @throws AuthenticationException
     * @throws BadDataException
     * @throws IOException
     * @throws InterruptedException
     */
    public static Backup from(Http http, Account account, ByteString backupUdid)
            throws AuthenticationException, BadDataException, InterruptedException, IOException {

        logger.trace("<< from() < account: {} backup: {}", account.settings().appleId(), backupUdid);

        BackupClient backups = BackupClient.create(account.authenticator(), account.settings().mobileBackupUrl());

        ICloud.MBSBackup mbsBackup = backups.backup(http, backupUdid);
        ICloud.MBSKeySet mbsKeySet = backups.keySet(http, backupUdid);

        Backup instance = from(account, mbsBackup, mbsKeySet);

        logger.trace(">> from() > udid: {}", instance);
        return instance;
    }

    static Backup from(Account account, ICloud.MBSBackup backup, ICloud.MBSKeySet mbsKeySet)
            throws BadDataException {

        KeyBagManager keyBagManager = KeyBagManager.from(mbsKeySet);

        List<ICloud.MBSSnapshot> snapshots = new ArrayList<>(backup.getSnapshotList());
        Collections.sort(snapshots, Comparator.comparingLong(ICloud.MBSSnapshot::getSnapshotID));

        return new Backup(
                account,
                backup,
                snapshots,
                keyBagManager);
    }

    private static final Logger logger = LoggerFactory.getLogger(Backup.class);

    private final Account account;
    private final ICloud.MBSBackup backup;
    private final List<ICloud.MBSSnapshot> snapshots;
    private final KeyBagManager keyBagManager;

    Backup(
            Account account,
            ICloud.MBSBackup backup,
            List<ICloud.MBSSnapshot> snapshots,
            KeyBagManager keyBagManager) {

        this.account = Objects.requireNonNull(account);
        this.backup = Objects.requireNonNull(backup);
        this.snapshots = Objects.requireNonNull(snapshots);
        this.keyBagManager = Objects.requireNonNull(keyBagManager);
    }

    /**
     * Returns the Account.
     *
     * @return the dsPrsId, not null
     */
    public Account account() {
        return account;
    }

    /**
     * Returns ICloud.MBSBackup.
     *
     * @return ICloud.MBSBackup, not null
     */
    public ICloud.MBSBackup backup() {
        return backup;
    }

    /**
     * Returns a copy of the ICloud.MBSSnapshot list, ordered by ascending id.
     *
     * @return new ICloud.MBSSnapshot list ordered by ascending id, not null
     */
    public List<ICloud.MBSSnapshot> snapshots() {
        return new ArrayList<>(snapshots);
    }

    public KeyBagManager keybagManager() {
        return keyBagManager;
    }

    public ByteString udid() {
        return backup.getBackupUDID();
    }

    /**
     * Returns a hex string representation of the Udid.
     *
     * @return hex string representation of the Udid, not null
     */
    public String udidString() {
        return Bytes.hex(backup.getBackupUDID());
    }

    @Override
    public String toString() {
        return "Backup{"
                + "account=" + account.settings().appleId()
                + ", backup=" + backup
                + ", snapshots=" + snapshots
                + ", keyBagManager=" + keyBagManager
                + '}';
    }
}
