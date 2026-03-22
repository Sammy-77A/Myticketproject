package com.myticket.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * MyTicket Desktop — JavaFX client entry point.
 * Full UI will be implemented in a later phase.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label placeholder = new Label("MyTicket Desktop — Coming Soon");
        placeholder.setStyle("-fx-font-size: 18px; -fx-text-fill: #555;");

        StackPane root = new StackPane(placeholder);
        root.setStyle("-fx-background-color: #f5f5f5;");

        Scene scene = new Scene(root, 600, 400);

        primaryStage.setTitle("MyTicket \u2014 Loading...");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
