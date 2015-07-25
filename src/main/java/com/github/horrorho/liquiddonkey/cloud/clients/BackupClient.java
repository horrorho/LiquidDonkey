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
import com.github.horrorho.liquiddonkey.http.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.settings.Markers;
import java.io.IOException;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
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

    public static BackupClient create() {
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(BackupClient.class);
    private static final Marker marker = MarkerFactory.getMarker(Markers.CLIENT);

    private static final BackupClient instance = new BackupClient(
            ResponseHandlerFactory.of(ICloud.MBSBackup.PARSER::parseFrom),
            ResponseHandlerFactory.of(ICloud.MBSKeySet.PARSER::parseFrom),
            Headers.create());

    private final ResponseHandler<ICloud.MBSBackup> mbsaBackupResponseHandler;
    private final ResponseHandler<ICloud.MBSKeySet> mbsaKeySetResponseHandler;
    private final Headers headers;

    BackupClient(
            ResponseHandler<ICloud.MBSBackup> mbsaBackupResponseHandler,
            ResponseHandler<ICloud.MBSKeySet> mbsaKeySetResponseHandler,
            Headers headers) {

        this.mbsaBackupResponseHandler = Objects.requireNonNull(mbsaBackupResponseHandler);
        this.mbsaKeySetResponseHandler = Objects.requireNonNull(mbsaKeySetResponseHandler);
        this.headers = Objects.requireNonNull(headers);
    }

    public ICloud.MBSBackup mbsBackup(
            HttpClient client,
            String dsPrsID,
            String mmeAuthToken,
            String mobileBackupUrl,
            String udid
    ) throws IOException {

        logger.trace("<< mbsBackup() < dsPrsID: {} udid: {}", dsPrsID, udid);

        String get = path(mobileBackupUrl, "mbs", dsPrsID, udid);
        HttpGet getMBSBackup = new HttpGet(get);
        headers.mobileBackupHeaders(dsPrsID, mmeAuthToken).stream().forEach(getMBSBackup::addHeader);
        ICloud.MBSBackup mbsBackup = client.execute(getMBSBackup, mbsaBackupResponseHandler);

        logger.trace(">> mbsaBackup() > {}", mbsBackup);
        return mbsBackup;
    }

    public ICloud.MBSKeySet mbsKeySet(
            HttpClient client,
            String dsPrsID,
            String mmeAuthToken,
            String mobileBackupUrl,
            String udid
    ) throws IOException {

        logger.trace("<< mbsKeySet() < dsPrsID: {} udid: {}", dsPrsID, udid);

        String get = path(mobileBackupUrl, "mbs", dsPrsID, udid, "getKeys");
        HttpGet getMBSBackup = new HttpGet(get);
        headers.mobileBackupHeaders(dsPrsID, mmeAuthToken).stream().forEach(getMBSBackup::addHeader);
        ICloud.MBSKeySet mbsKeySet = client.execute(getMBSBackup, mbsaKeySetResponseHandler);

        logger.debug(marker, ">> mbsKeySet() > mbsKeySet: {}", mbsKeySet);
        logger.trace(">> mbsKeySet() > count: {}", mbsKeySet.getKeyCount());
        return mbsKeySet;
    }
}
