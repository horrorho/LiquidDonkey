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
package com.github.horrorho.liquiddonkey.gui.controller;

import com.github.horrorho.liquiddonkey.cloud.Authentication;
import com.github.horrorho.liquiddonkey.exception.AuthenticationException;
import com.github.horrorho.liquiddonkey.exception.FatalException;
import com.github.horrorho.liquiddonkey.http.Http;
import com.github.horrorho.liquiddonkey.http.HttpFactory;
import com.github.horrorho.liquiddonkey.printer.Printer; 
import com.github.horrorho.liquiddonkey.settings.Props;
import com.github.horrorho.liquiddonkey.settings.PropsFactory;
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
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
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

    private final Props configuration = PropsFactory.getInstance().fromPropertyDefaults();

    void warnOnEmpty(TextField textField) {

        textField.textProperty().addListener((observable, oldValue, newValue)
                -> textField.pseudoClassStateChanged(error, textField.getText().isEmpty())
        );
    }

    @FXML
    private Accordion accordion;

    @FXML
    private TitledPane appleIdPasswordPane;

    @FXML
    private TitledPane authTokenPane;

    @FXML
    private TitledPane advancedPane;

    @FXML
    private TextField appleId;

    @FXML
    private TextField password;

    @FXML
    private TextField authToken;

    @FXML
    private Button goAppleIdPassword;

    @FXML
    private Button goAuthToken;

    @FXML
    private void handleAppleId(ActionEvent event) {
        if (!appleId.getText().isEmpty()) {
            if (password.getText().isEmpty()) {
                password.requestFocus();
            } else {
                goAppleIdPassword.requestFocus();
            }
        }
    }

    @FXML
    private void handlePassword(ActionEvent event) {
        if (!password.getText().isEmpty()) {
            if (appleId.getText().isEmpty()) {
                appleId.requestFocus();
            } else {
                goAppleIdPassword.requestFocus();
            }
        }
    }

    @FXML
    private void handleAuthToken(ActionEvent event) {
        if (!authToken.getText().isEmpty()) {
            goAuthToken.requestFocus();
        }
    }

    @FXML
    private void handleGoAppleIdPassword(Event event) {
        appleId.pseudoClassStateChanged(error, appleId.getText().isEmpty());
        password.pseudoClassStateChanged(error, password.getText().isEmpty());

        if (appleId.getText().isEmpty()) {
            appleId.requestFocus();
        } else if (password.getText().isEmpty()) {
            password.requestFocus();
        } else {
            //authenticate(AuthenticationConfig.fromAppleIdPassword(appleId.getText(), password.getText()));
            toSelection(Authentication.newInstance(null, "test@apple.com", "jon snow"));
        }

    }

    @FXML
    private void handleGoAuthToken(Event event) {
        authToken.pseudoClassStateChanged(error, authToken.getText().isEmpty());

        if (authToken.getText().isEmpty()) {
            authToken.requestFocus();
        } else {
            authenticate(AuthenticationConfig.fromAuthorizationToken(authToken.getText()));
            // toSelection(Authentication.newInstance(null, "test@apple.com", "king lear"));
        }
    }

    void authenticate(AuthenticationConfig authenticationConfig) {

        try {
            Http http = HttpFactory.newInstance(
                    HttpConfig.newInstance(configuration),
                    Printer.instanceOf(false));

            toSelection(Authentication.from(http, authenticationConfig));

        } catch (AuthenticationException ex) {
            logger.warn("-- authenticate() > exception: ", ex);
            badAuthentication(ex);
        }
    }

    void badAuthentication(AuthenticationException ex) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Error");
        alert.setHeaderText("Unable to authenticate.");
        alert.setContentText(ex.getLocalizedMessage());
        alert.showAndWait();
    }

    void toSelection(Authentication authentication) {
        Stage stage = (Stage) goAppleIdPassword.getScene().getWindow();
        Parent root;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Selection.fxml"));
            root = loader.load();
            SelectionController controller = loader.<SelectionController>getController();
            controller.initData(authentication);
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

        accordion.setExpandedPane(appleIdPasswordPane);

    }
}
