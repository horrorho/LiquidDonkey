package com.github.horrorho.liquiddonkey.gui;

import com.github.horrorho.liquiddonkey.gui.controller.AuthenticationController;
import com.github.horrorho.liquiddonkey.settings.Property;
import com.github.horrorho.liquiddonkey.settings.PropsManager;
import java.util.Arrays;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gui extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Gui.class);

    PropsManager propsManager;
    Stage stage;

    @Override
    public void start(Stage stage) throws Exception {
        logger.trace("<< start()");
        this.stage = stage;

        propsManager = PropsManager.from(Property.PROPERTIES_GUI_PATH);

        System.out.println(propsManager.props().get(Property.APP_NAME) + "\n\n\n\n\n");
        
        System.out.println(propsManager.props().distinct());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Authentication.fxml"));
        Parent root = loader.load();
        AuthenticationController controller = loader.<AuthenticationController>getController();
        controller.initData(propsManager.props());
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css"); // TODO is this problematic?
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
        logger.trace(">> start()");
    }

    @Override
    public void stop() throws Exception {
        logger.trace("<< stop()");
        if (propsManager != null) {
            propsManager.close();
        }
        logger.trace(">> stop()");
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application. main() serves only as fallback in case the
     * application can not be launched through deployment artifacts, e.g., in IDEs with limited FX support. NetBeans
     * ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        logger.trace("<< main() < args: {}", Arrays.asList(args));
        launch(args);
        logger.trace(">> main()");
    }
}
