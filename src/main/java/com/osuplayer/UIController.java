package com.osuplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
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

    // Historial de canciones
    private Stack<String> playHistory = new Stack<>();

    // Botones
    private Button shuffleButton;
    private Button playPauseButton;
    private Button favoritesToggleButton;

    // Lista de favoritos
    private List<String> favorites = new ArrayList<>();
    private boolean showingFavorites = false;

    public UIController() {
        musicManager = new MusicManager();
        configManager = new ConfigManager();
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("OSU! Music Player");

        configManager.loadConfig();
        volumeSlider.setValue(configManager.getVolume());

        Button chooseFolderButton = new Button("Seleccionar carpeta");
        TextField searchField = new TextField();
        searchField.setPromptText("Buscar canciones...");

        Button previousButton = createControlButton("⏮");
        playPauseButton = createControlButton("▶");
        Button stopButton = createControlButton("⏹");
        Button nextButton = createControlButton("⏭");
        shuffleButton = createControlButton("🔀");

        // Botón favoritos (estrella)
        favoritesToggleButton = new Button("☆");
        favoritesToggleButton.setStyle("-fx-font-size: 18px;");
        favoritesToggleButton.setOnAction(e -> toggleFavoritesView());

        updateShuffleButtonStyle();
        shuffleButton.setOnAction(e -> toggleShuffle());

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
                playPauseButton.setText("▶");
                if (!playHistory.isEmpty()) {
                    String currentSong = playHistory.peek();
                    double position = mediaPlayer.getCurrentTime().toSeconds();
                    configManager.saveCurrentSong(currentSong, position);
                }
            }
        });

        nextButton.setOnAction(e -> playNextSong());

        songListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                playSelectedSong();
                playPauseButton.setText("⏸");
            }
        });

        // Menú contextual (click derecho)
        ContextMenu contextMenu = new ContextMenu();
        MenuItem addToFav = new MenuItem("Añadir a favoritos");
        MenuItem removeFromFav = new MenuItem("Eliminar de favoritos");
        contextMenu.getItems().addAll(addToFav, removeFromFav);

        addToFav.setOnAction(e -> {
            String selected = songListView.getSelectionModel().getSelectedItem();
            if (selected != null && !favorites.contains(selected)) {
                favorites.add(selected);
                configManager.saveFavorites(favorites);
            }
        });

        removeFromFav.setOnAction(e -> {
            String selected = songListView.getSelectionModel().getSelectedItem();
            if (selected != null && favorites.contains(selected)) {
                favorites.remove(selected);
                configManager.saveFavorites(favorites);
            }
        });

        songListView.setContextMenu(contextMenu);

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

        HBox topBox = new HBox(10, chooseFolderButton, searchField, favoritesToggleButton);
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

        favorites = configManager.getFavorites();

        if (musicManager.getLastFolderPath() != null) {
            File folder = new File(musicManager.getLastFolderPath());
            if (folder.exists() && folder.isDirectory()) loadSongs(folder);
        }

        String lastSong = configManager.getCurrentSong();
        double lastPosition = configManager.getCurrentPosition();
        if (lastSong != null && !lastSong.isEmpty() && musicManager.getSongPath(lastSong) != null) {
            playHistory.push(lastSong);
            playSelectedSongFromHistory(lastSong, lastPosition);
            playPauseButton.setText("⏸");
        }
    }

    private Button createControlButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(40);
        btn.setPrefHeight(30);
        btn.setFocusTraversable(false);
        return btn;
    }

    private void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;
        updateShuffleButtonStyle();
    }

    private void updateShuffleButtonStyle() {
        if (shuffleEnabled) {
            shuffleButton.setStyle("-fx-background-color: #00cc00; -fx-text-fill: white;");
        } else {
            shuffleButton.setStyle("");
        }
    }

    private void toggleFavoritesView() {
        if (showingFavorites) {
            filteredSongList.setPredicate(s -> true);
            favoritesToggleButton.setText("☆");
        } else {
            filteredSongList.setPredicate(s -> favorites.contains(s));
            favoritesToggleButton.setText("★");
        }
        showingFavorites = !showingFavorites;
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

        if (playHistory.isEmpty() || !playHistory.peek().equals(selected)) {
            playHistory.push(selected);
        }
        playSelectedSongFromHistory(selected, 0);
    }

    private void playSelectedSongFromHistory(String song, double startPosition) {
        if (song == null) return;

        String path = musicManager.getSongPath(song);
        if (path == null) return;

        if (mediaPlayer != null) mediaPlayer.stop();
        Media media = new Media(new File(path).toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(volumeSlider.getValue());

        currentSongLabel.setText(song);
        updateCoverImage(path);

        mediaPlayer.setOnReady(() -> {
            double totalSeconds = media.getDuration().toSeconds();
            progressSlider.setMax(totalSeconds);
            updateTimeLabel(startPosition, totalSeconds);
            mediaPlayer.seek(Duration.seconds(startPosition));
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
        if (playHistory.size() > 1) {
            playHistory.pop();
            String previousSong = playHistory.peek();
            songListView.getSelectionModel().select(previousSong);
            playSelectedSongFromHistory(previousSong, 0);
        }
    }

    private void playNextSong() {
        if (shuffleEnabled) {
            int randomIndex = random.nextInt(songListView.getItems().size());
            String randomSong = songListView.getItems().get(randomIndex);
            playHistory.push(randomSong);
            songListView.getSelectionModel().select(randomSong);
        } else {
            int idx = songListView.getSelectionModel().getSelectedIndex();
            if (idx < songListView.getItems().size() - 1) {
                String nextSong = songListView.getItems().get(idx + 1);
                playHistory.push(nextSong);
                songListView.getSelectionModel().select(nextSong);
            } else {
                String firstSong = songListView.getItems().get(0);
                playHistory.push(firstSong);
                songListView.getSelectionModel().select(firstSong);
            }
        }
        playSelectedSongFromHistory(songListView.getSelectionModel().getSelectedItem(), 0);
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
