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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backup.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class Backup {

    private static final String NA = "N/A";
    private static final String INDENT = "\t";
    private static final DateTimeFormatter dateTimeFormatter
            = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());

    /**
     * Returns a new instance.
     *
     * @param backup the underlying backup, not null
     * @return a new instance, not null
     * @throws NullPointerException if the backup argument is null
     */
    public static Backup newInstance(ICloud.MBSBackup backup) {

        String size = Bytes.humanize(backup.getQuotaUsed());

        String hardwareModel;
        String marketingName;
        String serialNumber;

        if (backup.hasAttributes()) {
            ICloud.MBSBackupAttributes attributes = backup.getAttributes();
            hardwareModel = attributes.getHardwareModel();
            marketingName = attributes.getMarketingName();
            serialNumber = attributes.getSerialNumber();
        } else {
            hardwareModel = NA;
            marketingName = NA;
            serialNumber = NA;
        }

        String lastModified = backup.hasSnapshot() && backup.getSnapshot().hasLastModified()
                ? dateTimeFormatter.format(Instant.ofEpochSecond(backup.getSnapshot().getLastModified()))
                : NA;

        String deviceName;
        String productVerson;
        if (backup.hasSnapshot() && backup.getSnapshot().hasAttributes()) {
            ICloud.MBSSnapshotAttributes snapshotAttributes = backup.getSnapshot().getAttributes();
            deviceName = snapshotAttributes.getDeviceName();
            productVerson = snapshotAttributes.getProductVersion();
        } else {
            deviceName = NA;
            productVerson = NA;
        }

        return new Backup(
                backup,
                availableSnapshots(latestSnapshot(backup)),
                size,
                hardwareModel,
                marketingName,
                serialNumber,
                deviceName,
                lastModified,
                productVerson,
                Bytes.hex(backup.getBackupUDID()));
    }

    public static Backup newInstance(
            ICloud.MBSBackup backup,
            List<Integer> snapshots,
            String size,
            String hardwareModel,
            String marketingName,
            String serialNumber,
            String deviceName,
            String lastModified,
            String productVerson,
            String udid) {

        return new Backup(
                backup,
                snapshots,
                size,
                hardwareModel,
                marketingName,
                serialNumber,
                deviceName,
                lastModified,
                productVerson,
                udid);
    }

    static int latestSnapshot(ICloud.MBSBackup backup) {
        if (!backup.hasSnapshot() || !backup.getSnapshot().hasSnapshotID()) {
            return 0;
        }

        ICloud.MBSSnapshot snapshot = backup.getSnapshot();
        if (!snapshot.hasCommitted() || snapshot.getCommitted() == 0) {
            logger.debug("-- latestSnapshot() > the latest snapshot is incomplete: {}", snapshot.getSnapshotID());
            return snapshot.getSnapshotID() - 1;
        }
        return snapshot.getSnapshotID();
    }

    static List<Integer> availableSnapshots(int latestSnapshot) {
        // Unaware of iCloud api call to achieve this.
        // Instead we assume that the snapshots exit at index 1, latest_index -1, latest_index (not always true)
        return latestSnapshot == 0
                ? new ArrayList<>()
                : IntStream.of(1, latestSnapshot - 1, latestSnapshot)
                .filter(id -> id > 0)
                .distinct()
                .sorted()
                .mapToObj(Integer::valueOf)
                .collect(Collectors.toList());
    }

    private static final Logger logger = LoggerFactory.getLogger(Backup.class);

    public static Logger getLogger() {
        return logger;
    }

    private final ICloud.MBSBackup backup;
    private final List<Integer> snapshots;

    private final String size;
    private final String hardwareModel;
    private final String marketingName;
    private final String serialNumber;
    private final String deviceName;
    private final String lastModified;
    private final String productVerson;
    private final String udid;

    Backup(
            ICloud.MBSBackup backup,
            List<Integer> snapshots,
            String size,
            String hardwareModel,
            String marketingName,
            String serialNumber,
            String deviceName,
            String lastModified,
            String productVerson,
            String udid) {

        this.backup = Objects.requireNonNull(backup);
        this.snapshots = new ArrayList<>(Objects.requireNonNull(snapshots));
        this.size = size;
        this.hardwareModel = hardwareModel;
        this.marketingName = marketingName;
        this.serialNumber = serialNumber;
        this.deviceName = deviceName;
        this.lastModified = lastModified;
        this.productVerson = productVerson;
        this.udid = udid;
    }

    public List<Integer> snapshots() {
        return new ArrayList<>(snapshots);
    }

    public ICloud.MBSBackup backup() {
        return backup;
    }

    public ByteString udid() {
        return backup.getBackupUDID();
    }

    public String udidString() {
        return udid;
    }

    public String format() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter print = new PrintWriter(stringWriter);

        String snapshotsString = snapshots.isEmpty()
                ? "none or incomplete"
                : snapshots.stream().map(Object::toString).collect(Collectors.joining(" "));

        print.println(INDENT + "Name:\t" + deviceName);
        print.println(INDENT + "Device:\t" + marketingName + " " + hardwareModel);
        print.println(INDENT + "SN:\t" + serialNumber);
        print.println(INDENT + "UDID:\t" + udidString());
        print.println(INDENT + "iOS:\t" + productVerson);
        print.println(INDENT + "Size:\t" + size + " (Snapshot/s: " + snapshotsString + ")");
        print.println(INDENT + "Last:\t" + lastModified);

        return stringWriter.toString();
    }

    public String size() {
        return size;
    }

    public String hardwareModel() {
        return hardwareModel;
    }

    public String marketingName() {
        return marketingName;
    }

    public String serialNumber() {
        return serialNumber;
    }

    public String deviceName() {
        return deviceName;
    }

    public String lastModified() {
        return lastModified;
    }

    public String productVerson() {
        return productVerson;
    }

    @Override
    public String toString() {
        return "Backup{" + "backup=" + backup + ", snapshots=" + snapshots + ", size=" + size + ", hardwareModel="
                + hardwareModel + ", marketingName=" + marketingName + ", serialNumber=" + serialNumber
                + ", deviceName=" + deviceName + ", lastModified=" + lastModified + ", productVerson="
                + productVerson + ", udid=" + udid + '}';
    }
}
