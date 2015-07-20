/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation files (the "Software"), to deal
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

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.util.Bytes;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Backup.
 * <p>
 * Describes {@link com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud.MBSBackup}.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class BackupFormatter implements Function<ICloud.MBSBackup, String> {

    public static BackupFormatter create() {
        return create("\t", "N/A", DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    public static BackupFormatter create(String indent, String na, DateTimeFormatter dateTimeFormatter) {

        if (dateTimeFormatter.getLocale() == null) {
            dateTimeFormatter = dateTimeFormatter.withLocale(Locale.getDefault());
        }

        if (dateTimeFormatter.getZone() == null) {
            dateTimeFormatter = dateTimeFormatter.withZone(ZoneId.systemDefault());
        }

        return new BackupFormatter(indent, na, dateTimeFormatter);
    }

    private final String indent;
    private final String na;
    private final DateTimeFormatter dateTimeFormatter;

    BackupFormatter(String indent, String na, DateTimeFormatter dateTimeFormatter) {
        this.indent = indent;
        this.na = na;
        this.dateTimeFormatter = dateTimeFormatter;
    }

    @Override
    public String apply(ICloud.MBSBackup backup) {

        String hardwareModel;
        String marketingName;
        String serialNumber;

        if (backup.hasAttributes()) {
            ICloud.MBSBackupAttributes attributes = backup.getAttributes();
            hardwareModel = attributes.getHardwareModel();
            marketingName = attributes.getMarketingName();
            serialNumber = attributes.getSerialNumber();
        } else {
            hardwareModel = na;
            marketingName = na;
            serialNumber = na;
        }

        List<ICloud.MBSSnapshot> snapshots = backup.getSnapshotList();

        long lastModified = 0;

        ICloud.MBSSnapshot latest = snapshots.isEmpty()
                ? null
                : snapshots.get(snapshots.size() - 1);

        String deviceName;
        String productVerson;
        if (latest == null) {
            deviceName = na;
            productVerson = na;
        } else {
            deviceName = latest.getAttributes().getDeviceName();
            productVerson = latest.getAttributes().getProductVersion();
        }

        String snapshotsString = snapshots.isEmpty()
                ? "none or incomplete"
                : snapshots.stream().map(ICloud.MBSSnapshot::getSnapshotID).map(Object::toString)
                .collect(Collectors.joining(" "));

        String size = Bytes.humanize(backup.getQuotaUsed());

        StringWriter stringWriter = new StringWriter();
        PrintWriter print = new PrintWriter(stringWriter);

        String lastModifiedStr = dateTimeFormatter.format(Instant.ofEpochSecond(lastModified));

        print.println(indent + "Name:\t" + deviceName);
        print.println(indent + "Device:\t" + marketingName + " " + hardwareModel);
        print.println(indent + "SN:\t" + serialNumber);
        print.println(indent + "UDID:\t" + Bytes.hex(backup.getBackupUDID()));
        print.println(indent + "iOS:\t" + productVerson);
        print.println(indent + "Size:\t" + size + " (Snapshot/s: " + snapshotsString + ")");
        print.println(indent + "Last:\t" + lastModifiedStr);

        return stringWriter.toString();
    }
}
