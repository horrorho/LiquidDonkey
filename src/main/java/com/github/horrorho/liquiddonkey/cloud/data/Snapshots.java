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

import com.github.horrorho.liquiddonkey.cloud.client.SnapshotClient;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.settings.Markers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Snapshots.
 *
 * @author Ahseya
 */
public class Snapshots {

    public static final Snapshot from(Snapshot snapshot, Predicate<ICloud.MBSFile> predicate) {
        List<ICloud.MBSFile> filtered = snapshot.files().stream().filter(predicate).collect(Collectors.toList());
        return new Snapshot(snapshot, filtered);
    }

    public static final Snapshot from(HttpClient client, Backup backup, int id, int listLimit) throws IOException {
        logger.trace("<< from() < dsPrsID: {} udid: {} id: {} listLimit: {}",
                backup.dsPrsID(), backup.backupUDID(), id, listLimit);

        ICloud.MBSSnapshot mbsSnapshot = backup.backup().getSnapshotList().stream()
                .filter(s -> s.getSnapshotID() == id)
                .findFirst().orElse(null);

        final Snapshot snapshot;

        if (mbsSnapshot == null) {
            snapshot = null;
        } else {
            List<ICloud.MBSFile> files = snapshotClient.files(
                    client,
                    backup.dsPrsID(),
                    backup.mmeAuthToken(),
                    backup.mobileBackupUrl(),
                    backup.backupUDID(),
                    mbsSnapshot.getSnapshotID(),
                    listLimit);

            snapshot = new Snapshot(backup, mbsSnapshot, files);
        }

        logger.debug(marker, "-- from() > snapshots: {}", snapshot);
        logger.trace(">> from() > files: {}", snapshot == null ? null : snapshot.files().size());
        return snapshot;
    }

    public static List<Snapshot> from(HttpClient client, Backup backup, int listLimit) throws IOException {
        logger.trace("<< from() < dsPrsID: {} udid: {} listLimit: {}",
                backup.dsPrsID(), backup.backupUDID(), listLimit);

        List<Snapshot> snapshots = new ArrayList<>();

        for (ICloud.MBSSnapshot mbsSnapshot : backup.backup().getSnapshotList()) {
            final List<ICloud.MBSFile> files;

            if (mbsSnapshot.getCommitted() == 0) {
                logger.debug("-- from() > incomplete: {}", mbsSnapshot);
                files = new ArrayList<>();

            } else {
                files = snapshotClient.files(
                        client,
                        backup.dsPrsID(),
                        backup.mmeAuthToken(),
                        backup.mobileBackupUrl(),
                        backup.backupUDID(),
                        mbsSnapshot.getSnapshotID(),
                        listLimit);
            }

            Snapshot snapshot = new Snapshot(backup, mbsSnapshot, files);
            snapshots.add(snapshot);
        }

        logger.debug(marker, "-- from() > snapshots: {}", snapshots);
        logger.trace(">> from() > snapshots: {}", snapshots.size());
        return snapshots;
    }

    private static final Logger logger = LoggerFactory.getLogger(Snapshots.class);
    private static final Marker marker = MarkerFactory.getMarker(Markers.CLIENT);

    private static final SnapshotClient snapshotClient = SnapshotClient.create();
}
