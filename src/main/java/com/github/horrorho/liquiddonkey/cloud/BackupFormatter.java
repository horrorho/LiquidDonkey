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
import java.util.Locale;
import java.util.function.Function;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * BackupFormatter.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class BackupFormatter implements Function<ICloud.MBSBackup, String> {

    public static BackupFormatter create() {
        return create("\t", DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    public static BackupFormatter create(String indent, DateTimeFormatter dateTimeFormatter) {

        if (dateTimeFormatter.getLocale() == null) {
            dateTimeFormatter = dateTimeFormatter.withLocale(Locale.getDefault());
        }

        if (dateTimeFormatter.getZone() == null) {
            dateTimeFormatter = dateTimeFormatter.withZone(ZoneId.systemDefault());
        }

        return new BackupFormatter(indent, dateTimeFormatter);
    }

    private final String indent;
    private final DateTimeFormatter dateTimeFormatter;

    BackupFormatter(String indent, DateTimeFormatter dateTimeFormatter) {
        this.indent = indent;
        this.dateTimeFormatter = dateTimeFormatter;
    }

    @Override
    public String apply(ICloud.MBSBackup backup) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        ICloud.MBSBackupAttributes attributes = backup.getAttributes();

        writer.println(indent + "Device:\t" + attributes.getMarketingName() + " " + attributes.getHardwareModel()
                + " (" + attributes.getProductType() + ")");
        writer.println(indent + "SN:\t" + attributes.getSerialNumber());
        writer.println(indent + "UDID:\t" + Bytes.hex(backup.getBackupUDID()));
        writer.println(indent + "Size:\t" + Bytes.humanize(backup.getQuotaUsed()));

        backup.getSnapshotList().stream().forEach(snapshot -> {
            ICloud.MBSSnapshotAttributes attr = snapshot.getAttributes();

            String incomplete = snapshot.getCommitted() == 0 ? "Incomplete" : "";
            String lastModifiedStr = dateTimeFormatter.format(Instant.ofEpochSecond(snapshot.getLastModified()));
            String size = Bytes.humanize(snapshot.getQuotaReserved());

            writer.println(
                    indent
                    + String.format("%4s", snapshot.getSnapshotID()) + ":\t"
                    + attr.getDeviceName() + " " + attr.getProductVersion() + "  "
                    + String.format("%8s", size) + "  "
                    + lastModifiedStr
                    + incomplete);
        });
        return stringWriter.toString();
    }
}
