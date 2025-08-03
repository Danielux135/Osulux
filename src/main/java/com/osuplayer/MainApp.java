package com.osuplayer;

import javafx.application.Application;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

public class MainApp extends Application {

    private MediaPlayerFactory factory;
    private MediaPlayer mediaPlayer;
    private ConfigManager configManager;
    private MusicManager musicManager;

    @Override
    public void start(Stage primaryStage) {
        // Crear MediaPlayer una sola vez y pasarlo al UIController
        factory = new MediaPlayerFactory();
        mediaPlayer = factory.mediaPlayers().newMediaPlayer();

        // Instanciar ConfigManager y MusicManager
        configManager = new ConfigManager();
        musicManager = new MusicManager();

        UIController ui = new UIController(mediaPlayer, configManager, musicManager);
        ui.start(primaryStage);

        // Cerrar mediaPlayer y factory al cerrar la ventana
        primaryStage.setOnCloseRequest(event -> {
            if (mediaPlayer != null) {
                mediaPlayer.controls().stop();
                mediaPlayer.release();
            }
            if (factory != null) {
                factory.release();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
