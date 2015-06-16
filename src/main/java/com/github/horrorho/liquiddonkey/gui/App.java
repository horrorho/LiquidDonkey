package com.github.horrorho.liquiddonkey.gui;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    Stage stage;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Authentication.fxml"));

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setResizable(false);
        stage.setScene(scene);

        stage.show();
    }

//    public void next(String fxml) throws IOException {
//        Parent root = FXMLLoader.load(getClass().getResource(fxml));
//        Scene scene = new Scene(root);
//        stage.setScene(scene);
//        stage.show();
//    }
    /**
     * The main() method is ignored in correctly deployed JavaFX application. main() serves only as fallback in case the
     * application can not be launched through deployment artifacts, e.g., in IDEs with limited FX support. NetBeans
     * ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
