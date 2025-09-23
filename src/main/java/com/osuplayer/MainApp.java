package com.osuplayer;

import javafx.application.Application;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class MainApp extends Application {

    private static MediaPlayerFactory factory;
    private static EmbeddedMediaPlayer audioPlayer;
    private static EmbeddedMediaPlayer videoPlayer;

    private ConfigManager configManager;
    private MusicManager musicManager;

    @Override
    public void start(Stage primaryStage) {
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
        String os = System.getProperty("os.name").toLowerCase();
        String libPath = "lib";
        String pluginsPath = "lib/plugins";

        if (os.contains("win")) {
            System.setProperty("jna.library.path", libPath);
            System.setProperty("VLC_PLUGIN_PATH", pluginsPath);
        } else if (os.contains("nux") || os.contains("nix")) {
            System.setProperty("jna.library.path", libPath);
            System.setProperty("VLC_PLUGIN_PATH", pluginsPath);
        } else {
            System.err.println("OS no soportado: " + os);
        }

        factory = new MediaPlayerFactory("--input-title-format=Osulux");
        audioPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
        videoPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

        launch(args);
    }
}
