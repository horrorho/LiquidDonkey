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

import com.github.horrorho.liquiddonkey.cloud.keybag.KeyBagManager;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.util.Bytes;
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
public class Backup extends Account {

    private final ICloud.MBSBackup mbsBackup;
    private final ICloud.MBSKeySet mbsKeySet;
    private final KeyBagManager keyBagManager;
    private final Account account;

    Backup(Account account, ICloud.MBSBackup mbsBackup, ICloud.MBSKeySet mbsKeySet, KeyBagManager keyBagManager) {
        super(account);

        this.mbsBackup = Objects.requireNonNull(mbsBackup);
        this.account = Objects.requireNonNull(account);
        this.mbsKeySet = Objects.requireNonNull(mbsKeySet);
        this.keyBagManager = keyBagManager;
    }

    Backup(Backup backup) {
        this(backup.account, backup.mbsBackup(), backup.mbsKeySet(), backup.keyBagManager());
    }

    public final Account account() {
        return account;
    }

    public final String backupUDID() {
        return Bytes.hex(mbsBackup.getBackupUDID());
    }

    public final ICloud.MBSBackup mbsBackup() {
        return mbsBackup;
    }

    public final ICloud.MBSKeySet mbsKeySet() {
        return mbsKeySet;
    }

    public final KeyBagManager keyBagManager() {
        return keyBagManager;
    }

    @Override
    public String toString() {
        return "Backup{" + "mbsBackup=" + mbsBackup + ", account=" + account + '}';
    }
}
