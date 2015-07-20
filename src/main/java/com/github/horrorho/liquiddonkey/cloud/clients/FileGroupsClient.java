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

import static com.github.horrorho.liquiddonkey.cloud.clients.Util.path;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ProtoBufArray;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.settings.Markers;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.Header;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * FileGroupsClient.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class FileGroupsClient {

    public static FileGroupsClient create(
            Authenticator authenticator,
            String backupUdid,
            String mobileBackupUrl,
            String contentUrl) {

        return new FileGroupsClient(
                defaultFilesGroupsHandler,
                defaultMbsFileAuthTokenListHandler,
                authenticator,
                backupUdid,
                mobileBackupUrl,
                contentUrl,
                Headers.create());
    }

    private static final ResponseHandler<ChunkServer.FileGroups> defaultFilesGroupsHandler
            = ResponseHandlerFactory.of(ChunkServer.FileGroups.PARSER::parseFrom);
    private static final ResponseHandler<List<ICloud.MBSFileAuthToken>> defaultMbsFileAuthTokenListHandler
            = ResponseHandlerFactory.of(inputStream -> ProtoBufArray.decode(inputStream, ICloud.MBSFileAuthToken.PARSER));

    private static final Logger logger = LoggerFactory.getLogger(FileGroupsClient.class);
    private static final Marker client = MarkerFactory.getMarker(Markers.CLIENT);

    private final ResponseHandler<ChunkServer.FileGroups> filesGroupsHandler;
    private final ResponseHandler<List<ICloud.MBSFileAuthToken>> mbsFileAuthTokenListHandler;
    private final Authenticator authenticator;
    private final String backupUdid;
    private final String mobileBackupUrl;
    private final String contentUrl;
    private final Headers headers;

    private FileGroupsClient(
            ResponseHandler<ChunkServer.FileGroups> filesGroupsHandler,
            ResponseHandler<List<ICloud.MBSFileAuthToken>> mbsFileAuthTokenListHandler,
            Authenticator authenticator,
            String backupUdid,
            String mobileBackupUrl,
            String contentUrl,
            Headers headers) {

        this.filesGroupsHandler = Objects.requireNonNull(filesGroupsHandler);
        this.mbsFileAuthTokenListHandler = Objects.requireNonNull(mbsFileAuthTokenListHandler);
        this.authenticator = Objects.requireNonNull(authenticator);
        this.backupUdid = Objects.requireNonNull(backupUdid);
        this.mobileBackupUrl = Objects.requireNonNull(mobileBackupUrl);
        this.contentUrl = Objects.requireNonNull(contentUrl);
        this.headers = Objects.requireNonNull(headers);
    }

    /**
     * Queries the server and returns ChunkServer.FileGroupsClient.
     *
     * @param http, not null
     * @param snapshotId
     * @param files, not null
     * @return ChunkServer.FileGroupsClient, not null
     * @throws AuthenticationException
     * @throws BadDataException
     * @throws IOException
     * @throws InterruptedException
     */
    public ChunkServer.FileGroups fileGroups(Http http, int snapshotId, Collection<ICloud.MBSFile> files)
            throws AuthenticationException, BadDataException, InterruptedException, IOException {

        logger.trace("<< fileGroups() < backupUdid: {} snapshot: {} files: {}", backupUdid, snapshotId, files.size());
        logger.debug(client, "-- fileGroups() < files: {}", files);

        // Rationalize signatures. Collisions ignored. Null signatures are empty files/ non-downloadables.
        Collection<ICloud.MBSFile> unique = files.stream()
                .filter(ICloud.MBSFile::hasSignature)
                .collect(Collectors.toMap(ICloud.MBSFile::getSignature, Function.identity(), (a, b) -> a))
                .values();

        logger.debug("-- fileGroups() > rationalized count: {}", unique.size());

        List<ICloud.MBSFileAuthToken> authTokens = unique.isEmpty()
                ? new ArrayList<>()
                : getFiles(http, snapshotId, unique);

        ChunkServer.FileGroups fileGroups = authTokens.isEmpty()
                ? ChunkServer.FileGroups.getDefaultInstance()
                : authorizeGet(http, unique, authTokens);

        logger.debug(client, "-- fileGroups() > fileGroups: {}", fileGroups);
        logger.trace(">> fileGroups() > count: {}", fileGroups.getFileGroupsCount());
        return fileGroups;
    }

    List<ICloud.MBSFileAuthToken> getFiles(Http http, int snapshotId, Collection<ICloud.MBSFile> files)
            throws AuthenticationException, BadDataException, InterruptedException, IOException {

        logger.trace("<< getFiles() < backupUdid: {} snapshot: {} files: {}", backupUdid, snapshotId, files.size());
        logger.debug(client, "-- getFiles() < files: {}", files);

        List<ICloud.MBSFile> post = files.stream()
                .map(file -> ICloud.MBSFile.newBuilder().setFileID(file.getFileID()).build())
                .collect(Collectors.toList());

        byte[] encoded;
        try {
            encoded = ProtoBufArray.encode(post);
        } catch (IOException ex) {
            throw new BadDataException(ex);
        }

        List<ICloud.MBSFileAuthToken> tokens = authenticator.process(http, auth -> {
            String uri = path(mobileBackupUrl, "mbs", auth.dsPrsId(), backupUdid, Integer.toString(snapshotId), "getFiles");
            return http.executor(uri, mbsFileAuthTokenListHandler)
                    .headers(auth.mobileBackupHeaders())
                    .post(encoded);
        });

        logger.debug(client, "-- getFiles() > tokens: {}", tokens);
        logger.trace(">> getFiles() > count: {}", tokens.size());
        return tokens;
    }

    ChunkServer.FileGroups authorizeGet(
            Http http,
            Collection<ICloud.MBSFile> files,
            Collection<ICloud.MBSFileAuthToken> fileIdAuthTokens
    ) throws AuthenticationException, BadDataException, InterruptedException, IOException {

        logger.trace("<< authorizeGet() < tokens: {} files: {}", fileIdAuthTokens.size(), files.size());

        ICloud.MBSFileAuthTokens tokens = fileIdToSignatureAuthTokens(files, fileIdAuthTokens);

        ChunkServer.FileGroups groups;
        if (tokens.getTokensCount() == 0) {
            groups = ChunkServer.FileGroups.getDefaultInstance();

        } else {
            Header mmcsAuth = headers.mmcsAuth(Bytes.hex(tokens.getTokens(0).getFileID())
                    + " " + tokens.getTokens(0).getAuthToken());

            groups = authenticator.process(http, auth -> {
                String uri = path(contentUrl, auth.dsPrsId(), "authorizeGet");
                return http.executor(uri, filesGroupsHandler)
                        .headers(mmcsAuth).headers(auth.contentHeaders())
                        .post(tokens.toByteArray());
            });
        }

        logger.debug(client, "-- authorizeGet() > fileError: {}", groups.getFileErrorList());
        logger.debug(client, "-- authorizeGet() > fileChunkError: {}", groups.getFileChunkErrorList());
        logger.debug(client, "-- authorizeGet() > {}", groups);
        logger.trace(">> authorizeGet() > count: {}", groups.getFileGroupsCount());
        return groups;
    }

    ICloud.MBSFileAuthTokens fileIdToSignatureAuthTokens(
            Collection<ICloud.MBSFile> files,
            Collection<ICloud.MBSFileAuthToken> fileIdAuthTokens) {

        Map<ByteString, ByteString> fileIdToSignature = files.stream()
                .collect(Collectors.toMap(ICloud.MBSFile::getFileID, ICloud.MBSFile::getSignature));

        // Somewhat confusing proto definitions.
        // Each file is requested by file signature/ checksum not by it's FileID
        // Identical files share signatures and don't require multiple downloads.
        // Collisions improbable.
        ICloud.MBSFileAuthTokens.Builder builder = ICloud.MBSFileAuthTokens.newBuilder();
        fileIdAuthTokens.stream().forEach(token -> builder.addTokens(
                ICloud.MBSFileAuthToken.newBuilder()
                .setFileID(fileIdToSignature.get(token.getFileID()))
                .setAuthToken(token.getAuthToken())
                .build()));
        return builder.build();
    }
}
