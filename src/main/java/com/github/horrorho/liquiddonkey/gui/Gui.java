package com.github.horrorho.liquiddonkey.gui;

import com.github.horrorho.liquiddonkey.gui.controller.AuthenticationController;
import com.github.horrorho.liquiddonkey.settings.props.Parsers;
import com.github.horrorho.liquiddonkey.settings.Property;
import com.github.horrorho.liquiddonkey.settings.props.Props;
import com.github.horrorho.liquiddonkey.settings.props.PropsBuilder;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    Stage stage;

    @Override
    public void start(Stage stage) throws Exception {
        logger.trace("<< start()");
        this.stage = stage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Authentication.fxml"));
        Parent root = loader.load();
        AuthenticationController controller = loader.<AuthenticationController>getController();

        Props<Property> props = PropsBuilder.from(Property.class)
                .persistent(Gui.class)
                .parent(Property.props())
                .build();

        controller.init(
                stage,
                executorService,
                props,
                Parsers.newInstance(Property.dateTimeFormatter()));

        Scene scene = new Scene(root);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
        logger.trace(">> start()");
    }

    @Override
    public void stop() throws Exception {
        logger.trace("<< stop()");
        executorService.shutdownNow();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
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
