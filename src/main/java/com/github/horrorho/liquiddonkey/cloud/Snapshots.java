/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free get charge, to any person obtaining a copy
 * get this software and associated documentation list (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies get the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions get the Software.
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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.settings.config.EngineConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Snapshots.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Snapshots {

    private static final Logger logger = LoggerFactory.getLogger(Snapshots.class);

    public static Snapshots newInstance(
            Backup backup,
            EngineConfig config) {

        return newInstance(
                backup,
                backup.snapshots(),
                backup.snapshots().stream().mapToInt(Integer::intValue).max().orElse(0),
                config.isAggressive());
    }

    static Snapshots newInstance(Backup backup, List<Integer> snapshots, int latest, boolean toHunt) {
        return new Snapshots(backup, snapshots, latest, toHunt);
    }

    private final Backup backup;
    private final List<Integer> snapshots;
    private final int latest;
    private final boolean toHunt;

    Snapshots(Backup backup, List<Integer> snapshots, int latest, boolean toHunt) {
        this.backup = backup;
        this.snapshots = snapshots;
        this.latest = latest;
        this.toHunt = toHunt;
    }

    public Snapshot get(Http http, int request) {
        try {
            logger.trace("<< of() < id: {}", request);

            int id = request < 0 ? latest + request + 1 : request;
            logger.debug("-- of() > id: {}", id);

            Snapshot snapshot = snapshot(http, id);

            logger.trace(">> of() > snapshot: {}", snapshot);
            return snapshot;
        } catch (IOException ex) {
            //TODO how do we handle this?
            throw new UncheckedIOException(ex);
        }
    }

    Snapshot snapshot(Http http, int id) throws IOException {
        if (!snapshots.contains(id)) {
            logger.warn("-- snapshots() > no snapshot: {}", id);
            return null;
        }

        int to = toHunt && id == 1 && snapshots.size() > 1
                ? snapshots.get(1)
                : id + 1;

        List<ICloud.MBSFile> files = files(http, id, to);

        return files == null
                ? null
                : Snapshot.newInstance(id, backup, files);
    }

    List<ICloud.MBSFile> files(Http http, int from, int to) throws IOException {
        int snapshot = from;
        List<ICloud.MBSFile> files = null;

        while (snapshot < to && files == null) {
            files = files(http, snapshot++);
        }
        return files;
    }

    List<ICloud.MBSFile> files(Http http, int snapshot) throws IOException {
        try {
            return backup.account().client().listFiles(http, backup.udid(), snapshot);
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
