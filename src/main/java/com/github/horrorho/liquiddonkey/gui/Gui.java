package com.github.horrorho.liquiddonkey.gui;

import com.github.horrorho.liquiddonkey.gui.controller.AuthenticationController;
import com.github.horrorho.liquiddonkey.settings.Property;
import com.github.horrorho.liquiddonkey.settings.Props;
import com.github.horrorho.liquiddonkey.settings.PropsFactory;
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

    GuiProps guiProps;
    Stage stage;

    @Override
    public void start(Stage stage) throws Exception {
        logger.trace("<< start()");
        this.stage = stage;

        guiProps = GuiProps.newInstance();

        System.out.println(guiProps.props().distinct());
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Authentication.fxml"));
        Parent root = loader.load();
        AuthenticationController controller = loader.<AuthenticationController>getController();
        controller.initData(guiProps);
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
        if (guiProps != null) {
            guiProps.close();
        }
        logger.trace(">> stop()");
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
        logger.trace("<< main() < args: {}", Arrays.asList(args));
        launch(args);
        logger.trace(">> main()");
    }
}
