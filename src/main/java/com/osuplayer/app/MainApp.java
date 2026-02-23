package com.osuplayer.app;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;
import java.util.Optional;

import com.osuplayer.config.ConfigManager;
import com.osuplayer.dependencies.VlcDependencyLoader;
import com.osuplayer.discord.DiscordRichPresence;
import com.osuplayer.lang.LanguageManager;
import com.osuplayer.lang.LanguageSelectionDialog;
import com.osuplayer.playback.MusicManager;
import com.osuplayer.ui.UIController;

import javafx.application.Application;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class MainApp extends Application {
    private static MediaPlayerFactory factory;
    private static EmbeddedMediaPlayer audioPlayer;
    private static EmbeddedMediaPlayer videoPlayer;
    private static DiscordRichPresence discord;

    private ConfigManager configManager;

    @Override
    public void start(Stage primaryStage) {
        configManager = new ConfigManager();

        initializeLanguage(primaryStage);

        UIController ui = new UIController(audioPlayer,
                videoPlayer,
                configManager,
                new MusicManager(configManager),
                discord);

        ui.start(primaryStage);

        primaryStage.setOnCloseRequest(event -> {
            ui.shutdown();
            if (discord != null) discord.stop();
            if (audioPlayer != null) audioPlayer.release();
            if (videoPlayer != null) videoPlayer.release();
            if (factory != null) factory.release();
            System.exit(0);
        });
    }

    private void initializeLanguage(Stage owner) {
        LanguageManager languageManager = LanguageManager.getInstance();
        String languageId = configManager.getLanguage();
        if (languageId == null || languageId.isBlank()) {
            LanguageSelectionDialog dialog = new LanguageSelectionDialog(owner, configManager.getTheme());
            languageId = dialog.showAndWait();
            if (languageId == null || languageId.isBlank()) languageId = "es";
            configManager.setLanguage(languageId);
        }
        languageManager.setLanguage(languageId);
        if (discord != null) discord.setIdleStatus();
    }

    public static void main(String[] args) {
        VlcDependencyLoader.GLOBAL.loadOrThrow();

        factory = tryCreateFactory();

        if (factory == null) {
            throw new RuntimeException("Failed to initialize VLC media player factory");
        }

        audioPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
        videoPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

        if (shouldEnableDiscord()) {
            discord = new DiscordRichPresence();
            discord.start(1418149866168909846L);
        } else {
            discord = null;
        }

        launch(args);
    }

    private static MediaPlayerFactory tryCreateFactory() {
        try {
            return new MediaPlayerFactory("--input-title-format=Osulux");
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean shouldEnableDiscord() {
        String env = Optional.ofNullable(System.getenv("OSULUX_DISCORD"))
            .orElse(Optional.ofNullable(System.getProperty("osulux.discord")).orElse(""));

        if (!env.isBlank()) {
            String v = env.trim().toLowerCase(Locale.ROOT);
            if (v.equals("1") || v.equals("true") || v.equals("on")) {
                return isDiscordIpcAvailable();
            }
            return false;
        }

        return isDiscordIpcAvailable();
    }

    private static boolean isDiscordIpcAvailable() {
        for (int i = 0; i < 10; i++) {
            String pipe = "\\\\.\\pipe\\discord-ipc-" + i;
            try (RandomAccessFile raf = new RandomAccessFile(pipe, "rw")) {
                return true;
            } catch (IOException ignored) {
            }
        }
        return false;
    }
}
