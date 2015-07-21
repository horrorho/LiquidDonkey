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
package com.github.horrorho.liquiddonkey.cloud.data;

import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBagManager;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Backup.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Backup {

    public static Backup from(ICloud.MBSAccount account, ICloud.MBSBackup backup, ICloud.MBSKeySet mbsKeySet)
            throws BadDataException {

        KeyBagManager keyBagManager = KeyBagManager.from(mbsKeySet);

        List<ICloud.MBSSnapshot> snapshots = new ArrayList<>(backup.getSnapshotList());
        Collections.sort(snapshots, Comparator.comparingLong(ICloud.MBSSnapshot::getSnapshotID));

        return new Backup(
                account.getAccountID(), // dsPrsID
                backup,
                snapshots,
                keyBagManager);
    }

    private final String dsPrsID;
    private final ICloud.MBSBackup backup;
    private final List<ICloud.MBSSnapshot> snapshots;
    private final KeyBagManager keyBagManager;

    Backup(String dsPrsID, ICloud.MBSBackup backup, List<ICloud.MBSSnapshot> snapshots, KeyBagManager keyBagManager) {
        this.dsPrsID = Objects.requireNonNull(dsPrsID);
        this.backup = Objects.requireNonNull(backup);
        this.snapshots = Objects.requireNonNull(snapshots);
        this.keyBagManager = Objects.requireNonNull(keyBagManager);
    }

    /**
     * Returns the ICloud.MBSBackup.
     *
     * @return ICloud.MBSBackup, not null
     */
    public ICloud.MBSBackup backup() {
        return backup;
    }

    /**
     * Returns the dsPrsID.
     *
     * @return dsPrsID, not null
     */
    public String dsPrsID() {
        return dsPrsID;
    }

    /**
     * Returns the keyBagManager.
     *
     * @return keyBagManager, not null
     */
    public KeyBagManager keybagManager() {
        return keyBagManager;
    }

    /**
     * Returns a copy of the ICloud.MBSSnapshot list, ordered by ascending id.
     *
     * @return new ICloud.MBSSnapshot list ordered by ascending id, not null
     */
    public List<ICloud.MBSSnapshot> snapshots() {
        return new ArrayList<>(snapshots);
    }

    /**
     * Returns the UDID.
     *
     * @return UDID, not null
     */
    public ByteString udid() {
        return backup.getBackupUDID();
    }

    @Override
    public String toString() {
        return "Backup{"
                + "dsPrsID=" + dsPrsID
                + ", backup=" + backup
                + ", snapshots=" + snapshots
                + ", keyBagManager=" + keyBagManager
                + '}';
    }
}
