/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.horrorho.liquiddonkey.gui.controller.data;

import com.github.horrorho.liquiddonkey.cloud.Backup;
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

    public static BackupProperties newInstance(Backup backup) {

        String snapshotsString = backup.snapshots().isEmpty()
                ? "none or incomplete"
                : backup.snapshots().stream().map(Object::toString).collect(Collectors.joining(" "));

        String info
                = backup.deviceName() + "\n"
                + "SN: " + backup.serialNumber() + "\n"
                + "UDID: " + backup.udidString() + "\n"
                + "iOS: " + backup.productVerson() + "\n"
                + "Size: " + backup.size() + " (Snapshot/s: " + snapshotsString + ")" + "\n";

        return newInstance(
                true,
                backup.hardwareModel(),
                info,
                backup.lastModified());
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
