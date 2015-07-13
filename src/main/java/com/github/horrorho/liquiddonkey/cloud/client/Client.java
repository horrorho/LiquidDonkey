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

import com.github.horrorho.liquiddonkey.data.SimplePropertyList;
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
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import static com.github.horrorho.liquiddonkey.settings.Markers.client;
import static com.github.horrorho.liquiddonkey.http.NameValuePairs.parameter;
import static com.github.horrorho.liquiddonkey.util.Bytes.hex;
import com.github.horrorho.liquiddonkey.settings.config.ClientConfig;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class Client {

    /**
     * Returns a new instance.
     *
     * @param http, not null
     * @param authentication, not null
     * @param config, not null
     * @return new instance, not null
     * @throws BadDataException
     * @throws IOException
     */
    public static Client from(Http http, Authentication authentication, ClientConfig config)
            throws BadDataException, IOException {

        logger.trace("<< from() < authentication: {} config: {}", authentication, config);

        String auth = Tokens.getInstance().basic(authentication.dsPrsId(), authentication.mmeAuthToken());
        logger.trace("-- from() >  authentication token: {}", auth);

        byte[] data
                = http.executor("https://setup.icloud.com/setup/get_account_settings", byteArrayResponseHandler)
                .headers(Headers.mmeClientInfo, Headers.authorization(auth))
                .get();
        SimplePropertyList settings = SimplePropertyList.from(data);

        Client client = newInstance(settings, config.listLimit());

        logger.trace(">> from()");
        return client;
    }

    static Client newInstance(SimplePropertyList settings, int listLimit)
            throws BadDataException, IOException {

        logger.trace("<< newInstance() < settings: {} listLimit: {}", settings, listLimit);

        String dsPrsID = settings.value("appleAccountInfo", "dsPrsID");
        String mmeAuthToken = settings.value("tokens", "mmeAuthToken");
        String mobileBackupUrl = settings.value("com.apple.mobileme", "com.apple.Dataclass.Backup", "url");
        String contentUrl = settings.value("com.apple.mobileme", "com.apple.Dataclass.Content", "url");

        String authMme = Tokens.getInstance().mobilemeAuthToken(dsPrsID, mmeAuthToken);

        Client client = new Client(
                settings,
                Headers.mobileBackupHeaders(authMme),
                Headers.contentHeaders(dsPrsID),
                dsPrsID,
                contentUrl,
                mobileBackupUrl,
                listLimit);

        logger.trace(">> newInstance() > client: {}", client);
        return client;
    }

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    // Thread safe.
    private static final ResponseHandler<byte[]> byteArrayResponseHandler = ResponseHandlerFactory.toByteArray();
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

    private final SimplePropertyList settings;
    private final List<Header> mobileBackupHeaders;
    private final List<Header> contentHeaders;
    private final String dsPrsId;
    private final String contentUrl;
    private final String mobileBackupUrl;
    private final int listFilesLimit;

    Client(
            SimplePropertyList settings,
            List<Header> mobileBackupHeaders,
            List<Header> contentHeaders,
            String dsPrsID,
            String contentUrl,
            String mobileBackupUrl,
            int listFilesLimit) {

        this.settings = Objects.requireNonNull(settings);
        this.mobileBackupHeaders = Objects.requireNonNull(mobileBackupHeaders);
        this.contentHeaders = Objects.requireNonNull(contentHeaders);
        this.dsPrsId = Objects.requireNonNull(dsPrsID);
        this.contentUrl = Objects.requireNonNull(contentUrl);
        this.mobileBackupUrl = Objects.requireNonNull(mobileBackupUrl);
        this.listFilesLimit = listFilesLimit;
    }

    <T> T mobileBackupGet(Http http, ResponseHandler<T> handler, String path, NameValuePair... parameters)
            throws IOException {

        return http.executor(path(mobileBackupUrl, path), handler)
                .headers(mobileBackupHeaders)
                .parameters(parameters)
                .get();
    }

    <T> T mobileBackupPost(Http http, ResponseHandler<T> handler, String path, byte[] postData)
            throws IOException {

        return http.executor(path(mobileBackupUrl, path), handler)
                .headers(mobileBackupHeaders)
                .post(postData);
    }

    /**
     * Queries server and returns MBSAccount.
     *
     * @param http, not null
     * @return MBSAccount, not null
     * @throws AuthenticationException
     * @throws IOException
     */
    public MBSAccount account(Http http) throws AuthenticationException, IOException {
        logger.trace("<< account()");
        MBSAccount account
                = mobileBackupGet(
                        http,
                        mbsaAccountResponseHandler,
                        path("mbs", dsPrsId));

        logger.trace(">> account() > {}", account);
        return account;
    }

    /**
     * Queries server and returns MBSBackup.
     *
     * @param http, not null
     * @param backupUDID, not null
     * @return MBSBackup, not null
     * @throws AuthenticationException
     * @throws IOException
     */
    public MBSBackup backup(Http http, ByteString backupUDID) throws AuthenticationException, IOException {
        logger.trace("<< backup() < {}", hex(backupUDID));
        MBSBackup backup
                = mobileBackupGet(
                        http,
                        mbsaBackupResponseHandler,
                        path("mbs", dsPrsId, hex(backupUDID)));

        logger.trace(">> backup() > {}", backup);
        return backup;
    }

    /**
     * Queries server and returns MBSKeySet.
     *
     * @param http, not null
     * @param backupUDID, not null
     * @return MBSKeySet, not null
     * @throws AuthenticationException
     * @throws IOException
     */
    public MBSKeySet getKeys(Http http, ByteString backupUDID) throws AuthenticationException, IOException {
        logger.trace("<< getKeys() < {}", hex(backupUDID));
        MBSKeySet keys
                = mobileBackupGet(
                        http,
                        mbsaKeySetResponseHandler,
                        path("mbs", dsPrsId, hex(backupUDID), "getKeys"));

        logger.trace(client, ">> getKeys() > {}", keys);
        return keys;
    }

    /**
     * Queries server and returns a list of MBSFiles.
     *
     * @param http, not null
     * @param backupUDID, not null
     * @param snapshotId
     * @return list of MBSFiles, not null
     * @throws AuthenticationException
     * @throws IOException
     */
    public List<MBSFile> listFiles(Http http, ByteString backupUDID, int snapshotId)
            throws AuthenticationException, IOException {
        logger.trace("<< listFiles() < backupUDID: {} snapshotId: {}", hex(backupUDID), snapshotId);

        List<MBSFile> files = new ArrayList<>();
        NameValuePair limitParameter = parameter("limit", listFilesLimit);
        int offset = 0;
        List<MBSFile> data;
        do {
            data = mobileBackupGet(
                    http,
                    mbsFileListHandler,
                    path("mbs", dsPrsId, hex(backupUDID), Integer.toString(snapshotId), "listFiles"),
                    parameter("offset", offset), limitParameter);

            files.addAll(data);
            offset += listFilesLimit;
        } while (!data.isEmpty());

        logger.trace(">> listFiles() > count: {}", files.size());
        logger.trace(client, ">> listFiles() > {}", files);
        return files;
    }

    /**
     * Queries server and returns FileGroups.
     *
     * @param http, not null
     * @param backupUdid, not null
     * @param snapshotId
     * @param files
     * @return FileGroups, not null
     * @throws AuthenticationException
     * @throws BadDataException
     * @throws IOException
     */
    public FileGroups getFileGroups(Http http, ByteString backupUdid, int snapshotId, Set<MBSFile> files)
            throws AuthenticationException, BadDataException, IOException {

        logger.trace("<< getFilesGroups() < backupUdid: {} snapshot: {} files: {}",
                hex(backupUdid), snapshotId, files.size());

        FileGroups fileGroups = authorizeGet(
                http,
                files,
                getFiles(
                        http,
                        backupUdid,
                        snapshotId,
                        files));

        logger.trace(">> getFileGroups() > count: {}", fileGroups.getFileGroupsCount());
        logger.trace(client, ">> getFileGroups() > {}", fileGroups);
        return fileGroups;
    }

    List<MBSFileAuthToken> getFiles(Http http, ByteString backupUdid, int snapshotId, Set<MBSFile> files)
            throws AuthenticationException, BadDataException, IOException {

        logger.trace("<< getFiles() < backupUdid: {} snapshot: {} files: {}",
                hex(backupUdid), snapshotId, files.size());

        List<MBSFileAuthToken> tokens;
        if (files.isEmpty()) {
            tokens = new ArrayList<>();
        } else {
            List<MBSFile> post = files.stream()
                    .map(file -> MBSFile.newBuilder().setFileID(file.getFileID()).build())
                    .collect(Collectors.toList());

            String uri = path("mbs", dsPrsId, hex(backupUdid), Integer.toString(snapshotId), "getFiles");

            byte[] encoded;
            try {
                encoded = ProtoBufArray.encode(post);
            } catch (IOException ex) {
                throw new BadDataException(ex);
            }

            tokens = mobileBackupPost(http, mbsFileAuthTokenListHandler, uri, encoded);
        }
        logger.trace(">> getFiles() > count: {}", tokens.size());
        logger.trace(client, ">> getFiles() > {}", tokens);
        return tokens;
    }

    FileGroups authorizeGet(Http http, Set<ICloud.MBSFile> files, List<MBSFileAuthToken> fileIdAuthTokens)
            throws AuthenticationException, IOException {

        logger.trace("<< authorizeGet() < tokens: {} files: {}", fileIdAuthTokens.size(), files.size());

        MBSFileAuthTokens tokens = fileIdToSignatureAuthTokens(files, fileIdAuthTokens);

        FileGroups groups;
        if (tokens.getTokensCount() == 0) {
            return FileGroups.getDefaultInstance();
        } else {
            Header mmcsAuth
                    = Headers.mmcsAuth(hex(tokens.getTokens(0).getFileID()) + " " + tokens.getTokens(0).getAuthToken());

            groups = http.executor(path(contentUrl, dsPrsId, "authorizeGet"), filesGroupsHandler)
                    .headers(mmcsAuth)
                    .headers(contentHeaders)
                    .post(tokens.toByteArray());
        }
        logger.trace(">> authorizeGet() > count: {}", groups.getFileGroupsCount());
        logger.trace(client, ">> authorizeGet)() > fileError: {}", groups.getFileErrorList());
        logger.trace(client, ">> authorizeGet() > fileChunkError: {}", groups.getFileChunkErrorList());
        logger.trace(client, ">> authorizeGet() > {}", groups);
        return groups;
    }

    ICloud.MBSFileAuthTokens fileIdToSignatureAuthTokens(
            Set<ICloud.MBSFile> files,
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

    /**
     * Queries server and returns chunk data.
     *
     * @param http, not null
     * @param chunks, not null
     * @return chunk data, not null
     * @throws AuthenticationException
     * @throws IOException
     */
    public byte[] chunks(Http http, StorageHostChunkList chunks) throws AuthenticationException, IOException {
        logger.trace("<< chunks() < chunks count: {}", chunks.getChunkInfoCount());

        HostInfo hostInfo = chunks.getHostInfo();
        String uri = hostPath(hostInfo.getScheme(), hostInfo.getHostname(), hostInfo.getUri());
        byte[] data = http.executor(uri, byteArrayResponseHandler)
                .headers(Headers.headers(hostInfo.getHeadersList()))
                .execute(hostInfo.getMethod());

        logger.trace(">> chunks() > size: {}", data.length);
        return data;
    }

    public String dsPrsId() {
        return dsPrsId;
    }

    public SimplePropertyList settings() {
        return settings;
    }

    String path(String... parts) {
        return Arrays.asList(parts).stream().collect(Collectors.joining("/"));
    }

    String hostPath(String host, String... parts) {
        return host + "://" + path(parts);
    }

    @Override
    public String toString() {
        return "Client{"
                + "mobileBackupHeaders=" + mobileBackupHeaders
                + ", contentHeaders=" + contentHeaders
                + ", dsPrsID=" + dsPrsId
                + ", contentUrl=" + contentUrl
                + ", mobileBackupUrl=" + mobileBackupUrl
                + ", listFilesLimit=" + listFilesLimit
                + '}';
    }
}
