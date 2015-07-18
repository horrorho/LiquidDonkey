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
package com.github.horrorho.liquiddonkey.cloud;

import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Snapshot.
 * <p>
 * Container {@link ICloud.MBSFile}.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Snapshot {

    /**
     * Returns a new instance with a filtered file list.
     *
     * @param snapshot, not null
     * @param predicate, not null
     * @return new instance, not null
     */
    public static Snapshot from(Snapshot snapshot, Predicate<ICloud.MBSFile> predicate) {
        logger.trace("--from() < snapshot: {}", snapshot);

        Snapshot filtered = new Snapshot(
                snapshot.snapshot(),
                snapshot.backup(),
                snapshot.files().stream().filter(predicate::test).collect(Collectors.toSet()));

        logger.trace("--from() > filter: {}", filtered);
        return filtered;
    }

    /**
     * Queries the Client and returns a new instance.
     *
     * @param client, not null
     * @param backup, not null
     * @param id, negative for relative offset from the latest snapshot
     * @return new instance, may be null
     * @throws IOException
     */
    public static Snapshot from(Client client, Backup backup, int id) throws IOException {
        logger.trace("<< from() < id: {}", id);

        int resolved = id < 0 ? backup.latestSnapshotId() + id + 1 : id + backup.firstSnapshotId() - 1;

        ICloud.MBSSnapshot snapshot = backup.snapshots().stream()
                .filter(s -> s.getSnapshotID() == resolved)
                .findFirst().orElse(null);

        Snapshot instance;
        if (snapshot != null) {
            try {
                instance = new Snapshot(snapshot, backup, client.files(backup.udid(), resolved));
                id++;
            } catch (HttpResponseException ex) {
                if (ex.getStatusCode() == 401) {
                    throw ex;
                }
                logger.warn("-- from() > exception: ", ex);
                instance = null;
            }
        } else {
            logger.warn("-- from() > not such snapshot: {}", resolved);
            instance = null;
        }

        logger.trace(">> from() > snapshot: {}", snapshot);
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(Snapshot.class);

    private final ICloud.MBSSnapshot snapshot;
    private final Backup backup;
    private final Set<ICloud.MBSFile> files;

    public Snapshot(ICloud.MBSSnapshot snapshot, Backup backup, Collection<ICloud.MBSFile> files) {
        this.snapshot = Objects.requireNonNull(snapshot);
        this.backup = Objects.requireNonNull(backup);
        this.files = new HashSet<>(files);
    }

    public ICloud.MBSSnapshot snapshot() {
        return snapshot;
    }

    public Backup backup() {
        return backup;
    }

    public Set<ICloud.MBSFile> files() {
        return new HashSet<>(files);
    }

    public int id() {
        return snapshot.getSnapshotID();
    }

    public boolean isComplete() {
        return snapshot.getQuotaReserved() != 0;
    }

    public long lastModified() {
        return snapshot.getLastModified();
    }

    public ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures() {
        return files().stream()
                .collect(Collectors.groupingByConcurrent(ICloud.MBSFile::getSignature, Collectors.toSet()));
    }

    public ByteString keybagUuid() {
        return snapshot.getAttributes().getKeybagUUID();
    }

    @Override
    public String toString() {
        return "Snapshot{"
                + "snapshot=" + snapshot
                + ", backup=" + backup.udidString()
                + ", files count=" + files.size() + '}';
    }
}
