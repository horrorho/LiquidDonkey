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

import com.github.horrorho.liquiddonkey.cloud.data.Settings;
import static com.github.horrorho.liquiddonkey.cloud.clients.Util.path;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AccountClient.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class AccountClient {

    public static AccountClient create() {
        return new AccountClient(                defaultMbsaAccountResponseHandler);
    }

    private static final Logger logger = LoggerFactory.getLogger(AccountClient.class);

    private static final ResponseHandler<ICloud.MBSAccount> defaultMbsaAccountResponseHandler
            = ResponseHandlerFactory.of(ICloud.MBSAccount.PARSER::parseFrom);

    private final ResponseHandler<ICloud.MBSAccount> mbsaAccountResponseHandler;

    public AccountClient(            ResponseHandler<ICloud.MBSAccount> mbsaAccountResponseHandler) {

        this.mbsaAccountResponseHandler = mbsaAccountResponseHandler;
    }

    /**
     * Queries the server and returns ICloud.MBSAccount.
     *
     * @param http, not null
     * @param core, not null
     * @return ICloud.MBSAccount, not null
     * @throws IOException
     * @throws BadDataException
     * @throws AuthenticationException
     * @throws InterruptedException
     */
    public ICloud.MBSAccount get(Http http, Core core)
            throws AuthenticationException, BadDataException, InterruptedException, IOException {
        logger.trace("<< from()");

        ICloud.MBSAccount instance = core.process(http, core.dsPrsID(), (auth, settings) -> {

            String uri = path(settings.mobileBackupUrl(), "mbs", auth.dsPrsID());
            return http.executor(uri, mbsaAccountResponseHandler)
                    .headers(auth.mobileBackupHeaders())
                    .get();
        });

        logger.trace(">> from() > {}", instance);
        return instance;
    }
}
