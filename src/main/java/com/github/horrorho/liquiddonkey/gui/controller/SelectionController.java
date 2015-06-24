package com.github.horrorho.liquiddonkey.gui.controller;

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
import com.github.horrorho.liquiddonkey.cloud.Account;
import com.github.horrorho.liquiddonkey.cloud.Authentication;
import com.github.horrorho.liquiddonkey.gui.controller.data.BackupProperties;
import com.github.horrorho.liquiddonkey.printer.Printer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
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
    private Accordion accordion;

    @FXML
    private TitledPane main;

    @FXML
    private TitledPane filters;

    @FXML
    private Button downloadButton;

    @FXML
    private CheckBox checkAll;

    @FXML
    private TableView<BackupProperties> tableView;

    @FXML
    private void checkAllHandler(ActionEvent event) {
        backups.stream()
                .forEach(property -> property.checkedProperty().setValue(checkAll.isSelected()));
    }

    @FXML
    private void handleDownloadButtonAction(ActionEvent event) {

        backups.stream().forEach(System.out::println);
    }

    private void downloadButtonEnabledHandler() {
        downloadButton.setDisable(
                backups.stream()
                .map(BackupProperties::checkedProperty)
                .noneMatch(BooleanProperty::get));
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        backups.addListener((ListChangeListener.Change<? extends BackupProperties> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().stream().map(BackupProperties::checkedProperty)
                            .forEach(property -> property.addListener(
                                            (observable, oldValue, newValue) -> downloadButtonEnabledHandler()));
                }
            }
        });

        accordion.expandedPaneProperty().addListener((property, oldPane, newPane) -> {
            if (newPane == null) {
                accordion.setExpandedPane(oldPane == main ? filters : main);
            }
        });
        accordion.setExpandedPane(main);

        tableView.setPlaceholder(new Text("No backups."));
        tableView.setItems(backups);

    }

    public void initData(Authentication authentication) {
        this.authentication = authentication;
        
        Account account = Account.newInstance(authentication.client(), Printer.instanceOf(false));
        account.backups().stream()
                .map(BackupProperties::newInstance).forEach(backups::add);
        
        downloadButtonEnabledHandler();
        checkAll.setSelected(false);

        main.setText(authentication.fullName() + " - " + authentication.appleId());
    }
}
