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
package com.github.horrorho.liquiddonkey.cloud.client;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ProtoBufArray;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.http.Http;
import static com.github.horrorho.liquiddonkey.http.NameValuePairs.parameter;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import static com.github.horrorho.liquiddonkey.settings.Markers.client;
import com.github.horrorho.liquiddonkey.settings.config.ClientConfig;
import static com.github.horrorho.liquiddonkey.util.Bytes.hex;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client.
 *
 * @author Ahseya
 */
@ThreadSafe
public class Client {

    public static Client from(Auth auth, Settings settings, Http http, ClientConfig config) {
        return new Client(
                auth,
                settings.contentUrl(),
                settings.mobileBackupUrl(),
                http,
                Headers.create(),
                config.listLimit());
    }

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    // Thread safe
    // Thread safe.
    private static final ResponseHandler<byte[]> byteArrayResponseHandler = ResponseHandlerFactory.toByteArray();
    private static final ResponseHandler<List<ICloud.MBSFile>> mbsFileListHandler
            = ResponseHandlerFactory.of(inputStream -> ProtoBufArray.decode(inputStream, ICloud.MBSFile.PARSER));
    private static final ResponseHandler<List<ICloud.MBSFileAuthToken>> mbsFileAuthTokenListHandler
            = ResponseHandlerFactory.of(inputStream -> ProtoBufArray.decode(inputStream, ICloud.MBSFileAuthToken.PARSER));
    private static final ResponseHandler<ChunkServer.FileGroups> filesGroupsHandler
            = ResponseHandlerFactory.of(ChunkServer.FileGroups.PARSER::parseFrom);
    private static final ResponseHandler<ICloud.MBSAccount> mbsaAccountResponseHandler
            = ResponseHandlerFactory.of(ICloud.MBSAccount.PARSER::parseFrom);
    private static final ResponseHandler<ICloud.MBSBackup> mbsaBackupResponseHandler
            = ResponseHandlerFactory.of(ICloud.MBSBackup.PARSER::parseFrom);
    private static final ResponseHandler<ICloud.MBSKeySet> mbsaKeySetResponseHandler
            = ResponseHandlerFactory.of(ICloud.MBSKeySet.PARSER::parseFrom);

    private final Auth auth;
    private final String contentUrl;
    private final String mobileBackupUrl;
    private final Http http;
    private final Headers headers;
    private final int listFilesLimit;

    Client(Auth auth, String contentUrl, String mobileBackupUrl, Http http, Headers headers, int listFilesLimit) {
        this.auth = Objects.requireNonNull(auth);
        this.contentUrl = Objects.requireNonNull(contentUrl);
        this.mobileBackupUrl = Objects.requireNonNull(mobileBackupUrl);
        this.http = Objects.requireNonNull(http);
        this.headers = Objects.requireNonNull(headers);
        this.listFilesLimit = listFilesLimit;
    }

    /**
     * Queries server and returns ICloud.MBSAccount.
     *
     * @return ICloud.MBSAccount, not null
     * @throws IOException
     */
    public ICloud.MBSAccount account() throws IOException {
        logger.trace("<< account()");

        ICloud.MBSAccount account = mobileBackupGet(mbsaAccountResponseHandler, "");

        logger.trace(">> account() > {}", account);
        return account;
    }

    /**
     * Queries server and returns ICloud.MBSBackup.
     *
     * @param backupUDID, not null
     * @return ICloud.MBSBackup, not null
     * @throws IOException
     */
    public ICloud.MBSBackup backup(ByteString backupUDID) throws IOException {
        logger.trace("<< backup() < {}", hex(backupUDID));

        ICloud.MBSBackup backup = mobileBackupGet(mbsaBackupResponseHandler, hex(backupUDID));

        logger.trace(">> backup() > {}", backup);
        return backup;
    }

    /**
     * Queries server and returns ICloud.MBSKeySet.
     *
     * @param backupUDID, not null
     * @return ICloud.MBSKeySet, not null
     * @throws IOException
     */
    public ICloud.MBSKeySet keys(ByteString backupUDID) throws IOException {
        logger.trace("<< keys() < {}", hex(backupUDID));

        ICloud.MBSKeySet keys = mobileBackupGet(mbsaKeySetResponseHandler, path(hex(backupUDID), "getKeys"));

        logger.trace(client, ">> getKeys() > {}", keys);
        return keys;
    }

