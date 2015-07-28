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
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.http.client.HttpClient;

/**
 * Backup.
 *
 * @author Ahseya
 */
public class Backup extends Account {

    

    private final ICloud.MBSBackup backup;
    private final ICloud.MBSKeySet keySet;
    private final KeyBagManager keyBagManager;
    private final Account account;

    Backup(Account account, ICloud.MBSBackup backup, ICloud.MBSKeySet keySet, KeyBagManager keyBagManager) {
        super(account);

        this.backup = Objects.requireNonNull(backup);
        this.account = Objects.requireNonNull(account);
        this.keySet = Objects.requireNonNull(keySet);
        this.keyBagManager = keyBagManager;
    }

    Backup(Backup backup) {
        this(backup.account, backup.backup(), backup.keySet(), backup.keyBagManager());
    }

    public final String backupUDID() {
        return Bytes.hex(backup.getBackupUDID());
    }

    public final ICloud.MBSBackup backup() {
        return backup;
    }

    public final ICloud.MBSKeySet keySet() {
        return keySet;
    }

    public final KeyBagManager keyBagManager() {
        return keyBagManager;
    }

    @Override
    public String toString() {
        return "Backup{" + "backup=" + backup + ", account=" + account + '}';
    }
}
