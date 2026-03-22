package com.myticket.desktop;

import com.myticket.desktop.util.ApiClient;
import com.myticket.desktop.util.SceneManager;
import com.myticket.desktop.util.SessionManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * MyTicket Desktop — JavaFX client entry point.
 * Loads the login screen (or restores session) and sets up the primary stage.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Wire up the SceneManager
        SceneManager.getInstance().setPrimaryStage(primaryStage);

        primaryStage.setTitle("MyTicket \u2014 Campus Ticketing");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // Try to restore a persisted session first
        String persistedToken = SessionManager.getInstance().loadPersistedToken();
        if (persistedToken != null) {
            // Validate the token in the background
            Task<Boolean> validateTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return ApiClient.getInstance().isTokenValid(persistedToken);
                }
            };

            validateTask.setOnSucceeded(event -> {
                if (validateTask.getValue()) {
                    // Token is valid — restore session and skip login
                    SessionManager.getInstance().setJwtToken(persistedToken);
                    // For now, go to login anyway (dashboard not yet built)
                    loadLoginScreen(primaryStage);
                } else {
                    // Token expired — show login
                    SessionManager.getInstance().clear();
                    loadLoginScreen(primaryStage);
                }
            });

            validateTask.setOnFailed(event -> {
                // Server unreachable or error — show login
                loadLoginScreen(primaryStage);
            });

            // Show a loading placeholder while validating
            loadLoginScreen(primaryStage);

            Thread thread = new Thread(validateTask);
            thread.setDaemon(true);
            thread.start();
        } else {
            // No persisted token — go straight to login
            loadLoginScreen(primaryStage);
        }
    }

    private void loadLoginScreen(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Failed to load login screen: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        // Cleanup on app close
        System.out.println("MyTicket Desktop shutting down...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
