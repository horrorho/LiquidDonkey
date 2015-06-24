/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.horrorho.liquiddonkey.gui.controller.data;

import com.github.horrorho.liquiddonkey.cloud.Backup;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author Ahseya
 */
public class BackupProperties {

    private static final DateTimeFormatter defaultDateTimeFormatter
            = CellDateTimeFormatter.formatter().withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());

    public static BackupProperties newInstance(Backup backup) {
        return newInstance(backup, defaultDateTimeFormatter);
    }

    public static BackupProperties newInstance(Backup backup, DateTimeFormatter dateTimeFormatter) {

        String device = backup.marketingName() + "\n" + backup.hardwareModel();

        String snapshotsString = backup.snapshots().isEmpty()
                ? "none or incomplete"
                : backup.snapshots().stream().map(Object::toString).collect(Collectors.joining(" "));

        String info
                = backup.deviceName() + "\n"
                + "SN: " + backup.serialNumber() + "\n"
                + "UDID: " + backup.udidString() + "\n"
                + "iOS: " + backup.productVerson() + "\n"
                + "Size: " + backup.size() + " (Snapshot/s: " + snapshotsString + ")" + "\n";

        String lastModifiedStr = dateTimeFormatter.format(Instant.ofEpochSecond(backup.lastModified()));

        return newInstance(
                true,
                device,
                info,
                lastModifiedStr);
    }

    public static BackupProperties newInstance(boolean checked, String device, String info, String updated) {
        return new BackupProperties(checked, device, info, updated);
    }

    private final BooleanProperty checked;
    private final StringProperty device;
    private final StringProperty info;
    private final StringProperty updated;

    private BackupProperties(boolean checked, String device, String info, String updated) {
        this.checked = new SimpleBooleanProperty(checked);
        this.device = new SimpleStringProperty(device);
        this.info = new SimpleStringProperty(info);
        this.updated = new SimpleStringProperty(updated);
    }

    public BooleanProperty checkedProperty() {
        return checked;
    }

    public StringProperty deviceProperty() {
        return device;
    }

    public StringProperty infoProperty() {
        return info;
    }

    public StringProperty updatedProperty() {
        return updated;
    }

    @Override
    public String toString() {
        return "BackupProperties{" + "checked=" + checked + ", device=" + device + ", info=" + info
                + ", updated=" + updated + '}';
    }
}
