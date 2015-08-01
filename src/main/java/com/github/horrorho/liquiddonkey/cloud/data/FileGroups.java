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
package com.github.horrorho.liquiddonkey.cloud.data;

import com.github.horrorho.liquiddonkey.cloud.client.FileGroupsClient;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileGroups.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class FileGroups {

    public static ChunkServer.FileGroups from(HttpClient client, Core core, String mmeAuthToken, Snapshot snapshot)
            throws BadDataException, IOException {

        logger.trace("<< get() < dsPrsID: {} udid: {} snapshot: {} files: {}",
                snapshot.dsPrsID(), snapshot.backupUDID(), snapshot.snapshotID(), snapshot.filesCount());

        if (!core.dsPrsID().equals(snapshot.dsPrsID())) {
            logger.error("-- from() > dsPrsID mismatch, core: {} snapshot: {}", core.dsPrsID(), snapshot.dsPrsID());
        }

        // Signatures represent unique files hashes.
        // Discard duplicate signatures. Collisions unlikely.
        // Null signatures are empty files/ non-downloadables.        
        Collection<ICloud.MBSFile> unique = snapshot.files().stream()
                .filter(ICloud.MBSFile::hasSignature)
                .collect(Collectors.toCollection(HashSet::new));
        logger.debug("-- get() > rationalized count: {}", unique.size());

        List<ICloud.MBSFileAuthToken> authTokens = unique.isEmpty()
                ? new ArrayList<>()
                : fileGroupsClient.getFiles(
                        client,
                        snapshot.dsPrsID(),
                        mmeAuthToken,
                        core.mobileBackupUrl(),
                        snapshot.backupUDID(),
                        Integer.toString(snapshot.snapshotID()),
                        unique);

        ICloud.MBSFileAuthTokens tokens = fileIdToSignatureAuthTokens(unique, authTokens);

        ChunkServer.FileGroups fileGroups = authTokens.isEmpty()
                ? ChunkServer.FileGroups.getDefaultInstance()
                : fileGroupsClient.authorizeGet(client, snapshot.dsPrsID(), core.contentUrl(), tokens);

        logger.trace(">> get() > fileGroups: {}", fileGroups.getFileGroupsCount());
        return fileGroups;
    }

    static ICloud.MBSFileAuthTokens fileIdToSignatureAuthTokens(
            Collection<ICloud.MBSFile> files,
            Collection<ICloud.MBSFileAuthToken> fileIdAuthTokens) {

        Map<ByteString, ByteString> fileIdToSignature = files.stream()
                .collect(Collectors.toMap(ICloud.MBSFile::getFileID, ICloud.MBSFile::getSignature));

        // Somewhat confusing proto definitions.
        // Each file is requested by file signature/ checksum not by it's FileID
        ICloud.MBSFileAuthTokens.Builder builder = ICloud.MBSFileAuthTokens.newBuilder();
        fileIdAuthTokens.stream().forEach(token -> builder.addTokens(
                ICloud.MBSFileAuthToken.newBuilder()
                .setFileID(fileIdToSignature.get(token.getFileID()))
                .setAuthToken(token.getAuthToken())
                .build()));
        return builder.build();
    }

    private static final Logger logger = LoggerFactory.getLogger(FileGroups.class);

    private static final FileGroupsClient fileGroupsClient = FileGroupsClient.create();
}
