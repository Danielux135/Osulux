package com.osuplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

    private String currentSong = null;
    private double savedPosition = 0;

    private final List<String> playHistory = new ArrayList<>();
    private int historyIndex = -1;

    // Tamaño fijo para botones
    private static final double BUTTON_WIDTH = 40;
    private static final double BUTTON_HEIGHT = 30;

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

        Button previousButton = createFixedSizeButton("⏮");
        Button playPauseButton = createFixedSizeButton("▶");
        Button stopButton = createFixedSizeButton("⏹");
        Button nextButton = createFixedSizeButton("⏭");
        Button shuffleButton = createFixedSizeButton("🔀");

        chooseFolderButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Selecciona la carpeta de canciones de OSU!");
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                musicManager.setLastFolderPath(selectedDirectory.getAbsolutePath());
                configManager.setLastFolder(selectedDirectory.getAbsolutePath());
                configManager.saveConfig(volumeSlider.getValue());
                loadSongs(selectedDirectory);
                clearHistory();
            }
        });

        previousButton.setOnAction(e -> playPreviousSong());

        playPauseButton.setOnAction(e -> {
            if (mediaPlayer == null) {
                playSelectedSong();
                playPauseButton.setText("⏸");
            } else {
                MediaPlayer.Status status = mediaPlayer.getStatus();
                if (status == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    playPauseButton.setText("▶");
                } else {
                    mediaPlayer.play();
                    playPauseButton.setText("⏸");
                }
            }
        });

        stopButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                savedPosition = 0;
                if (currentSong != null) {
                    configManager.saveCurrentSong(currentSong, savedPosition);
                }
                playPauseButton.setText("▶");
            }
        });

        nextButton.setOnAction(e -> playNextSong());
        shuffleButton.setOnAction(e -> toggleShuffle());

        songListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                playSelectedSong();
                playPauseButton.setText("⏸");
            }
        });

        progressSlider.setMin(0);
        progressSlider.setMax(1);
        progressSlider.setValue(0);

        progressSlider.setOnMousePressed(e -> isSeeking = true);
        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
                savedPosition = progressSlider.getValue();
            }
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

        primaryStage.setOnCloseRequest(event -> {
            if (currentSong != null) {
                double pos = 0;
                if (mediaPlayer != null) {
                    pos = mediaPlayer.getCurrentTime().toSeconds();
                }
                configManager.saveCurrentSong(currentSong, pos);
            }
        });

        primaryStage.show();

        filteredSongList = new FilteredList<>(masterSongList, s -> true);
        songListView.setItems(filteredSongList);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String lowerFilter = newValue.toLowerCase();
            filteredSongList.setPredicate(item -> item != null && item.toLowerCase().contains(lowerFilter));
        });

        if (musicManager.getLastFolderPath() != null) {
            File folder = new File(musicManager.getLastFolderPath());
            if (folder.exists() && folder.isDirectory()) {
                loadSongs(folder);

                String savedSong = configManager.getCurrentSong();
                savedPosition = configManager.getCurrentPosition();

                if (savedSong != null && !savedSong.isEmpty() && masterSongList.contains(savedSong)) {
                    currentSong = savedSong;
                    songListView.getSelectionModel().select(currentSong);
                    prepareSong(currentSong);
                }
            }
        }
    }

    private Button createFixedSizeButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(BUTTON_WIDTH);
        btn.setPrefHeight(BUTTON_HEIGHT);
        btn.setMinWidth(BUTTON_WIDTH);
        btn.setMinHeight(BUTTON_HEIGHT);
        btn.setMaxWidth(BUTTON_WIDTH);
        btn.setMaxHeight(BUTTON_HEIGHT);
        btn.setFocusTraversable(false);
        btn.setAlignment(Pos.CENTER);
        return btn;
    }

    private void clearHistory() {
        playHistory.clear();
        historyIndex = -1;
    }

    private void addToHistory(String song) {
        while (playHistory.size() > historyIndex + 1) {
            playHistory.remove(playHistory.size() - 1);
        }
        playHistory.add(song);
        historyIndex = playHistory.size() - 1;
    }

    private void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;
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

    private void prepareSong(String songName) {
        String path = musicManager.getSongPath(songName);
        if (path == null) return;

        if (mediaPlayer != null) mediaPlayer.stop();
        Media media = new Media(new File(path).toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(volumeSlider.getValue());

        currentSongLabel.setText(songName);
        updateCoverImage(path);

        mediaPlayer.setOnReady(() -> {
            double totalSeconds = media.getDuration().toSeconds();
            progressSlider.setMax(totalSeconds);
            updateTimeLabel(mediaPlayer.getCurrentTime().toSeconds(), totalSeconds);
            mediaPlayer.seek(Duration.seconds(savedPosition));
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
    }

    private void playSelectedSong() {
        String selected = songListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        currentSong = selected;
        savedPosition = 0;
        prepareSong(currentSong);
        mediaPlayer.play();

        addToHistory(currentSong);
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
        if (historyIndex > 0) {
            historyIndex--;
            String previousSong = playHistory.get(historyIndex);
            currentSong = previousSong;
            savedPosition = 0;
            songListView.getSelectionModel().select(previousSong);
            prepareSong(previousSong);
            mediaPlayer.play();
        }
    }

    private void playNextSong() {
        if (shuffleEnabled) {
            int randomIndex = random.nextInt(songListView.getItems().size());
            String randomSong = songListView.getItems().get(randomIndex);
            currentSong = randomSong;
            savedPosition = 0;
            songListView.getSelectionModel().select(randomSong);
            prepareSong(randomSong);
            mediaPlayer.play();

            addToHistory(randomSong);
        } else {
            int idx = songListView.getSelectionModel().getSelectedIndex();
            if (idx < songListView.getItems().size() - 1) {
                idx++;
            } else {
                idx = 0;
            }
            String nextSong = songListView.getItems().get(idx);
            currentSong = nextSong;
            savedPosition = 0;
            songListView.getSelectionModel().select(nextSong);
            prepareSong(nextSong);
            mediaPlayer.play();

            addToHistory(nextSong);
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
