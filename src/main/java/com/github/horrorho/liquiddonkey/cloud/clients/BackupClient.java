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
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.settings.Markers;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * BackupClient.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class BackupClient {

    public static BackupClient create(Account account)
            throws IOException {

        return new BackupClient(
                defaultMbsaBackupResponseHandler,
                defaultMbsaKeySetResponseHandler,
                account);
    }

    private static final Logger logger = LoggerFactory.getLogger(BackupClient.class);
    private static final Marker client = MarkerFactory.getMarker(Markers.CLIENT);

    private static final ResponseHandler<ICloud.MBSBackup> defaultMbsaBackupResponseHandler
            = ResponseHandlerFactory.of(ICloud.MBSBackup.PARSER::parseFrom);
    private static final ResponseHandler<ICloud.MBSKeySet> defaultMbsaKeySetResponseHandler
            = ResponseHandlerFactory.of(ICloud.MBSKeySet.PARSER::parseFrom);

    private final ResponseHandler<ICloud.MBSBackup> mbsaBackupResponseHandler;
    private final ResponseHandler<ICloud.MBSKeySet> mbsaKeySetResponseHandler;
    private final Account account;

    public BackupClient(
            ResponseHandler<ICloud.MBSBackup> mbsaBackupResponseHandler,
            ResponseHandler<ICloud.MBSKeySet> mbsaKeySetResponseHandler,
            Account account) {

        this.mbsaBackupResponseHandler = Objects.requireNonNull(mbsaBackupResponseHandler);
        this.mbsaKeySetResponseHandler = Objects.requireNonNull(mbsaKeySetResponseHandler);
        this.account = Objects.requireNonNull(account);
    }

    /**
     * Queries the server and returns ICloud.MBSAccount.
     *
     * @param http, not null
     * @param authenticators, not null
     * @param backupUdid, not null
     * @return ICloud.MBSAccount, not null
     * @throws AuthenticationException
     * @throws BadDataException
     * @throws IOException
     * @throws InterruptedException
     */
    public Backup get(Http http, Authenticator authenticator, ByteString backupUdid)
            throws AuthenticationException, BadDataException, InterruptedException, IOException {

        logger.trace("<< backup()");
// TODO no such get
        Settings settings = account.settings();

        ICloud.MBSBackup mbsBackup = authenticator.process(http, auth -> {

            String uri = path(settings.mobileBackupUrl(), "mbs", auth.dsPrsId(), Bytes.hex(backupUdid));
            return http.executor(uri, mbsaBackupResponseHandler)
                    .headers(auth.mobileBackupHeaders())
                    .get();
        });

        ICloud.MBSKeySet mbsKeySet = authenticator.process(http, auth -> {

            String uri = path(settings.mobileBackupUrl(), "mbs", auth.dsPrsId(), Bytes.hex(backupUdid), "getKeys");
            return http.executor(uri, mbsaKeySetResponseHandler)
                    .headers(auth.mobileBackupHeaders())
                    .get();
        });

        Backup instance = Backup.from(account, mbsBackup, mbsKeySet);
        logger.trace(">> backup() > {}", instance);
        return instance;
    }
}
