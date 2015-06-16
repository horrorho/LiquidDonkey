/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.horrorho.liquiddonkey.gui.controller.data;

import com.github.horrorho.liquiddonkey.cloud.Backup;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

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
                + "Size: " + backup.size() + " (Snapshot/s: " + snapshotsString + ")" +"\n";

        return newInstance(
                false,
                backup.hardwareModel(),
                info,
                backup.lastModified());
    }

    public static BackupProperties newInstance(boolean checked, String device, String info, String updated) {
        return new BackupProperties(checked, device, info, updated);
    }

    private final SimpleBooleanProperty checked;
    private final SimpleStringProperty device;
    private final SimpleStringProperty info;
    private final SimpleStringProperty updated;

    private BackupProperties(boolean checked, String device, String info, String updated) {
        this.checked = new SimpleBooleanProperty(checked);
        this.device = new SimpleStringProperty(device);
        this.info = new SimpleStringProperty(info);
        this.updated = new SimpleStringProperty(updated);
    }

    public SimpleBooleanProperty checkedProperty() {
        return checked;
    }

    public SimpleStringProperty deviceProperty() {
        return device;
    }

    public SimpleStringProperty infoProperty() {
        return info;
    }

    public SimpleStringProperty updatedProperty() {
        return updated;
    }
}
