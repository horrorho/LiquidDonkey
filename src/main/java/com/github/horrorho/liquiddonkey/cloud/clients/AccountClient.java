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
package com.github.horrorho.liquiddonkey.cloud.clients;

import static com.github.horrorho.liquiddonkey.cloud.clients.Util.path;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.http.ResponseHandlerFactory;
import java.io.IOException;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AccountClient.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class AccountClient {

    public static AccountClient create() {
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(AccountClient.class);

    private static final AccountClient instance
            = new AccountClient(
                    ResponseHandlerFactory.of(ICloud.MBSAccount.PARSER::parseFrom),
                    Headers.create());

    private final ResponseHandler<ICloud.MBSAccount> mbsAccountResponseHandler;
    private final Headers headers;

    AccountClient(ResponseHandler<ICloud.MBSAccount> mbsaAccountResponseHandler, Headers headers) {
        this.mbsAccountResponseHandler = Objects.requireNonNull(mbsaAccountResponseHandler);
        this.headers = Objects.requireNonNull(headers);
    }

    /**
     * Queries the server and returns ICloud.MBSAccount.
     *
     * @param client, not null
     * @param dsPrsID, not null
     * @param mmeAuthToken, not null
     * @param mobileBackupUrl, not null
     * @return ICloud.MBSAccount, not null
     * @throws IOException
     * @throws AuthenticationException
     */
    public ICloud.MBSAccount get(HttpClient client, String dsPrsID, String mmeAuthToken, String mobileBackupUrl)
            throws IOException {

        logger.trace("<< get() < dsPrsID: {} mmeAuthToken: {} mobileBackupUrl: {}",
                dsPrsID, mmeAuthToken, mobileBackupUrl);

        HttpGet get = new HttpGet(path(mobileBackupUrl, "mbs", dsPrsID));
        headers.mobileBackupHeaders(dsPrsID, mmeAuthToken).stream().forEach(get::addHeader);
        ICloud.MBSAccount mbsAccount = client.execute(get, mbsAccountResponseHandler);

        logger.trace(">> get() > {}", mbsAccount);
        return mbsAccount;
    }
}
