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
import com.github.horrorho.liquiddonkey.settings.config.EngineConfig;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
    public static Snapshot of(Snapshot snapshot, Predicate<ICloud.MBSFile> predicate) {
        logger.trace("--of() < snapshot: {}", snapshot);

        Snapshot filtered = new Snapshot(
                snapshot.id(),
                snapshot.backup(),
                snapshot.files().stream().filter(predicate::test).collect(Collectors.toSet()));

        logger.trace("--of() > filter: {}", filtered);
        return filtered;
    }

    /**
     * Queries the Client and returns a new instance.
     *
     * @param client, not null
     * @param backup, not null
     * @param id
     * @param config, not null
     * @return new instance, may be null
     * @throws IOException
     */
    public static Snapshot of(Client client, Backup backup, int id, EngineConfig config) throws IOException {
        logger.trace("<< of() < id: {}", id);
        int latest = backup.snapshots().stream().mapToInt(Integer::intValue).max().orElse(0);

        Snapshot snapshot = of(
                client,
                backup,
                id < 0 ? latest + id + 1 : id,
                config.isAggressive());

        logger.trace(">> of() > snapshot: {}", snapshot);
        return snapshot;
    }

    static Snapshot of(Client client, Backup backup, int id, boolean toHunt) throws IOException {
        List<Integer> snapshots = backup.snapshots();

        if (!snapshots.contains(id)) {
            logger.warn("-- of() > no snapshot: {}", id);
            return null;
        }

        int to = toHunt && id == 1 && snapshots.size() > 1
                ? snapshots.get(1)
                : id + 1;

        return snapshot(client, backup, id, to);
    }

    static Snapshot snapshot(Client client, Backup backup, int from, int to) throws IOException {
        int id = from;
        Snapshot snapshot = null;

        // The initial snapshot isn't always at 1.
        while (id < to && snapshot == null) {
            try {
                snapshot = new Snapshot(id, backup, client.files(backup.udid(), id));
                id++;
            } catch (HttpResponseException ex) {
                if (ex.getStatusCode() == 401) {
                    throw ex;
                }
                logger.warn("-- snapshot() > exception: ", ex);
            }
        }
        return snapshot;
    }

    private static final Logger logger = LoggerFactory.getLogger(Snapshot.class);

    private final int id;
    private final Backup backup;
    private final Set<ICloud.MBSFile> files;

    Snapshot(int id, Backup backup, Collection<ICloud.MBSFile> files) {
        this.id = id;
        this.backup = Objects.requireNonNull(backup);
        this.files = new HashSet<>(files);
    }

    public int id() {
        return id;
    }

    public Backup backup() {
        return backup;
    }

    public Set<ICloud.MBSFile> files() {
        return new HashSet<>(files);
    }

    public ConcurrentMap<ByteString, Set<ICloud.MBSFile>> signatures() {
        // ConcurrentMap, non-concurrent Set
        return files().stream()
                .collect(Collectors.groupingByConcurrent(ICloud.MBSFile::getSignature, Collectors.toSet()));
    }

    @Override
    public String toString() {
        return "Snapshot{"
                + "id=" + id
                + ", files=" + files.size()
                + '}';
    }
}
