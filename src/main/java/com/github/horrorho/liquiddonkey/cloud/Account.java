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
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Account.
 * <p>
 * Describes {@link ICloud.MBSAccount}.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Account {

    /**
     * Queries the client and returns a new instance.
     *
     * @param client, not null
     * @return new instance, not null
     * @throws IOException
     */
    public static Account of(Client client) throws IOException {
        logger.trace("<< from()");

        Account instance = new Account(client.account());

        logger.trace(">> from() > {}", instance);
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(Account.class);

    private final ICloud.MBSAccount account;

    Account(ICloud.MBSAccount account) {
        this.account = Objects.requireNonNull(account);
    }

    public String id() {
        return account.getAccountID();
    }

    public List<ByteString> list() {
        return account.getBackupUDIDList();
    }

    @Override
    public String toString() {
        return "Account{" + "account=" + account + '}';
    }
}
