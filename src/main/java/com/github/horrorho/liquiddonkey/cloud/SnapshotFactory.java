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
package com.github.horrorho.liquiddonkey.cloud;

import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.FatalException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.settings.config.SnapshotFactoryConfig;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SnapshotFactory.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class SnapshotFactory {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotFactory.class);

    public static SnapshotFactory newInstance(
            Backup backup,
            Collection<Integer> snapshots,
            SnapshotFactoryConfig config) {

        return new SnapshotFactory(
                backup.udid(),
                backup.snapshots(),
                SnapshotFilter.newInstance(backup, snapshots),
                config.toHuntFirstSnapshot());
    }

    static SnapshotFactory newInstance(
            ByteString udid,
            Collection<Integer> snapshots,
            Predicate<Integer> snapshotFilter,
            boolean hunt) {

        return new SnapshotFactory(udid, snapshots, snapshotFilter, hunt);
    }

    private final ByteString udid;
    private final List<Integer> snapshots;
    private final Predicate<Integer> snapshotFilter;
    private final boolean hunt;

    SnapshotFactory(
            ByteString udid,
            Collection<Integer> snapshots,
            Predicate<Integer> snapshotFilter,
            boolean hunt) {

        this.udid = Objects.requireNonNull(udid);
        this.snapshots = new ArrayList<>(new HashSet<>(snapshots));
        this.snapshotFilter = Objects.requireNonNull(snapshotFilter);
        this.hunt = hunt;

        Collections.sort(this.snapshots);
    }

    public Snapshot of(Http http, Client client, int id) {
        try {
            logger.trace("<< id() < id: {}", id);
            Snapshot snapshot = snapshot(http, client, id);
            logger.trace(">> id() > snapshot: {}", snapshot);
            return snapshot;
        } catch (IOException ex) {
            throw new FatalException(ex);
        }
    }

    Snapshot snapshot(Http http, Client client, int id) throws IOException {
        if (!snapshots.contains(id)) {
            logger.warn("-- of() > bad id: {}", id);
            return null;
        }

        if (!snapshotFilter.test(id)) {
            logger.debug("-- of() > filtered: {}", id);
            return null;
        }

        int to = hunt && id == 1 && snapshots.size() > 1
                ? snapshots.get(1)
                : id + 1;

        List<ICloud.MBSFile> files = files(http, client, id, to);

        return files == null
                ? null
                : Snapshot.newInstance(id, files);
    }

    List<ICloud.MBSFile> files(Http http, Client client, int from, int to) throws IOException {
        int snapshot = from;
        List<ICloud.MBSFile> files = null;

        while (snapshot < to && files == null) {
            files = files(http, client, snapshot++);
        }
        return files;
    }

    List<ICloud.MBSFile> files(Http http, Client client, int snapshot) throws IOException {
        try {
            return client.listFiles(http, udid, snapshot);
        } catch (HttpResponseException ex) {

            if (ex.getStatusCode() == 401) {
                throw new AuthenticationException(ex);
            }

            logger.trace("-- files() > snapshot not found: {}", snapshot);
            return null;
        }
    }
}
// TODO set ordering