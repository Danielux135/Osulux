package com.osuplayer;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        UIController uiController = new UIController();
        uiController.start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
