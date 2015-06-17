package com.github.horrorho.liquiddonkey.gui.controller;

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
import com.github.horrorho.liquiddonkey.cloud.Authentication;
import com.github.horrorho.liquiddonkey.cloud.Backup;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.gui.controller.data.BackupProperties;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

/**
 * FXML Controller class
 *
 * @author Ahseya
 */
public class SelectionController implements Initializable {

    private final ObservableList<BackupProperties> backups = FXCollections.observableArrayList();
    private Authentication authentication;

    @FXML
    private Text text;

    @FXML
    private Button button;

    @FXML
    private TableView<BackupProperties> tableView;

    @FXML
    private void handleButtonAction(ActionEvent event) throws IOException {

        backups.stream().forEach(System.out::println);
    }

    private void buttonDisable() {
        button.setDisable(
                backups.stream()
                .map(BackupProperties::checkedProperty)
                .noneMatch(BooleanProperty::get));
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("INITIALIZE");

        backups.addListener((ListChangeListener.Change<? extends BackupProperties> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().stream().map(BackupProperties::checkedProperty)
                            .forEach(property -> property.addListener(
                                            (observable, oldValue, newValue) -> buttonDisable()));
                }
            }
        });

        for (int i = 0; i < 5; i++) {
            backups.add(BackupProperties.newInstance(Backup.newInstance(ICloud.MBSBackup.getDefaultInstance())));
        }
        tableView.setItems(backups);
        buttonDisable();

//        backups.stream().map(BackupProperties::checkedProperty)
//                .forEach(x -> x.addListener(
//                                (observable, oldValue, newValue)
//                                -> button.setDisable(
//                                        backups.stream()
//                                        .map(BackupProperties::checkedProperty)
//                                        .noneMatch(BooleanProperty::get))));
    }

    public void initData(Authentication authentication) {
        System.out.println("INITDATA");
        this.authentication = authentication;
        //text.setText(authentication.fullName() + " (" + authentication.client().dsPrsID() + ") - " + authentication.appleId());

              text.setText(authentication.fullName() + " (" +"123123123" + ") - " + authentication.appleId());
  
        
    }
}
