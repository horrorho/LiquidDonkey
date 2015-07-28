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
package com.github.horrorho.liquiddonkey.cloud.client;

import static com.github.horrorho.liquiddonkey.cloud.client.Util.path;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ProtoBufArray;
import com.github.horrorho.liquiddonkey.http.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.settings.Markers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
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
@Immutable
@ThreadSafe
public final class SnapshotClient {

    public static SnapshotClient create() {
        return new SnapshotClient(defaultMbsFileListHandler, Headers.create());
    }

    private static final Logger logger = LoggerFactory.getLogger(SnapshotClient.class);
    private static final Marker marker = MarkerFactory.getMarker(Markers.CLIENT);

    private static final ResponseHandler<List<ICloud.MBSFile>> defaultMbsFileListHandler
            = ResponseHandlerFactory.of(inputStream -> ProtoBufArray.decode(inputStream, ICloud.MBSFile.PARSER));

    private final ResponseHandler<List<ICloud.MBSFile>> mbsFileListHandler;
    private final Headers headers;

    SnapshotClient(ResponseHandler<List<ICloud.MBSFile>> mbsFileListHandler, Headers headers) {
        this.mbsFileListHandler = Objects.requireNonNull(mbsFileListHandler);
        this.headers = Objects.requireNonNull(headers);
    }

    public List<ICloud.MBSFile> files(
            HttpClient client,
            String dsPrsID,
            String mmeAuthToken,
            String mobileBackupUrl,
            String udid,
            int id,
            int listLimit
    ) throws IOException {

        logger.trace("<< files() < dsPrsID: {} udid: {} snapshot: {} listLimit: {}", dsPrsID, udid, id, listLimit);

        NameValuePair limitParameter = new BasicNameValuePair("limit", Integer.toString(listLimit));

        List<ICloud.MBSFile> files = new ArrayList<>();
        List<ICloud.MBSFile> part;
        int offset = 0;
        do {
            String uri = path(mobileBackupUrl, "mbs", dsPrsID, udid, Integer.toString(id), "listFiles");
            NameValuePair offsetParameter = new BasicNameValuePair("offset", Integer.toString(offset));

            RequestBuilder builder = RequestBuilder.get(uri).addParameters(offsetParameter, limitParameter);
            headers.mobileBackupHeaders(dsPrsID, mmeAuthToken).stream().forEach(builder::addHeader);
            HttpUriRequest get = builder.build();
            part = client.execute(get, mbsFileListHandler);

            files.addAll(part);
            offset += listLimit;
        } while (!part.isEmpty());

        logger.debug(marker, "-- files() > files: {}", files);
        logger.trace(">> files() > {}", files.size());
        return files;
    }
}
