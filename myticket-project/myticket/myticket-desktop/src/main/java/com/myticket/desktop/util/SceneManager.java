package com.myticket.desktop.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Centralized scene-switch manager.  Holds a reference to the primary stage
 * and swaps the scene root whenever navigateTo() is called.
 */
public class SceneManager {

    private static final SceneManager INSTANCE = new SceneManager();
    private Stage primaryStage;

    private SceneManager() {}

    public static SceneManager getInstance() {
        return INSTANCE;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Navigate to a new FXML view (no data).
     */
    public void navigateTo(String fxmlPath) {
        navigateTo(fxmlPath, null);
    }

    /**
     * Navigate to a new FXML view and pass data to its controller.
     * If the controller implements {@link DataReceiver}, initData(data) is called
     * AFTER JavaFX's initialize() lifecycle completes.
     */
    public void navigateTo(String fxmlPath, Object data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (data != null) {
                Object controller = loader.getController();
                if (controller instanceof DataReceiver) {
                    ((DataReceiver) controller).initData(data);
                }
            }

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, 800, 600);
                scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Failed to load FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
