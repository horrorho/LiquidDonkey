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
package com.github.horrorho.liquiddonkey.gui.controller;

import com.github.horrorho.liquiddonkey.cloud.Authentication;
import com.github.horrorho.liquiddonkey.cloud.client.Client;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.FatalException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.HttpFactory;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig;
import com.github.horrorho.liquiddonkey.settings.config.HttpConfig;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author Ahseya
 */
public class AuthenticationController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    private final static PseudoClass error = PseudoClass.getPseudoClass("error");

    static void warnOnEmpty(TextField textField) {

        textField.textProperty().addListener((observable, oldValue, newValue)
                -> textField.pseudoClassStateChanged(error, textField.getText().isEmpty())
        );
    }

    @FXML
    private TextField appleId;

    @FXML
    private TextField password;

    @FXML
    private Button go;

    @FXML
    private void handleAppleId(ActionEvent event) {
        if (!appleId.getText().isEmpty()) {
            if (password.getText().isEmpty()) {
                password.requestFocus();
            } else {
                go.requestFocus();
            }
        }
    }

    @FXML
    private void handlePassword(ActionEvent event) {
        if (!password.getText().isEmpty()) {
            if (appleId.getText().isEmpty()) {
                appleId.requestFocus();
            } else {
                go.requestFocus();
            }
        }
    }

    @FXML
    private void handleGoButton(Event event) {

        appleId.pseudoClassStateChanged(error, appleId.getText().isEmpty());
        password.pseudoClassStateChanged(error, password.getText().isEmpty());

        if (appleId.getText().isEmpty()) {
            appleId.requestFocus();
        } else if (password.getText().isEmpty()) {
            password.requestFocus();
        } else {
            //authenticate();
            toSelection(null);
        }
    }

    void authenticate() {
        try {
            Http http = HttpFactory.newInstance(
                    HttpConfig.newInstance(true, false),
                    Printer.instanceOf(false));

            Authentication.of(
                    http,
                    AuthenticationConfig.newInstance(appleId.getText(), password.getText()));

        } catch (AuthenticationException ex) {
            logger.warn("-- authenticate() > exception: ", ex);
            badAuthentication(ex);
        }
    }

    void badAuthentication(AuthenticationException ex) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Unable to authenticate.");
        alert.setContentText(ex.getLocalizedMessage());
        alert.showAndWait();
    }

    void toSelection(Client client) {
        Stage stage = (Stage) go.getScene().getWindow();
        Parent root;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Selection.fxml"));
            root = loader.load();
            SelectionController controller = loader.<SelectionController>getController();
            controller.initData(client);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException ex) {
            throw new FatalException("Bad fxml resource", ex);
        }
    }

    /**
     * Initializes the controller class.
     *
     * @param url not used
     * @param rb not used
     */
    @Override
    public void initialize(URL url, ResourceBundle rb
    ) {
        warnOnEmpty(appleId);
        warnOnEmpty(password);
    }
}
