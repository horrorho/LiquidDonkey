/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation list (the "Software"), to deal
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
package com.github.horrorho.liquiddonkey.cloud.clients;

import com.github.horrorho.liquiddonkey.cloud.data.Backup;
import com.github.horrorho.liquiddonkey.cloud.data.Snapshot;
import com.github.horrorho.liquiddonkey.cloud.data.Settings;
import static com.github.horrorho.liquiddonkey.cloud.clients.Util.path;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ProtoBufArray;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.settings.Markers;
import com.github.horrorho.liquiddonkey.util.Bytes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * SnapshotClient.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class SnapshotClient {

    public static SnapshotClient create(
            Backup backup,
            int listLimit) {

        return new SnapshotClient(
                defaultMbsFileListHandler,
                backup,
                listLimit,
                new BasicNameValuePair("limit", Integer.toString(listLimit)));
    }

    private static final ResponseHandler<List<ICloud.MBSFile>> defaultMbsFileListHandler
            = ResponseHandlerFactory.of(inputStream -> ProtoBufArray.decode(inputStream, ICloud.MBSFile.PARSER));

    private static final Logger logger = LoggerFactory.getLogger(SnapshotClient.class);
    private static final Marker client = MarkerFactory.getMarker(Markers.CLIENT);

    private final ResponseHandler<List<ICloud.MBSFile>> mbsFileListHandler;
    private final Backup backup;
    private final int listLimit;
    private final NameValuePair limitParameter;

    SnapshotClient(
            ResponseHandler<List<ICloud.MBSFile>> mbsFileListHandler,
            Backup backup,
            int listLimit,
            NameValuePair limitParameter) {

        this.mbsFileListHandler = Objects.requireNonNull(mbsFileListHandler);
        this.backup = Objects.requireNonNull(backup);
        this.listLimit = listLimit;
        this.limitParameter = Objects.requireNonNull(limitParameter);
    }

    /**
     * Queries the server and returns an ICloud.MBSFile list.
     *
     * @param http, not null
     * @param core, not null
     * @param snapshotId
     * @return ICloud.MBSFile list, not null
     * @throws AuthenticationException
     * @throws BadDataException
     * @throws IOException
     * @throws InterruptedException
     */
    public Snapshot get(Http http, Core core, int snapshotId)
            throws AuthenticationException, BadDataException, InterruptedException, IOException {

        logger.trace("<< get()");

        ICloud.MBSSnapshot snapshot = backup.backup().getSnapshotList().stream()
                .filter(s -> s.getSnapshotID() == snapshotId)
                .findFirst().orElse(null);

        final Snapshot instance;
        if (snapshot == null) {
            logger.warn("-- get() > no such snapshot: {}", snapshotId);
            instance = null;

        } else if (snapshot.getCommitted() == 0) {
            logger.warn("-- get() > incomplete snapshot: {}", snapshotId);
            instance = null;

        } else {
            instance = doGet(http, core, snapshot);
        }

        logger.debug(client, "-- files() > files: {}", instance.files());
        logger.trace(">> get() > count: {}", instance.files().size());
        return instance;
    }

    Snapshot doGet(Http http, Core core, ICloud.MBSSnapshot snapshot)
            throws AuthenticationException, BadDataException, InterruptedException, IOException {

        logger.trace("<< files()");

        List<ICloud.MBSFile> files = new ArrayList<>();
        List<ICloud.MBSFile> data;
        int offset = 0;
        do {
            NameValuePair offsetParameter = new BasicNameValuePair("offset", Integer.toString(offset));

            data = core.process(http, backup.dsPrsID(), (auth, settings) -> {
                String uri = path(
                        settings.mobileBackupUrl(),
                        "mbs",
                        auth.dsPrsID(),
                        Bytes.hex(backup.udid()),
                        Integer.toString(snapshot.getSnapshotID()),
                        "listFiles");

                return http.executor(uri, mbsFileListHandler)
                        .headers(auth.mobileBackupHeaders())
                        .parameters(offsetParameter, limitParameter)
                        .get();
            });

            files.addAll(data);
            offset += listLimit;
        } while (!data.isEmpty());

        Snapshot instance = Snapshot.from(backup, snapshot, files);

        logger.debug(client, "-- files() > files: {}", files);
        logger.trace(">> files() > count: {}", files.size());
        return instance;
    }
}