    /**
     * Queries server and returns a list of ICloud.MBSFile/s.
     *
     * @param backupUDID, not null
     * @param snapshotId
     * @return list of ICloud.MBSFile/s, not null
     * @throws IOException
     */
    public List<ICloud.MBSFile> files(ByteString backupUDID, int snapshotId) throws IOException {
        logger.trace("<< listFiles() < backupUDID: {} snapshotId: {}", hex(backupUDID), snapshotId);

        List<ICloud.MBSFile> files = new ArrayList<>();
        NameValuePair limitParameter = parameter("limit", listFilesLimit);
        int offset = 0;
        List<ICloud.MBSFile> data;
        do {
            data = mobileBackupGet(
                    mbsFileListHandler,
                    path(hex(backupUDID), Integer.toString(snapshotId), "listFiles"),
                    parameter("offset", offset), limitParameter);

            files.addAll(data);
            offset += listFilesLimit;
        } while (!data.isEmpty());

        logger.debug(client, "-- listFiles() > files: {}", files);
        logger.trace(">> listFiles() > count: {}", files.size());
        return files;
    }

    /**
     * Queries server and returns FileGroups.
     *
     * @param backupUdid, not null
     * @param snapshotId
     * @param files
     * @return ChunkServer.FileGroups, not null
     * @throws BadDataException
     * @throws IOException
     */
    public ChunkServer.FileGroups fileGroups(ByteString backupUdid, int snapshotId, Set<ICloud.MBSFile> files)
            throws BadDataException, IOException {

        logger.trace("<< fileGroups() < backupUdid: {} snapshot: {} files: {}",
                hex(backupUdid), snapshotId, files.size());

        // Rationalize signatures. Collisions improbable. Null signatures are empty files or other structures.
        Set<ICloud.MBSFile> unique = files.stream()
                .filter(ICloud.MBSFile::hasSignature)
                .collect(Collectors.toMap(ICloud.MBSFile::getSignature, Function.identity(), (a, b) -> a))
                .values().stream().collect(Collectors.toSet());

        logger.debug("-- fileGroups() > rationalized count: {}", unique.size());

        List<ICloud.MBSFileAuthToken> authTokens = getFiles(backupUdid, snapshotId, unique);
        ChunkServer.FileGroups fileGroups = authorizeGet(unique, authTokens);

        logger.debug(client, "-- fileGroups() > fileGroups: {}", fileGroups);
        logger.trace(">> fileGroups() > count: {}", fileGroups.getFileGroupsCount());
        return fileGroups;
    }

    List<ICloud.MBSFileAuthToken> getFiles(ByteString backupUdid, int snapshotId, Set<ICloud.MBSFile> files)
            throws BadDataException, IOException {

        logger.trace("<< getFiles() < backupUdid: {} snapshot: {} files: {}",
                hex(backupUdid), snapshotId, files.size());

        List<ICloud.MBSFileAuthToken> tokens;
        if (files.isEmpty()) {
            tokens = new ArrayList<>();
        } else {
            List<ICloud.MBSFile> post = files.stream()
                    .map(file -> ICloud.MBSFile.newBuilder().setFileID(file.getFileID()).build())
                    .collect(Collectors.toList());

            String uri = path(hex(backupUdid), Integer.toString(snapshotId), "getFiles");

            byte[] encoded;
            try {
                encoded = ProtoBufArray.encode(post);
            } catch (IOException ex) {
                throw new BadDataException(ex);
            }

            tokens = mobileBackupPost(mbsFileAuthTokenListHandler, uri, encoded);
        }

        logger.debug(client, "-- getFiles() > tokens: {}", tokens);
        logger.trace(">> getFiles() > count: {}", tokens.size());
        return tokens;
    }

