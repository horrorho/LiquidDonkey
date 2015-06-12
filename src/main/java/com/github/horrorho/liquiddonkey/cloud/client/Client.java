/* 
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, of any person obtaining a copy
 * of this software and associated documentation files (the "Software"), of deal
 * in the Software without restriction, including without limitation the rights
 * of use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and of permit persons of whom the Software is
 * furnished of do so, subject of the following conditions:
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

import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer.FileGroups;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer.HostInfo;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer.StorageHostChunkList;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSAccount;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSBackup;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFile;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFileAuthToken;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSFileAuthTokens;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSKeySet;
import com.github.horrorho.liquiddonkey.http.responsehandler.ResponseHandlerFactory;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ProtoBufArray;
import static com.github.horrorho.liquiddonkey.settings.Markers.CLIENT;
import com.github.horrorho.liquiddonkey.util.PropertyLists;
import static com.github.horrorho.liquiddonkey.http.NameValuePairs.parameter;
import static com.github.horrorho.liquiddonkey.util.Bytes.hex;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListFormatException;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ahseya
 */
@ThreadSafe
public final class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    // Thread safe.
    private static final ResponseHandler<byte[]> byteArrayResponseHandler = ResponseHandlerFactory.toByteArray();

    // Thread safe.
    private static final ResponseHandler<List<MBSFile>> mbsFileListHandler
            = ResponseHandlerFactory.of(inputStream -> ProtoBufArray.decode(inputStream, MBSFile.PARSER));
    private static final ResponseHandler<List<MBSFileAuthToken>> mbsFileAuthTokenListHandler
            = ResponseHandlerFactory.of(inputStream -> ProtoBufArray.decode(inputStream, MBSFileAuthToken.PARSER));
    private static final ResponseHandler<FileGroups> filesGroupsHandler
            = ResponseHandlerFactory.of(FileGroups.PARSER::parseFrom);
    private static final ResponseHandler<MBSAccount> mbsaAccountResponseHandler
            = ResponseHandlerFactory.of(MBSAccount.PARSER::parseFrom);
    private static final ResponseHandler<MBSBackup> mbsaBackupResponseHandler
            = ResponseHandlerFactory.of(MBSBackup.PARSER::parseFrom);
    private static final ResponseHandler<MBSKeySet> mbsaKeySetResponseHandler
            = ResponseHandlerFactory.of(MBSKeySet.PARSER::parseFrom);

    private final List<Header> mobileBackupHeaders;
    private final List<Header> contentHeaders;
    private final Http http;
    private final String dsPrsID;
    private final String contentUrl;
    private final String mobileBackupUrl;
    private final int listFilesLimit;

    public Client(
            Http http,
            NSDictionary settings,
            String dsPrsID,
            String authMme,
            int listFilesLimit
    ) throws PropertyListFormatException {

        Objects.requireNonNull(http);
        Objects.requireNonNull(settings);
        Objects.requireNonNull(dsPrsID);
        Objects.requireNonNull(authMme);

        this.http = http;
        this.dsPrsID = dsPrsID;
        this.listFilesLimit = listFilesLimit;

        mobileBackupUrl
                = PropertyLists.stringValue(settings, "com.apple.mobileme", "com.apple.Dataclass.Backup", "url");
        contentUrl = PropertyLists.stringValue(settings, "com.apple.mobileme", "com.apple.Dataclass.Content", "url");
        mobileBackupHeaders = Collections.unmodifiableList(Headers.mobileBackupHeaders(authMme));
        contentHeaders = Collections.unmodifiableList(Headers.contentHeaders(dsPrsID));

        logger.trace("** Client() < Mobile backup url: {}", mobileBackupUrl);
        logger.trace("** Client() < Content url: {}", contentUrl);
        logger.trace("** Client() < Backup Headers: {}", mobileBackupHeaders);
        logger.trace("** Client() < Content Headers: {}", mobileBackupHeaders);
        logger.trace("** Client() < dsPrsID: {}", dsPrsID);
        logger.trace("** Client() < listFilesLimit: {}", listFilesLimit);

    }

    private <T> T mobileBackupGet(ResponseHandler<T> handler, String path, NameValuePair... parameters)
            throws IOException {

        return http.executor(path(mobileBackupUrl, path), handler)
                .headers(mobileBackupHeaders)
                .parameters(parameters)
                .get();
    }

    private <T> T mobileBackupPost(ResponseHandler<T> handler, String path, byte[] postData) throws IOException {
        return http.executor(path(mobileBackupUrl, path), handler)
                .headers(mobileBackupHeaders)
                .post(postData);
    }

    public MBSAccount account() throws IOException {
        logger.trace("<< account()");
        MBSAccount account = mobileBackupGet(mbsaAccountResponseHandler, path("mbs", dsPrsID));
        logger.trace(">> account() > {}", account);
        return account;
    }

    public MBSBackup backup(ByteString backupUDID) throws IOException {
        logger.trace("<< backup() < {}", hex(backupUDID));
        MBSBackup backup = mobileBackupGet(mbsaBackupResponseHandler, path("mbs", dsPrsID, hex(backupUDID)));
        logger.trace(">> backup() > {}", backup);
        return backup;
    }

    public MBSKeySet getKeys(ByteString backupUDID) throws IOException {
        logger.trace("<< getKeys() < {}", hex(backupUDID));
        MBSKeySet keys = mobileBackupGet(mbsaKeySetResponseHandler, path("mbs", dsPrsID, hex(backupUDID), "getKeys"));
        logger.trace(CLIENT, ">> getKeys() > {}", keys);
        return keys;
    }

    public List<MBSFile> listFiles(ByteString backupUDID, Integer snapshotId) throws IOException {
        logger.trace("<< listFiles() < backupUDID: {} snapshotId: {}", hex(backupUDID), snapshotId);

        List<MBSFile> files = new ArrayList<>();
        NameValuePair limitParameter = parameter("limit", listFilesLimit);
        int offset = 0;
        List<MBSFile> data;
        do {
            data = mobileBackupGet(mbsFileListHandler,
                    path("mbs", dsPrsID, hex(backupUDID), snapshotId.toString(), "listFiles"),
                    parameter("offset", offset), limitParameter);

            files.addAll(data);
            offset += listFilesLimit;
        } while (!data.isEmpty());

        logger.trace(">> listFiles() > count: {}", files.size());
        logger.trace(CLIENT, ">> listFiles() > {}", files);
        return files;
    }

    public FileGroups getFileGroups(ByteString backupUdid, Integer snapshot, List<MBSFile> files) throws IOException {
        logger.trace("<< getFilesGroups() < backupUdid: {} snapshot: {} files: {}",
                hex(backupUdid), snapshot, files.size());

        FileGroups fileGroups = authorizeGet(
                files,
                getFiles(
                        backupUdid,
                        snapshot,
                        files));

        logger.trace(">> getFileGroups() > count: {}", fileGroups.getFileGroupsCount());
        logger.trace(CLIENT, ">> getFileGroups() > {}", fileGroups);
        return fileGroups;
    }

    List<MBSFileAuthToken> getFiles(ByteString backupUdid, Integer snapshot, List<MBSFile> files) throws IOException {
        logger.trace("<< getFiles() < backupUdid: {} snapshot: {} files: {}",
                hex(backupUdid), snapshot, files.size());

        List<MBSFileAuthToken> tokens;
        if (files.isEmpty()) {
            tokens = new ArrayList<>();
        } else {
            List<MBSFile> post = files.stream()
                    .map(file -> MBSFile.newBuilder().setFileID(file.getFileID()).build())
                    .collect(Collectors.toList());

            String uri = path("mbs", dsPrsID, hex(backupUdid), snapshot.toString(), "getFiles");
            tokens = mobileBackupPost(mbsFileAuthTokenListHandler, uri, ProtoBufArray.encode(post));
        }
        logger.trace(">> getFiles() > count: {}", tokens.size());
        logger.trace(CLIENT, ">> getFiles() > {}", tokens);
        return tokens;
    }

    FileGroups authorizeGet(List<ICloud.MBSFile> files, List<MBSFileAuthToken> fileIdAuthTokens) throws IOException {
        logger.trace("<< authorizeGet() < tokens: {} files: {}", fileIdAuthTokens.size(), files.size());

        MBSFileAuthTokens tokens = fileIdToSignatureAuthTokens(files, fileIdAuthTokens);

        FileGroups groups;
        if (tokens.getTokensCount() == 0) {
            return FileGroups.getDefaultInstance();
        } else {
            Header mmcsAuth
                    = Headers.mmcsAuth(hex(tokens.getTokens(0).getFileID()) + " " + tokens.getTokens(0).getAuthToken());

            groups = http.executor(path(contentUrl, dsPrsID, "authorizeGet"), filesGroupsHandler)
                    .headers(mmcsAuth)
                    .headers(contentHeaders)
                    .post(tokens.toByteArray());
        }
        logger.trace(">> authorizeGet() > count: {}", groups.getFileGroupsCount());
        logger.trace(CLIENT, ">> authorizeGet)() > fileError: {}", groups.getFileErrorList());
        logger.trace(CLIENT, ">> authorizeGet() > fileChunkError: {}", groups.getFileChunkErrorList());
        logger.trace(CLIENT, ">> authorizeGet() > {}", groups);
        return groups;
    }

    ICloud.MBSFileAuthTokens fileIdToSignatureAuthTokens(
            List<ICloud.MBSFile> files,
            List<ICloud.MBSFileAuthToken> fileIdAuthTokens) {

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

    public byte[] chunks(StorageHostChunkList chunks) throws IOException {
        logger.trace("<< chunks() < chunks count: {}", chunks.getChunkInfoCount());

        HostInfo hostInfo = chunks.getHostInfo();
        String uri = hostPath(hostInfo.getScheme(), hostInfo.getHostname(), hostInfo.getUri());
        byte[] data = http.executor(uri, byteArrayResponseHandler)
                .headers(Headers.headers(hostInfo.getHeadersList()))
                .execute(hostInfo.getMethod());

        logger.trace(">> chunks() > size: {}", data.length);
        return data;
    }
    
    String path(String... parts) {
        return Arrays.asList(parts).stream().collect(Collectors.joining("/"));
    }

    String hostPath(String host, String... parts) {
        return host + "://" + path(parts);
    }
}
