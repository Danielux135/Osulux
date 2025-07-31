package com.osuplayer;

import java.io.File;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class UIController {

    private ListView<String> songListView = new ListView<>();
    private Slider progressSlider = new Slider();
    private Label timeLabel = new Label("00:00 / 00:00");
    private Slider volumeSlider = new Slider(0, 1, 0.5);
    private FilteredList<String> filteredSongList;
    private ObservableList<String> masterSongList = FXCollections.observableArrayList();

    private MediaPlayer mediaPlayer;
    private boolean isSeeking = false;

    private MusicManager musicManager;
    private ConfigManager configManager;

    public UIController() {
        musicManager = new MusicManager();
        configManager = new ConfigManager();
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("OSU! Music Player");

        configManager.loadConfig();
        volumeSlider.setValue(configManager.getVolume());

        Button chooseFolderButton = new Button("Seleccionar carpeta de canciones");
        TextField searchField = new TextField();
        searchField.setPromptText("Buscar canciones o artistas...");

        Button previousButton = new Button("Anterior");
        Button playButton = new Button("Play");
        Button pauseButton = new Button("Pause");
        Button stopButton = new Button("Stop");
        Button nextButton = new Button("Siguiente");

        chooseFolderButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Selecciona la carpeta de canciones de OSU!");
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                musicManager.setLastFolderPath(selectedDirectory.getAbsolutePath());
                configManager.setLastFolder(selectedDirectory.getAbsolutePath());
                configManager.saveConfig(volumeSlider.getValue());
                loadSongs(selectedDirectory);
            }
        });

        previousButton.setOnAction(e -> playPreviousSong());
        playButton.setOnAction(e -> playSelectedSong());
        pauseButton.setOnAction(e -> {
            if (mediaPlayer != null) mediaPlayer.pause();
        });
        stopButton.setOnAction(e -> {
            if (mediaPlayer != null) mediaPlayer.stop();
        });
        nextButton.setOnAction(e -> playNextSong());

        songListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) playSelectedSong();
        });

        progressSlider.setMin(0);
        progressSlider.setMax(1);
        progressSlider.setValue(0);

        progressSlider.setOnMousePressed(e -> isSeeking = true);
        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            isSeeking = false;
        });

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(newVal.doubleValue());
            configManager.saveConfig(newVal.doubleValue());
        });

        HBox controlBox = new HBox(10, previousButton, playButton, pauseButton, stopButton, nextButton, volumeSlider);
        HBox progressBox = new HBox(10, progressSlider, timeLabel);
        HBox topBox = new HBox(10, chooseFolderButton, searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(songListView);
        root.setBottom(new VBox(controlBox, progressBox));

        Scene scene = new Scene(root, 700, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        filteredSongList = new FilteredList<>(masterSongList, s -> true);
        songListView.setItems(filteredSongList);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String lowerFilter = newValue.toLowerCase();
            filteredSongList.setPredicate(item -> item != null && item.toLowerCase().contains(lowerFilter));
        });

        if (musicManager.getLastFolderPath() != null) {
            File folder = new File(musicManager.getLastFolderPath());
            if (folder.exists() && folder.isDirectory()) loadSongs(folder);
        }
    }

    private void loadSongs(File folder) {
        masterSongList.clear();
        Map<String, String> loadedSongs = musicManager.loadSongsFromFolder(folder);
        if (loadedSongs.isEmpty()) {
            masterSongList.add("No se encontraron canciones en esta carpeta");
        } else {
            masterSongList.addAll(loadedSongs.keySet());
        }
    }

    private void playSelectedSong() {
        String selected = songListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String path = musicManager.getSongPath(selected);
        if (path == null) return;

        if (mediaPlayer != null) mediaPlayer.stop();
        Media media = new Media(new File(path).toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(volumeSlider.getValue());

        mediaPlayer.setOnReady(() -> {
            double totalSeconds = media.getDuration().toSeconds();
            progressSlider.setMax(totalSeconds);
            updateTimeLabel(0, totalSeconds);
        });

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isSeeking) {
                double currentSeconds = newTime.toSeconds();
                double totalSeconds = mediaPlayer.getTotalDuration().toSeconds();
                Platform.runLater(() -> {
                    progressSlider.setValue(currentSeconds);
                    updateTimeLabel(currentSeconds, totalSeconds);
                });
            }
        });

        mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(this::playNextSong));
        mediaPlayer.play();
    }

    private void playPreviousSong() {
        int idx = songListView.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            songListView.getSelectionModel().select(idx - 1);
            playSelectedSong();
        }
    }

    private void playNextSong() {
        int idx = songListView.getSelectionModel().getSelectedIndex();
        if (idx < songListView.getItems().size() - 1) {
            songListView.getSelectionModel().select(idx + 1);
            playSelectedSong();
        }
    }

    private void updateTimeLabel(double currentSeconds, double totalSeconds) {
        String currentTime = formatTime(currentSeconds);
        String totalTime = formatTime(totalSeconds);
        timeLabel.setText(currentTime + " / " + totalTime);
    }

    private String formatTime(double seconds) {
        int minutes = (int) seconds / 60;
        int secs = (int) seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
}
