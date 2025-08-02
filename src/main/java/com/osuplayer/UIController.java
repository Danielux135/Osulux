package com.osuplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
import javafx.scene.control.ListCell;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.State;

public class UIController {

    private final ListView<String> songListView = new ListView<>();
    private final Slider progressSlider = new Slider();
    private final Label timeLabel = new Label("00:00 / 00:00");
    private final Slider volumeSlider = new Slider(0, 100, 50);
    private FilteredList<String> filteredSongList;
    private final ObservableList<String> masterSongList = FXCollections.observableArrayList();

    private final MediaPlayer mediaPlayer;

    private boolean isSeeking = false;
    private boolean shuffleEnabled = false;
    private boolean showOnlyFavorites = false;
    private final Random random = new Random();

    private final MusicManager musicManager;
    private final ConfigManager configManager;

    private final Label currentSongLabel = new Label("Sin canción");
    private final ImageView coverImageView = new ImageView();

    private final Stack<String> playHistory = new Stack<>();

    private Button shuffleButton;
    private Button playPauseButton;

    private final Set<String> favoritos = new HashSet<>();
    private final Button favoriteButton = new Button("♡");
    private final Button favoritesFilterButton = new Button("Mostrar favoritos");

    public UIController(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;

        this.musicManager = new MusicManager();
        this.configManager = new ConfigManager();

        // Carga favoritos desde config (puede ser List o Set)
        favoritos.addAll(new HashSet<>(configManager.getFavorites()));

        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                if (!isSeeking) {
                    Platform.runLater(() -> {
                        double seconds = newTime / 1000.0;
                        progressSlider.setValue(seconds);
                        updateTimeLabel(seconds, mediaPlayer.status().length() / 1000.0);
                    });
                }
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> playNextSong());
            }
        });

        favoriteButton.setStyle("-fx-font-size: 24;");
        favoriteButton.setFocusTraversable(false);
        favoriteButton.setOnAction(e -> toggleFavorito());

        favoritesFilterButton.setFocusTraversable(false);
        favoritesFilterButton.setOnAction(e -> toggleFavoritesFilter());
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("OSU! Music Player");
        volumeSlider.setValue(configManager.getVolume() * 100);

        Button chooseFolderButton = new Button("Seleccionar carpeta de canciones");
        TextField searchField = new TextField();
        searchField.setPromptText("Buscar canciones o artistas...");

        Button previousButton = createControlButton("⏮");
        playPauseButton = createControlButton("▶");
        Button stopButton = createControlButton("⏹");
        Button nextButton = createControlButton("⏭");
        shuffleButton = createControlButton("🔀");
        updateShuffleButtonStyle();
        shuffleButton.setOnAction(e -> toggleShuffle());

        chooseFolderButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Selecciona la carpeta de canciones de OSU!");
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                musicManager.setLastFolderPath(selectedDirectory.getAbsolutePath());
                configManager.setLastFolder(selectedDirectory.getAbsolutePath());
                loadSongs(selectedDirectory);
            }
        });

        previousButton.setOnAction(e -> playPreviousSong());

        playPauseButton.setOnAction(e -> {
            State state = mediaPlayer.status().state();
            if (state == State.PLAYING) {
                mediaPlayer.controls().pause();
                playPauseButton.setText("▶");
            } else if (state == State.PAUSED || state == State.STOPPED) {
                if (!playHistory.isEmpty()) {
                    mediaPlayer.controls().play();
                } else {
                    playSelectedSong();
                }
                playPauseButton.setText("⏸");
            } else {
                playSelectedSong();
                playPauseButton.setText("⏸");
            }
        });

        stopButton.setOnAction(e -> {
            State state = mediaPlayer.status().state();
            if (state != State.STOPPED) {
                mediaPlayer.controls().stop();
                playPauseButton.setText("▶");
            }
        });

        nextButton.setOnAction(e -> playNextSong());

        songListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                playSelectedSong();
                playPauseButton.setText("⏸");
            }
        });

        // Context menu favoritos
        songListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);

                if (empty || item == null) {
                    setContextMenu(null);
                } else {
                    MenuItem toggleFavoriteItem = new MenuItem();
                    toggleFavoriteItem.setText(favoritos.contains(item) ? "Eliminar de favoritos" : "Agregar a favoritos");
                    toggleFavoriteItem.setOnAction(e -> {
                        if (favoritos.contains(item)) favoritos.remove(item);
                        else favoritos.add(item);

                        updateFavoriteButton(currentSongLabel.getText());
                        configManager.saveFavorites(new ArrayList<>(favoritos));
                        updateFilters(null);
                        songListView.refresh();
                    });
                    setContextMenu(new ContextMenu(toggleFavoriteItem));
                }
            }
        });

        progressSlider.setMin(0);
        progressSlider.setMax(100);
        progressSlider.setValue(0);

        progressSlider.setOnMousePressed(e -> isSeeking = true);
        progressSlider.setOnMouseReleased(e -> {
            State state = mediaPlayer.status().state();
            if (state == State.PLAYING || state == State.PAUSED) {
                mediaPlayer.controls().setTime((long) (progressSlider.getValue() * 1000));
            }
            isSeeking = false;
        });

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            mediaPlayer.audio().setVolume(newVal.intValue());
            configManager.saveConfig(newVal.doubleValue() / 100.0);
        });

        coverImageView.setFitWidth(150);
        coverImageView.setFitHeight(150);
        coverImageView.setPreserveRatio(true);

        currentSongLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        currentSongLabel.setMaxWidth(150);
        currentSongLabel.setWrapText(true);

        VBox coverBox = new VBox(5, coverImageView, currentSongLabel, favoriteButton);
        coverBox.setAlignment(Pos.CENTER);
        coverBox.setPrefWidth(170);

        HBox controlBox = new HBox(10, previousButton, playPauseButton, stopButton, nextButton, shuffleButton, volumeSlider);
        controlBox.setAlignment(Pos.CENTER);

        HBox progressBox = new HBox(10, progressSlider, timeLabel);
        progressBox.setAlignment(Pos.CENTER);

        HBox topBox = new HBox(10, chooseFolderButton, searchField, favoritesFilterButton);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        songListView.setPrefWidth(400);
        root.setCenter(songListView);

        VBox bottomControls = new VBox(5, controlBox, progressBox);
        bottomControls.setAlignment(Pos.CENTER);
        root.setBottom(bottomControls);
        root.setRight(coverBox);

        Scene scene = new Scene(root, 850, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

        filteredSongList = new FilteredList<>(masterSongList, s -> true);
        songListView.setItems(filteredSongList);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> updateFilters(newValue));

        // Cargar última carpeta
        String lastFolder = configManager.getLastFolder();
        if (lastFolder != null && !lastFolder.isEmpty()) {
            File folder = new File(lastFolder);
            if (folder.exists() && folder.isDirectory()) loadSongs(folder);
        }

        // Cargar última canción
        String lastSong = configManager.getCurrentSong();
        if (lastSong != null && !lastSong.isEmpty() && musicManager.getSongPath(lastSong) != null) {
            songListView.getSelectionModel().select(lastSong);
            currentSongLabel.setText(lastSong);
            updateFavoriteButton(lastSong);
            updateCoverImage(musicManager.getSongPath(lastSong));
        }
    }

    private void updateFilters(String searchText) {
        String lowerFilter = searchText == null ? "" : searchText.toLowerCase();
        filteredSongList.setPredicate(item -> {
            boolean matchesSearch = item != null && item.toLowerCase().contains(lowerFilter);
            boolean matchesFavorites = !showOnlyFavorites || favoritos.contains(item);
            return matchesSearch && matchesFavorites;
        });
    }

    private void toggleFavoritesFilter() {
        showOnlyFavorites = !showOnlyFavorites;
        favoritesFilterButton.setText(showOnlyFavorites ? "Mostrar todo" : "Mostrar favoritos");
        updateFilters(null);
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
        shuffleButton.setStyle(shuffleEnabled ? "-fx-background-color: #00cc00; -fx-text-fill: white;" : "");
    }

    private void loadSongs(File folder) {
        masterSongList.clear();
        Map<String, String> loadedSongs = musicManager.loadSongsFromFolder(folder);
        if (loadedSongs.isEmpty()) {
            masterSongList.add("No se encontraron canciones en esta carpeta");
        } else {
            masterSongList.addAll(loadedSongs.keySet());
        }
        updateFilters(null);
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

        mediaPlayer.controls().stop();
        mediaPlayer.media().startPaused(path);

        Platform.runLater(() -> {
            long length = mediaPlayer.status().length();
            if (length > 0) {
                progressSlider.setMax(length / 1000.0);
                mediaPlayer.controls().setTime((long) (startPosition * 1000));
                updateTimeLabel(startPosition, length / 1000.0);
                mediaPlayer.controls().play();
                configManager.saveCurrentSong(song, startPosition);
            }
        });

        currentSongLabel.setText(song);
        updateFavoriteButton(song);
        updateCoverImage(path);
    }

    private void toggleFavorito() {
        String cancion = currentSongLabel.getText();
        if (favoritos.contains(cancion)) favoritos.remove(cancion);
        else favoritos.add(cancion);

        updateFavoriteButton(cancion);
        configManager.saveFavorites(new ArrayList<>(favoritos));
        updateFilters(null);
        songListView.refresh();
    }

    private void updateFavoriteButton(String cancion) {
        favoriteButton.setText(favoritos.contains(cancion) ? "♥" : "♡");
    }

    private void updateCoverImage(String songPath) {
        File songFile = new File(songPath);
        File parentDir = songFile.getParentFile();
        if (parentDir != null) {
            File[] images = parentDir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
            coverImageView.setImage(images != null && images.length > 0
                    ? new Image(images[0].toURI().toString())
                    : null);
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
            String nextSong = (idx < songListView.getItems().size() - 1)
                    ? songListView.getItems().get(idx + 1)
                    : songListView.getItems().get(0);
            playHistory.push(nextSong);
            songListView.getSelectionModel().select(nextSong);
        }
        playSelectedSongFromHistory(songListView.getSelectionModel().getSelectedItem(), 0);
    }

    private void updateTimeLabel(double currentSeconds, double totalSeconds) {
        timeLabel.setText(formatTime(currentSeconds) + " / " + formatTime(totalSeconds));
        if (!playHistory.isEmpty()) {
            configManager.saveCurrentSong(playHistory.peek(), currentSeconds);
        }
    }

    private String formatTime(double seconds) {
        int minutes = (int) seconds / 60;
        int secs = (int) seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
}
