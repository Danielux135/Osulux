package com.osuplayer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
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
    private final Random random = new Random();

    private final MusicManager musicManager;
    private final ConfigManager configManager;

    private final Label currentSongLabel = new Label("Sin canción");
    private final Stack<String> playHistory = new Stack<>();

    private Button shuffleButton;
    private Button playPauseButton;

    private final Set<String> favoritos = new HashSet<>();
    private final Button favoriteButton = new Button("♡");

    private final LinkedHashMap<String, List<String>> playlists = new LinkedHashMap<>();
    private final ListView<String> playlistListView = new ListView<>();
    private Button newPlaylistButton;

    private final ImageView coverImageView = new ImageView();
    private String currentPlaylist = "Todo";

    private TextField searchField;

    public UIController(MediaPlayer mediaPlayer, ConfigManager configManager, MusicManager musicManager) {
        this.mediaPlayer = mediaPlayer;
        this.musicManager = musicManager;
        this.configManager = configManager;

        favoritos.addAll(new HashSet<>(configManager.getFavorites()));

        initPlaylists();

        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                if (!isSeeking) {
                    Platform.runLater(() -> {
                        double seconds = newTime / 1000.0;
                        progressSlider.setValue(seconds);

                        long length = 0;
                        if (mediaPlayer.media().info() != null) {
                            length = mediaPlayer.media().info().duration();
                        }

                        updateTimeLabel(seconds, length / 1000.0);
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

        coverImageView.setFitWidth(170);
        coverImageView.setFitHeight(170);
        coverImageView.setPreserveRatio(true);
    }

    private void initPlaylists() {
        playlists.clear();
        Map<String, List<String>> loadedPlaylists = configManager.getPlaylists();
        playlists.put("Todo", new ArrayList<>());
        playlists.put("Favoritos", new ArrayList<>());

        for (Map.Entry<String, List<String>> entry : loadedPlaylists.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("Todo") && !key.equals("Favoritos")) {
                playlists.put(key, new ArrayList<>(entry.getValue()));
            }
        }

        List<String> allSongs = new ArrayList<>(musicManager.loadSongsFromFolder(new File(configManager.getLastFolder())).keySet());
        playlists.put("Todo", allSongs);

        List<String> favList = new ArrayList<>(favoritos);
        playlists.put("Favoritos", favList);

        updatePlaylistListViewItems();
    }

    private void updatePlaylistListViewItems() {
        playlistListView.getItems().clear();
        playlistListView.getItems().add("Todo");
        playlistListView.getItems().add("Favoritos");
        for (String key : playlists.keySet()) {
            if (!key.equals("Todo") && !key.equals("Favoritos")) {
                playlistListView.getItems().add(key);
            }
        }
        playlistListView.getSelectionModel().select(currentPlaylist);
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Osulux");

        volumeSlider.setValue(configManager.getVolume() * 100);
        mediaPlayer.audio().setVolume((int) volumeSlider.getValue());

        Button chooseFolderButton = new Button("Abrir carpeta Songs");

        searchField = new TextField();
        searchField.setPromptText("Buscar canciones o artistas...");
        searchField.setPrefWidth(600);

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

            String userHome = System.getProperty("user.home");
            File defaultDir = new File(userHome + File.separator + "AppData" + File.separator + "Local" + File.separator + "osu!" + File.separator + "Songs");

            if (defaultDir.exists() && defaultDir.isDirectory()) {
                directoryChooser.setInitialDirectory(defaultDir);
            }

            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                musicManager.setLastFolderPath(selectedDirectory.getAbsolutePath());
                configManager.setLastFolder(selectedDirectory.getAbsolutePath());
                loadSongs(selectedDirectory);
                updatePlaylistListViewItems();
                selectPlaylist("Todo");
            }
        });

        previousButton.setOnAction(e -> playPreviousSong());
        playPauseButton.setOnAction(e -> togglePlayPause());
        stopButton.setOnAction(e -> stopPlayback());
        nextButton.setOnAction(e -> playNextSong());

        songListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);

                    if (empty || item == null) {
                        setContextMenu(null);
                        return;
                    }

                    ContextMenu contextMenu = new ContextMenu();

                    MenuItem toggleFavoriteItem = new MenuItem(
                            favoritos.contains(item) ? "Eliminar de favoritos" : "Agregar a favoritos"
                    );
                    toggleFavoriteItem.setOnAction(e -> {
                        if (favoritos.contains(item)) favoritos.remove(item);
                        else favoritos.add(item);
                        playlists.put("Favoritos", new ArrayList<>(favoritos));
                        configManager.setFavorites(new ArrayList<>(favoritos));
                        updatePlaylistListViewItems();
                        if (currentPlaylist.equals("Favoritos")) loadPlaylistSongs("Favoritos");
                        updateFavoriteButton(currentSongLabel.getText());
                        songListView.refresh();
                    });
                    contextMenu.getItems().add(toggleFavoriteItem);

                    if (!playlists.isEmpty()) {
                        Menu addToPlaylistMenu = new Menu("Añadir a playlist");
                        for (String playlistName : playlists.keySet()) {
                            if (!playlistName.equals("Todo")) {
                                MenuItem playlistItem = new MenuItem(playlistName);
                                playlistItem.setOnAction(e -> {
                                    List<String> list = playlists.get(playlistName);
                                    if (list != null && !list.contains(item)) {
                                        list.add(item);
                                        configManager.setPlaylists(playlists);
                                        if (playlistName.equals(currentPlaylist)) loadPlaylistSongs(playlistName);
                                    }
                                });
                                addToPlaylistMenu.getItems().add(playlistItem);
                            }
                        }
                        contextMenu.getItems().add(addToPlaylistMenu);
                    }

                    MenuItem exportSongItem = new MenuItem("Exportar canción");
                    exportSongItem.setOnAction(e -> exportSong(item));
                    contextMenu.getItems().add(exportSongItem);

                    setContextMenu(contextMenu);
                }
            };
            return cell;
        });

        songListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                playSelectedSong();
                playPauseButton.setText("⏸");
            }
        });

        playlistListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    if (item != null && (item.equals("Todo") || item.equals("Favoritos"))) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            };

            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Eliminar playlist");
            deleteItem.setOnAction(e -> {
                String selected = cell.getItem();
                if (selected != null && !selected.equals("Todo") && !selected.equals("Favoritos")) {
                    playlists.remove(selected);
                    configManager.setPlaylists(playlists);
                    updatePlaylistListViewItems();
                    selectPlaylist("Todo");
                }
            });

            MenuItem exportAllItem = new MenuItem("Exportar todas las canciones");
            exportAllItem.setOnAction(e -> exportPlaylist(cell.getItem()));

            cell.itemProperty().addListener((obs, oldVal, newVal) -> {
                contextMenu.getItems().clear();
                if (newVal != null) {
                    contextMenu.getItems().add(exportAllItem);
                    if (!newVal.equals("Todo") && !newVal.equals("Favoritos")) {
                        contextMenu.getItems().add(deleteItem);
                    }
                    cell.setContextMenu(contextMenu);
                } else {
                    cell.setContextMenu(null);
                }
            });

            return cell;
        });
        playlistListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null && !n.equals(currentPlaylist)) selectPlaylist(n);
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
            configManager.setVolume(newVal.doubleValue() / 100.0);
        });

        currentSongLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        currentSongLabel.setMaxWidth(170);
        currentSongLabel.setWrapText(true);
        currentSongLabel.setAlignment(Pos.CENTER);

        VBox coverBox = new VBox(5, coverImageView, currentSongLabel, favoriteButton);
        coverBox.setAlignment(Pos.CENTER);
        coverBox.setPrefWidth(170);

        HBox controlBox = new HBox(10, previousButton, playPauseButton, stopButton, nextButton, shuffleButton, volumeSlider);
        controlBox.setAlignment(Pos.CENTER);

        HBox progressBox = new HBox(10, progressSlider, timeLabel);
        progressBox.setAlignment(Pos.CENTER);

        newPlaylistButton = new Button("Nueva Playlist");
        newPlaylistButton.setOnAction(e -> createNewPlaylist());

        VBox playlistBox = new VBox(5);
        playlistBox.setPrefWidth(170);
        playlistBox.getChildren().addAll(playlistListView);
        HBox newPlaylistButtonBox = new HBox(newPlaylistButton);
        newPlaylistButtonBox.setAlignment(Pos.CENTER);
        playlistBox.getChildren().add(newPlaylistButtonBox);

        HBox topLeftBox = new HBox(chooseFolderButton);
        topLeftBox.setAlignment(Pos.CENTER_LEFT);
        topLeftBox.setPrefWidth(170);

        HBox searchBox = new HBox(searchField);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setPrefWidth(600);

        HBox topBox = new HBox(10, topLeftBox, searchBox);
        topBox.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(topBox, new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(songListView);
        root.setRight(coverBox);
        root.setBottom(new VBox(5, controlBox, progressBox));
        root.setLeft(playlistBox);

        Scene scene = new Scene(root, 900, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

        filteredSongList = new FilteredList<>(masterSongList, s -> true);
        songListView.setItems(filteredSongList);
        searchField.textProperty().addListener((obs, o, n) ->
                filteredSongList.setPredicate(item -> item.toLowerCase().contains(n.toLowerCase()))
        );

        String lastFolder = configManager.getLastFolder();
        if (lastFolder != null && !lastFolder.isEmpty()) {
            File folder = new File(lastFolder);
            if (folder.exists() && folder.isDirectory()) {
                musicManager.setLastFolderPath(lastFolder);
                loadSongs(folder);
                updatePlaylistListViewItems();
                selectPlaylist("Todo");
            }
        }

        String lastSong = configManager.getLastSong();
        if (lastSong != null && !lastSong.isEmpty()) {
            for (String song : songListView.getItems()) {
                if (song.equals(lastSong)) {
                    songListView.getSelectionModel().select(song);
                    songListView.scrollTo(song);
                    playPauseButton.setText("▶");
                    currentSongLabel.setText(lastSong);
                    updateFavoriteButton(lastSong);
                    break;
                }
            }
        }
    }

    // Métodos auxiliares

    private void exportSong(String song) {
        String songPath = musicManager.getSongPath(song);
        if (songPath == null) return;
        try {
            File exportDir = new File("Exportado");
            if (!exportDir.exists()) exportDir.mkdirs();

            String extension = "";
            int dotIndex = songPath.lastIndexOf('.');
            if (dotIndex != -1) {
                extension = songPath.substring(dotIndex);
            }
            String safeName = song.replaceAll("[\\\\/:*?\"<>|]", "_");
            File exportFile = new File(exportDir, safeName + extension);
            Files.copy(new File(songPath).toPath(), exportFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Canción exportada a: " + exportFile.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void exportPlaylist(String playlistName) {
        if (playlistName == null) return;
        List<String> songs = playlists.get(playlistName);
        if (songs == null || songs.isEmpty()) return;

        try {
            File exportDir = new File("Exportado");
            if (!exportDir.exists()) exportDir.mkdirs();

            for (String song : songs) {
                String songPath = musicManager.getSongPath(song);
                if (songPath != null) {
                    File sourceFile = new File(songPath);
                    String extension = "";
                    int dotIndex = songPath.lastIndexOf('.');
                    if (dotIndex != -1) {
                        extension = songPath.substring(dotIndex);
                    }
                    String safeName = song.replaceAll("[\\\\/:*?\"<>|]", "_");
                    File destFile = new File(exportDir, safeName + extension);
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.out.println("Playlist exportada a carpeta: " + exportDir.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void selectPlaylist(String playlistName) {
        currentPlaylist = playlistName;
        loadPlaylistSongs(playlistName);
    }

    private void loadPlaylistSongs(String playlistName) {
        List<String> songs = playlists.get(playlistName);
        if (songs == null) songs = Collections.emptyList();
        masterSongList.clear();
        masterSongList.addAll(songs);
        filteredSongList.setPredicate(s -> true);
        scrollToCurrentSong();
    }

    private Button createControlButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(40);
        btn.setPrefHeight(30);
        btn.setFocusTraversable(false);
        return btn;
    }

    private void togglePlayPause() {
        State state = mediaPlayer.status().state();
        if (state == State.PLAYING) {
            mediaPlayer.controls().pause();
            playPauseButton.setText("▶");
        } else if (state == State.STOPPED) {
            playSelectedSong();
            playPauseButton.setText("⏸");
        } else {
            mediaPlayer.controls().play();
            playPauseButton.setText("⏸");
        }
    }

    private void stopPlayback() {
        State state = mediaPlayer.status().state();
        if (state != State.STOPPED) {
            mediaPlayer.controls().stop();
            playPauseButton.setText("▶");
        }
    }

    private void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;
        updateShuffleButtonStyle();
    }

    private void updateShuffleButtonStyle() {
        if (shuffleEnabled) {
            shuffleButton.setStyle("-fx-background-color: lightgreen;");
        } else {
            shuffleButton.setStyle("");
        }
    }

    private void playSelectedSong() {
        String selected = songListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        playSong(selected);
    }

    private void playSong(String songName) {
        String path = musicManager.getSongPath(songName);
        if (path == null) return;

        mediaPlayer.media().play(path);
        currentSongLabel.setText(songName);
        playPauseButton.setText("⏸");

        playHistory.push(songName);

        configManager.setCurrentSong(songName, 0);
        configManager.setLastSong(songName);

        updateFavoriteButton(songName);
        updateCoverImage(songName);

        if (mediaPlayer.media().info() != null) {
            progressSlider.setMax(mediaPlayer.media().info().duration() / 1000.0);
        } else {
            progressSlider.setMax(100);
        }
    }

    private void playNextSong() {
        if (shuffleEnabled) {
            int idx = random.nextInt(masterSongList.size());
            playSong(masterSongList.get(idx));
            songListView.getSelectionModel().select(idx);
            songListView.scrollTo(idx);
        } else {
            int currentIndex = songListView.getSelectionModel().getSelectedIndex();
            int nextIndex = (currentIndex + 1) % masterSongList.size();
            playSong(masterSongList.get(nextIndex));
            songListView.getSelectionModel().select(nextIndex);
            songListView.scrollTo(nextIndex);
        }
    }

    private void playPreviousSong() {
        if (!playHistory.isEmpty()) {
            playHistory.pop();
            if (!playHistory.isEmpty()) {
                String lastSong = playHistory.peek();
                playSong(lastSong);
                songListView.getSelectionModel().select(lastSong);
                songListView.scrollTo(lastSong);
            }
        }
    }

    private void toggleFavorito() {
        String currentSong = currentSongLabel.getText();
        if (currentSong == null || currentSong.isEmpty() || currentSong.equals("Sin canción")) return;

        if (favoritos.contains(currentSong)) {
            favoritos.remove(currentSong);
            favoriteButton.setText("♡");
        } else {
            favoritos.add(currentSong);
            favoriteButton.setText("♥");
        }

        playlists.put("Favoritos", new ArrayList<>(favoritos));
        configManager.setFavorites(new ArrayList<>(favoritos));
        if (currentPlaylist.equals("Favoritos")) loadPlaylistSongs("Favoritos");
        updatePlaylistListViewItems();
    }

    private void updateFavoriteButton(String songName) {
        if (favoritos.contains(songName)) {
            favoriteButton.setText("♥");
        } else {
            favoriteButton.setText("♡");
        }
    }

    private void updateCoverImage(String songName) {
        String imagePath = musicManager.getCoverImagePath(songName);
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                coverImageView.setImage(image);
                return;
            }
        }
        coverImageView.setImage(null);
    }

    private void updateTimeLabel(double currentSeconds, double totalSeconds) {
        String currentTime = formatSecondsToMMSS(currentSeconds);
        String totalTime = formatSecondsToMMSS(totalSeconds);
        timeLabel.setText(currentTime + " / " + totalTime);
    }

    private String formatSecondsToMMSS(double seconds) {
        int mins = (int) seconds / 60;
        int secs = (int) seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private void loadSongs(File folder) {
        Map<String, String> loadedSongs = musicManager.loadSongsFromFolder(folder);
        if (loadedSongs != null) {
            masterSongList.clear();
            masterSongList.addAll(loadedSongs.keySet());
            playlists.put("Todo", new ArrayList<>(loadedSongs.keySet()));
            updatePlaylistListViewItems();
            loadPlaylistSongs(currentPlaylist);
        }
    }

    private void scrollToCurrentSong() {
        String currentSong = currentSongLabel.getText();
        if (currentSong != null) {
            int idx = masterSongList.indexOf(currentSong);
            if (idx >= 0) {
                songListView.scrollTo(idx);
            }
        }
    }

    private void createNewPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva Playlist");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre de la nueva playlist:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String playlistName = result.get().trim();
            if (!playlistName.isEmpty() && !playlists.containsKey(playlistName)) {
                playlists.put(playlistName, new ArrayList<>());
                configManager.setPlaylists(playlists);
                updatePlaylistListViewItems();
            }
        }
    }
}
