package com.osuplayer;

import javafx.application.Application;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class MainApp extends Application {

    private MediaPlayerFactory factory;
    private EmbeddedMediaPlayer audioPlayer;
    private EmbeddedMediaPlayer videoPlayer;
    private ConfigManager configManager;
    private MusicManager musicManager;

    @Override
    public void start(Stage primaryStage) {
        factory = new MediaPlayerFactory("--input-title-format=Osulux");
        audioPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
        videoPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

        configManager = new ConfigManager();
        musicManager = new MusicManager();

        UIController ui = new UIController(audioPlayer, videoPlayer, configManager, musicManager);
        ui.start(primaryStage);

        primaryStage.setOnCloseRequest(event -> {
            if (audioPlayer != null) {
                audioPlayer.controls().stop();
                audioPlayer.release();
            }
            if (videoPlayer != null) {
                videoPlayer.controls().stop();
                videoPlayer.release();
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