    ChunkServer.FileGroups authorizeGet(Set<ICloud.MBSFile> files, List<ICloud.MBSFileAuthToken> fileIdAuthTokens)
            throws IOException {

        logger.trace("<< authorizeGet() < tokens: {} files: {}", fileIdAuthTokens.size(), files.size());

        ICloud.MBSFileAuthTokens tokens = fileIdToSignatureAuthTokens(files, fileIdAuthTokens);

        ChunkServer.FileGroups groups;
        if (tokens.getTokensCount() == 0) {
            return ChunkServer.FileGroups.getDefaultInstance();
        } else {
            Header mmcsAuth
                    = headers.mmcsAuth(hex(tokens.getTokens(0).getFileID()) + " " + tokens.getTokens(0).getAuthToken());

            groups = http.executor(path(contentUrl, auth.dsPrsId(), "authorizeGet"), filesGroupsHandler)
                    .headers(mmcsAuth).headers(auth.contentHeaders())
                    .post(tokens.toByteArray());
        }
        logger.debug(client, "-- authorizeGet() > fileError: {}", groups.getFileErrorList());
        logger.debug(client, "-- authorizeGet() > fileChunkError: {}", groups.getFileChunkErrorList());
        logger.debug(client, "-- authorizeGet() > {}", groups);
        logger.trace(">> authorizeGet() > count: {}", groups.getFileGroupsCount());
        return groups;
    }

    ICloud.MBSFileAuthTokens
            fileIdToSignatureAuthTokens(Set<ICloud.MBSFile> files, List<ICloud.MBSFileAuthToken> fileIdAuthTokens) {

        Map<ByteString, ByteString> fileIdToSignature = files.stream()
                .collect(Collectors.toMap(ICloud.MBSFile::getFileID, ICloud.MBSFile::getSignature));

        // Somewhat confusing proto definitions.
        // Each file is requested by file signature/ checksum not by it's FileID
        // Identical files share signatures and don't require multiple downloads.
        ICloud.MBSFileAuthTokens.Builder builder = ICloud.MBSFileAuthTokens.newBuilder();
        fileIdAuthTokens.stream().forEach(token -> builder.addTokens(
                ICloud.MBSFileAuthToken.newBuilder()
                .setFileID(fileIdToSignature.get(token.getFileID()))
                .setAuthToken(token.getAuthToken())
                .build()));
        return builder.build();
    }

    /**
     * Queries server and returns chunk data.
     *
     * @param chunks, not null
     * @return chunk data, not null
     * @throws IOException
     */
    public byte[] chunks(ChunkServer.StorageHostChunkList chunks) throws IOException {
        logger.trace("<< chunks() < chunks count: {}", chunks.getChunkInfoCount());

        ChunkServer.HostInfo hostInfo = chunks.getHostInfo();
        String uri = hostInfo.getScheme() + "://" + hostInfo.getHostname() + "/" + hostInfo.getUri();

        byte[] data = http.executor(uri, byteArrayResponseHandler)
                .headers(headers.headers(hostInfo.getHeadersList()))
                .execute(hostInfo.getMethod());

        logger.trace(">> chunks() > size: {}", data.length);
        return data;
    }

    <T> T mobileBackupGet(ResponseHandler<T> handler, String path, NameValuePair... parameters) throws IOException {
        return http.executor(path(mobileBackupUrl, "mbs", auth.dsPrsId(), path), handler)
                .headers(auth.mobileBackupHeaders())
                .parameters(parameters)
                .get();
    }

    <T> T mobileBackupPost(ResponseHandler<T> handler, String path, byte[] postData) throws IOException {
        return http.executor(path(mobileBackupUrl, "mbs", auth.dsPrsId(), path), handler)
                .headers(auth.mobileBackupHeaders())
                .post(postData);
    }

    String path(String... parts) {
        return Arrays.asList(parts).stream().collect(Collectors.joining("/"));
    }

    @Override
    public String toString() {
        return "Client{"
                + "auth=" + auth
                + ", contentUrl=" + contentUrl
                + ", mobileBackupUrl=" + mobileBackupUrl
                + ", http=" + http
                + ", headers=" + headers
                + ", listFilesLimit=" + listFilesLimit
                + '}';
    }
}
