package com.osuplayer;

import java.io.File;
import java.util.Map;
import java.util.Random;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private boolean shuffleEnabled = false;
    private final Random random = new Random();

    private MusicManager musicManager;
    private ConfigManager configManager;

    private Label currentSongLabel = new Label("Sin canción");
    private ImageView coverImageView = new ImageView();

    // Estado de reproducción
    private boolean isPlaying = false;
    private String lastSong = "";
    private double lastPosition = 0.0;

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

        Button previousButton = new Button("⏮");
        Button playPauseButton = new Button("▶");
        Button stopButton = new Button("⏹");
        Button nextButton = new Button("⏭");

        Button shuffleButton = new Button("🔀");
        shuffleButton.setPrefSize(40, 30);
        playPauseButton.setPrefSize(40, 30);
        previousButton.setPrefSize(40, 30);
        stopButton.setPrefSize(40, 30);
        nextButton.setPrefSize(40, 30);

        shuffleButton.setOnAction(e -> toggleShuffle());
        playPauseButton.setOnAction(e -> togglePlayPause(playPauseButton));
        stopButton.setOnAction(e -> stopSong());
        previousButton.setOnAction(e -> playPreviousSong());
        nextButton.setOnAction(e -> playNextSong());

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

        songListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                playSelectedSong();
            }
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

        coverImageView.setFitWidth(150);
        coverImageView.setFitHeight(150);
        coverImageView.setPreserveRatio(true);

        currentSongLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        currentSongLabel.setMaxWidth(150);
        currentSongLabel.setWrapText(true);

        VBox coverBox = new VBox(5, coverImageView, currentSongLabel);
        coverBox.setAlignment(Pos.CENTER);
        coverBox.setPrefWidth(170);

        HBox controlBox = new HBox(10, previousButton, playPauseButton, stopButton, nextButton, shuffleButton, volumeSlider);
        controlBox.setAlignment(Pos.CENTER);

        HBox progressBox = new HBox(10, progressSlider, timeLabel);
        progressBox.setAlignment(Pos.CENTER);

        HBox topBox = new HBox(10, chooseFolderButton, searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        songListView.setPrefWidth(400);
        root.setCenter(songListView);

        VBox bottomControls = new VBox(5, controlBox, progressBox);
        bottomControls.setAlignment(Pos.CENTER);
        root.setBottom(bottomControls);

        root.setRight(coverBox);

        Scene scene = new Scene(root, 800, 500);
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

        // Restaurar última canción y posición
        lastSong = configManager.getCurrentSong();
        lastPosition = configManager.getCurrentPosition();
        if (!lastSong.isEmpty()) {
            songListView.getSelectionModel().select(lastSong);
            playSelectedSong();
            if (mediaPlayer != null) mediaPlayer.seek(Duration.seconds(lastPosition));
        }
    }

    private void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;
    }

    private void togglePlayPause(Button button) {
        if (mediaPlayer == null) {
            playSelectedSong();
            button.setText("⏸");
            isPlaying = true;
        } else {
            if (isPlaying) {
                mediaPlayer.pause();
                button.setText("▶");
                isPlaying = false;
            } else {
                mediaPlayer.play();
                button.setText("⏸");
                isPlaying = true;
            }
        }
    }

    private void stopSong() {
        if (mediaPlayer != null) {
            configManager.saveCurrentSong(songListView.getSelectionModel().getSelectedItem(), mediaPlayer.getCurrentTime().toSeconds());
            mediaPlayer.stop();
            isPlaying = false;
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

        currentSongLabel.setText(selected);
        updateCoverImage(path);

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
        isPlaying = true;
    }

    private void updateCoverImage(String songPath) {
        File songFile = new File(songPath);
        File parentDir = songFile.getParentFile();
        if (parentDir != null) {
            File[] images = parentDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
            if (images != null && images.length > 0) {
                coverImageView.setImage(new Image(images[0].toURI().toString()));
            } else {
                coverImageView.setImage(null);
            }
        }
    }

    private void playPreviousSong() {
        int idx = songListView.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            songListView.getSelectionModel().select(idx - 1);
            playSelectedSong();
        }
    }

    private void playNextSong() {
        if (shuffleEnabled) {
            int randomIndex = random.nextInt(songListView.getItems().size());
            songListView.getSelectionModel().select(randomIndex);
        } else {
            int idx = songListView.getSelectionModel().getSelectedIndex();
            if (idx < songListView.getItems().size() - 1) {
                songListView.getSelectionModel().select(idx + 1);
            } else {
                songListView.getSelectionModel().select(0);
            }
        }
        playSelectedSong();
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
