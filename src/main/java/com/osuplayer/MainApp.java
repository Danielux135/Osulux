package com.osuplayer;

import javafx.application.Application;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Crear MediaPlayer una sola vez y pasarlo al UIController
        MediaPlayerFactory factory = new MediaPlayerFactory();
        MediaPlayer mediaPlayer = factory.mediaPlayers().newMediaPlayer();

        UIController ui = new UIController(mediaPlayer);
        ui.start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
