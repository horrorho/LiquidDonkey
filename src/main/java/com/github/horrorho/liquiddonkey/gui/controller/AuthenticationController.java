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
 * values copies or substantial portions from the Software.
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

import com.github.horrorho.liquiddonkey.cloud.Account;
import com.github.horrorho.liquiddonkey.cloud.client.Authentication;
import com.github.horrorho.liquiddonkey.cloud.Backup;
import com.github.horrorho.liquiddonkey.exception.FatalException;
import com.github.horrorho.liquiddonkey.gui.controller.data.BackupProperties;
import com.github.horrorho.liquiddonkey.http.HttpFactory;
import com.github.horrorho.liquiddonkey.printer.Printer;
import static com.github.horrorho.liquiddonkey.settings.Markers.GUI;
import com.github.horrorho.liquiddonkey.settings.props.Parsers;
import com.github.horrorho.liquiddonkey.settings.Property;
import com.github.horrorho.liquiddonkey.settings.config.AuthenticationConfig;
import com.github.horrorho.liquiddonkey.settings.config.HttpConfig;
import com.github.horrorho.liquiddonkey.settings.props.Props;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
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

    private final SimpleObjectProperty<Task<?>> httpTask = new SimpleObjectProperty<>();
    private final long debounceMs = 2500;
    private final String go = "Go";
    private final String cancel = "Cancel";

    private ExecutorService executorService;
    private Props<Property> props;
    private Parsers parsers;
    private Stage stage;

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
    private CheckBox isPersistent;
    @FXML
    private CheckBox isAggressive;
    @FXML
    private CheckBox toRelaxSSL;
    @FXML
    private CheckBox toCombine;
    @FXML
    private CheckBox toForce;
    @FXML
    private ChoiceBox<Integer> threads;

    public void init(
            Stage stage,
            ExecutorService executorService,
            Props<Property> props,
            Parsers parsers) {

        this.stage = stage;
        this.executorService = executorService;
        this.props = props;
        this.parsers = parsers;

        httpTask.setValue(null);

        initButton(isPersistent, Property.ENGINE_PERSISTENT);
        initButton(isAggressive, Property.ENGINE_AGGRESSIVE);
        initButton(toRelaxSSL, Property.HTTP_RELAX_SSL);
        initButton(toCombine, Property.FILE_COMBINED);
        initButton(toForce, Property.FILE_FORCE);
        initThreads();
        disableButtons();

        stage.setOnCloseRequest(windowEvent -> {
            if (httpTask.get() != null) {
                httpTask.get().cancel();
            }
        });

        if (props.contains(Property.AUTHENTICATION_TOKEN)) {
            authToken.setText(props.get(Property.AUTHENTICATION_TOKEN));
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        accordion.setExpandedPane(appleIdPasswordPane);

    }

    @FXML
    private void handleAppleId(ActionEvent event) {
        password.requestFocus();
    }

    @FXML
    private void handlePassword(ActionEvent event) {
        goAppleIdPassword.requestFocus();
    }

    @FXML
    private void handleAuthToken(ActionEvent event) {
        goAuthToken.requestFocus();
    }

    @FXML
    private void handleReset(Event event) {
        resetButton(isPersistent, Property.ENGINE_PERSISTENT);
        resetButton(isAggressive, Property.ENGINE_AGGRESSIVE);
        resetButton(toRelaxSSL, Property.HTTP_RELAX_SSL);
        resetButton(toCombine, Property.FILE_COMBINED);
        resetButton(toForce, Property.FILE_FORCE);
        resetThreads();
    }

    @FXML
    private void handleGoAppleIdPassword(Event event) {
        if (httpTask.get() == null) {
            authenticate(AuthenticationConfig.fromAppleIdPassword(appleId.getText(), password.getText()));
        } else {
            cancel();
        }
    }

    @FXML
    private void handleGoAuthToken(Event event) {
        if (httpTask.get() == null) {
            try {
                authenticate(AuthenticationConfig.fromAuthorizationToken(authToken.getText()));
                // toSelection(Authentication.newInstance(null, "test@apple.com", "king lear"));
            } catch (IllegalArgumentException ex) {
                bad("Authentication error.", ex);
            }
        } else {
            cancel();
        }
    }

    void authenticate(AuthenticationConfig authenticationConfig) {
        logger.trace(GUI, "<< authenticate()");
        HttpTaskFactory<Authentication> httpTaskFactory = HttpTaskFactory.<Authentication>from(
                HttpFactory.from(HttpConfig.newInstance(props)),
                Printer.instanceOf(false));

        httpTask.setValue(httpTaskFactory.newInstance(
                http -> Authentication.from(http, authenticationConfig),
                t -> {
                    debounceGoButtons();
                    switch (t.getState()) {
                        case CANCELLED:
                            logger.debug("-- authentication() > cancelled");
                            httpTask.setValue(null);
                            break;
                        case FAILED:
                            bad("Authentication error.", t.getException());
                            httpTask.setValue(null);
                            break;
                        case SUCCEEDED: {
                            try {
                                toSelection(t.get());
                            } catch (InterruptedException | ExecutionException ex) {
                                logger.warn("-- authenticate() > exception: ", ex);
                            }
                        }
                    }
                }));

        executorService.submit(httpTask.getValue());
        debounceGoButtons();
        logger.trace(GUI, ">> authenticate()");
    }

//    void backups(Authentication authentication) {
//        logger.trace(GUI, "<< authentication()");
//        HttpTaskFactory<List<Backup>> httpTaskFactory = HttpTaskFactory.<List<Backup>>from(
//                HttpFactory.from(HttpConfig.newInstance(props)),
//                Printer.instanceOf(false));
//
//        httpTask.setValue(httpTaskFactory.newInstance(
//                http -> Authentication.from(http, authenticationConfig),
//                t -> {
//                    debounceGoButtons();
//                    httpTask.setValue(null);
//                    switch (t.getState()) {
//                        case FAILED:
//                            bad("Authentication error.", t.getException());
//                            break;
//                        case SUCCEEDED: {
//                            try {
//                                toSelection(t.get());
//                            } catch (InterruptedException | ExecutionException ex) {
//                                logger.warn("-- authenticate() > exception: ", ex);
//                            }
//                        }
//                    }
//                }));
//        executorService.submit(httpTask.getValue());
//
//        logger.trace(GUI, ">> authentication()");
//
//    }

    void cancel() {
        logger.trace("<< cancel()");
        httpTask.getValue().cancel();
        logger.trace(">> cancel()");
    }

    void bad(String text, Throwable throwable) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Error");
        alert.setHeaderText(text);
        alert.setContentText(throwable.getLocalizedMessage());
        alert.showAndWait();
    }

    void toSelection(Authentication authentication) {
        try {
            logger.trace(GUI, "<< toSelection()");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Selection.fxml"));
            Parent root = loader.load();
            SelectionController controller = loader.<SelectionController>getController();
            controller.init(authentication, null, stage, executorService, props, parsers);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            throw new FatalException("Bad fxml resource", ex);
        } finally {
            logger.trace(GUI, ">> toSelection()");
        }
    }

    void initButton(CheckBox checkbox, Property property) {
        boolean state = props.contains(property)
                ? props.get(property, parsers::asBoolean)
                : false;

        checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            props.put(property, newValue);
        });

        checkbox.setSelected(state);
    }

    void resetButton(CheckBox checkbox, Property property) {
        boolean state = props.parent().contains(property)
                ? props.parent().get(property, parsers::asBoolean)
                : false;
        checkbox.setSelected(state);
    }

    void initThreads() {
        List<Integer> values = props.getList(Property.GUI_SELECTION_THREADS, parsers::asInteger);

        int value = props.contains(Property.ENGINE_THREAD_COUNT)
                ? props.get(Property.ENGINE_THREAD_COUNT, parsers::asInteger)
                : 2;

        threads.getItems().addAll(0, values);
        threads.valueProperty().addListener((observable, oldValue, newValue) -> {
            props.put(Property.ENGINE_THREAD_COUNT, newValue);
        });
        setThreads(value);
    }

    void resetThreads() {
        int value = props.contains(Property.ENGINE_THREAD_COUNT)
                ? props.get(Property.ENGINE_THREAD_COUNT, parsers::asInteger)
                : 2;
        setThreads(value);
    }

    void setThreads(int value) {
        ObservableList<Integer> list = threads.getItems();
        // Add value to the list if required.
        if (!list.contains(value)) {
            list.add(value);
            Collections.sort(list);
        }
        threads.setValue(value);
    }

    private void disableButtons() {
        goAppleIdPassword.setDisable(true);
        goAuthToken.setDisable(true);
        httpTask.addListener(this::morphGoAppleIdPassword);
        httpTask.addListener(this::morphGoAuthToken);
        appleId.textProperty().addListener(this::morphGoAppleIdPassword);
        password.textProperty().addListener(this::morphGoAppleIdPassword);
        authToken.textProperty().addListener(this::morphGoAuthToken);
    }

    private <T> void debounceGoButtons() {
        executorService.submit(Debouncer.newInstance(debounceMs, goAppleIdPassword, goAuthToken));
    }

    private <T> void morphGoAppleIdPassword(ObservableValue<? extends T> observable, T oldValue, T newValue) {
        goAppleIdPassword.setDisable(httpTask.get() != null || appleId.getText().isEmpty() || password.getText().isEmpty());
        goAppleIdPassword.setText(httpTask.get() == null ? go : cancel);
    }

    private <T> void morphGoAuthToken(ObservableValue<? extends T> observable, T oldValue, T newValue) {
        goAuthToken.setDisable(httpTask.get() != null || authToken.getText().isEmpty());
    }

    void disable(TextField textField, ChangeListener<String> listener) {
        textField.textProperty().addListener(listener);
    }
}
