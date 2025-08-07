package com.osuplayer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class UIController {

    private final ListView<String> songListView = new ListView<>();
    private final Slider progressSlider = new Slider();
    private final Label timeLabel = new Label("00:00 / 00:00");
    private final Slider volumeSlider = new Slider(0, 100, 50);
    private FilteredList<String> filteredSongList;
    private final ObservableList<String> masterSongList = FXCollections.observableArrayList();

    private final EmbeddedMediaPlayer audioPlayer;
    private final EmbeddedMediaPlayer videoPlayer;

    private boolean isSeeking = false;
    private boolean shuffleEnabled = false;
    private final Random random = new Random();

    private final MusicManager musicManager;
    private final ConfigManager configManager;

    private final Label currentSongLabel = new Label("Sin canción");

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

    private final ExportManager exportManager;
    private final CoverManager coverManager;

    private final SearchManager searchManager;

    public UIController(EmbeddedMediaPlayer audioPlayer, EmbeddedMediaPlayer videoPlayer, ConfigManager configManager, MusicManager musicManager) {
        this.audioPlayer = audioPlayer;
        this.videoPlayer = videoPlayer;
        this.configManager = configManager;
        this.musicManager = musicManager;

        this.exportManager = new ExportManager(musicManager);
        this.coverManager = new CoverManager(musicManager);

        favoritos.addAll(new HashSet<>(configManager.getFavorites()));

        initPlaylists();

        audioPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void timeChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer, long newTime) {
                if (!isSeeking) {
                    Platform.runLater(() -> {
                        double seconds = newTime / 1000.0;
                        if (progressSlider.getMax() <= 0) {
                            long length = 0;
                            if (mediaPlayer.media().info() != null) {
                                length = mediaPlayer.media().info().duration();
                            }
                            if (length > 0) {
                                progressSlider.setMax(length / 1000.0);
                            }
                        }
                        if (seconds <= progressSlider.getMax()) {
                            progressSlider.setValue(seconds);
                        }
                        long length = 0;
                        if (mediaPlayer.media().info() != null) {
                            length = mediaPlayer.media().info().duration();
                        }
                        updateTimeLabel(seconds, length / 1000.0);
                    });
                }
            }

            @Override
            public void finished(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
                Platform.runLater(() -> playNextSong());
            }

            @Override
            public void lengthChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer, long newLength) {
                Platform.runLater(() -> {
                    double maxSeconds = newLength / 1000.0;
                    if (maxSeconds > 0) {
                        progressSlider.setMax(maxSeconds);
                    }
                });
            }
        });

        videoPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
                Platform.runLater(() -> {
                    videoPlayer.controls().stop();
                    coverImageView.setImage(null);
                    updateCoverImage(currentSongLabel.getText());
                });
            }
        });

        favoriteButton.setStyle("-fx-font-size: 24;");
        favoriteButton.setFocusTraversable(false);
        favoriteButton.setOnAction(e -> toggleFavorito());

        coverImageView.setFitWidth(170);
        coverImageView.setFitHeight(170);
        coverImageView.setPreserveRatio(true);

        videoPlayer.videoSurface().set(new ImageViewVideoSurface(coverImageView));

        searchManager = new SearchManager(musicManager);
    }

    private void initPlaylists() {
        playlists.clear();
        Map<String, List<String>> loadedPlaylists = configManager.getPlaylists();
        playlists.put("Todo", new ArrayList<>());
        playlists.put("Favoritos", new ArrayList<>());
        playlists.put("Historial", new ArrayList<>());

        for (Map.Entry<String, List<String>> entry : loadedPlaylists.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("Todo") && !key.equals("Favoritos") && !key.equals("Historial")) {
                playlists.put(key, new ArrayList<>(entry.getValue()));
            }
        }

        List<String> allSongs = new ArrayList<>();
        if (configManager.getLastFolder() != null && !configManager.getLastFolder().isEmpty()) {
            allSongs.addAll(musicManager.loadSongsFromFolder(new File(configManager.getLastFolder())).keySet());
        }
        playlists.put("Todo", allSongs);

        List<String> favList = new ArrayList<>(favoritos);
        playlists.put("Favoritos", favList);

        List<String> historyList = musicManager.getHistory();
        playlists.put("Historial", new ArrayList<>(historyList));

        updatePlaylistListViewItems();
    }

    private void updatePlaylistListViewItems() {
        playlistListView.getItems().clear();
        playlistListView.getItems().add("Todo");
        playlistListView.getItems().add("Favoritos");
        playlistListView.getItems().add("Historial");
        for (String key : playlists.keySet()) {
            if (!key.equals("Todo") && !key.equals("Favoritos") && !key.equals("Historial")) {
                playlistListView.getItems().add(key);
            }
        }
        playlistListView.getSelectionModel().select(currentPlaylist);
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Osulux");

        try (InputStream iconStream = getClass().getResourceAsStream("/Icon.jpg")) {
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                primaryStage.getIcons().add(icon);
            }
        } catch (Exception e) {
            System.err.println("Error cargando icono: " + e.getMessage());
        }

        volumeSlider.setValue(configManager.getVolume() * 100);
        audioPlayer.audio().setVolume((int) volumeSlider.getValue());

        Button chooseFolderButton = new Button("Abrir carpeta Songs");

        searchField = new TextField();
        searchField.setPromptText("Buscar canciones, artistas, creadores o tags...");
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
        
                    Menu addToPlaylistMenu = new Menu("Añadir a playlist");
        
                    // Construimos el submenú solo al mostrarlo para que se actualice dinámicamente
                    contextMenu.setOnShowing(event -> {
                        addToPlaylistMenu.getItems().clear();
        
                        // Añadir Favoritos
                        MenuItem favItem = new MenuItem("Favoritos");
                        favItem.setOnAction(e -> {
                            if (!favoritos.contains(item)) {
                                favoritos.add(item);
                                playlists.put("Favoritos", new ArrayList<>(favoritos));
                                configManager.setFavorites(new ArrayList<>(favoritos));
                                if (currentPlaylist.equals("Favoritos")) loadPlaylistSongs("Favoritos");
                                updateFavoriteButton(currentSongLabel.getText());
                                songListView.refresh();
                            }
                        });
                        addToPlaylistMenu.getItems().add(favItem);
        
                        // Añadir Historial
                        MenuItem historyItem = new MenuItem("Historial");
                        historyItem.setOnAction(e -> {
                            List<String> historial = playlists.get("Historial");
                            if (historial == null) historial = new ArrayList<>();
                            if (!historial.contains(item)) {
                                historial.add(0, item);
                                playlists.put("Historial", historial);
                                musicManager.addToHistory(item);
                                configManager.setPlaylists(playlists);
                                if (currentPlaylist.equals("Historial")) loadPlaylistSongs("Historial");
                            }
                        });
                        addToPlaylistMenu.getItems().add(historyItem);
        
                        // Añadir otras playlists (excluyendo Todo, Favoritos e Historial)
                        for (String playlistName : playlists.keySet()) {
                            if (!playlistName.equals("Todo") && !playlistName.equals("Favoritos") && !playlistName.equals("Historial")) {
                                MenuItem playlistItem = new MenuItem(playlistName);
                                playlistItem.setOnAction(ev -> {
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
                    });
        
                    contextMenu.getItems().add(addToPlaylistMenu);
        
                    MenuItem exportSongItem = new MenuItem("Exportar canción");
                    exportSongItem.setOnAction(e -> exportManager.exportSong(item));
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
                    if (item != null && (item.equals("Todo") || item.equals("Favoritos") || item.equals("Historial"))) {
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
                if (selected != null && !selected.equals("Todo") && !selected.equals("Favoritos") && !selected.equals("Historial")) {
                    playlists.remove(selected);
                    configManager.setPlaylists(playlists);
                    updatePlaylistListViewItems();
                    selectPlaylist("Todo");
                }
            });

            MenuItem exportAllItem = new MenuItem("Exportar todas las canciones");
            exportAllItem.setOnAction(e -> exportManager.exportPlaylist(cell.getItem(), playlists));

            cell.itemProperty().addListener((obs, oldVal, newVal) -> {
                contextMenu.getItems().clear();
                if (newVal != null) {
                    contextMenu.getItems().add(exportAllItem);
                    if (!newVal.equals("Todo") && !newVal.equals("Favoritos") && !newVal.equals("Historial")) {
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
            if (n != null && !n.equals(currentPlaylist)) {
                selectPlaylist(n);
            }
        });

        progressSlider.setMin(0);
        progressSlider.setMax(0);
        progressSlider.setValue(0);
        progressSlider.setOnMousePressed(e -> isSeeking = true);
        progressSlider.setOnMouseReleased(e -> {
            State state = audioPlayer.status().state();
            if (state == State.PLAYING || state == State.PAUSED) {
                long time = (long) (progressSlider.getValue() * 1000);
                audioPlayer.controls().setTime(time);
                videoPlayer.controls().setTime(time);
            }
            isSeeking = false;
        });

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            audioPlayer.audio().setVolume(newVal.intValue());
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

        searchManager.attachSearchField(searchField, filteredSongList);

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
            selectAndPreloadLastSong(lastSong);
        }
    }

    private void selectAndPreloadLastSong(String lastSong) {
        if (lastSong == null || lastSong.isEmpty()) return;
        int index = -1;
        for (int i = 0; i < filteredSongList.size(); i++) {
            if (filteredSongList.get(i).equals(lastSong)) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            final int idx = index;
            Platform.runLater(() -> {
                songListView.getSelectionModel().select(idx);
                songListView.scrollTo(idx);

                currentSongLabel.setText(lastSong);
                updateFavoriteButton(lastSong);
                updateCoverImage(lastSong);

                String path = musicManager.getSongPath(lastSong);
                if (path != null) {
                    audioPlayer.media().prepare(path);
                    long duration = 0;
                    if (audioPlayer.media().info() != null) {
                        duration = audioPlayer.media().info().duration();
                    }
                    progressSlider.setMax(duration > 0 ? duration / 1000.0 : 0);
                }

                playPauseButton.setText("▶");

                musicManager.clearHistory();
                musicManager.addToHistory(lastSong);
            });
        }
    }

    private void selectPlaylist(String playlistName) {
        if (playlistName == null) return;
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
        State state = audioPlayer.status().state();
        if (state == null) {
            audioPlayer.controls().play();
            videoPlayer.controls().pause();
            playPauseButton.setText("⏸");
        } else switch (state) {
            case PLAYING -> {
                audioPlayer.controls().pause();
                videoPlayer.controls().pause();
                playPauseButton.setText("▶");
            }
            case STOPPED -> {
                playSelectedSong();
                playPauseButton.setText("⏸");
            }
            default -> {
                audioPlayer.controls().play();
                videoPlayer.controls().pause();
                playPauseButton.setText("⏸");
            }
        }
    }

    private void stopPlayback() {
        audioPlayer.controls().stop();
        videoPlayer.controls().stop();
        playPauseButton.setText("▶");
        progressSlider.setValue(0);
    }

    private void playSelectedSong() {
        String selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong == null) return;
        playSong(selectedSong, false);
    }

    private void playSong(String songName, boolean fromHistory) {
        if (songName == null) return;
        String songPath = musicManager.getSongPath(songName);
        if (songPath == null) return;

        audioPlayer.controls().stop();
        videoPlayer.controls().stop();

        String videoPath = musicManager.getVideoPath(songName);
        if (videoPath != null && !videoPath.isEmpty() && new File(videoPath).exists()) {
            videoPlayer.media().play(videoPath, ":no-audio");
        } else {
            videoPlayer.controls().stop();
        }

        audioPlayer.media().play(songPath);

        currentSongLabel.setText(songName);
        updateFavoriteButton(songName);
        updateCoverImage(songName);

        long duration = 0;
        if (audioPlayer.media().info() != null) {
            duration = audioPlayer.media().info().duration();
        }
        progressSlider.setMax(duration > 0 ? duration / 1000.0 : 0);

        playPauseButton.setText("⏸");

        if (!fromHistory) {
            musicManager.addToHistory(songName);
            List<String> historial = playlists.get("Historial");
            if (historial == null) historial = new ArrayList<>();
            historial.remove(songName);
            historial.add(0, songName);
            playlists.put("Historial", historial);
            configManager.setPlaylists(playlists);
        }

        configManager.setLastSong(songName);
        scrollToCurrentSong();
    }

    private void updateFavoriteButton(String songName) {
        favoriteButton.setText(songName != null && favoritos.contains(songName) ? "♥" : "♡");
    }

    private void toggleFavorito() {
        String currentSong = currentSongLabel.getText();
        if (currentSong == null || currentSong.isEmpty()) return;

        if (favoritos.contains(currentSong)) favoritos.remove(currentSong);
        else favoritos.add(currentSong);

        playlists.put("Favoritos", new ArrayList<>(favoritos));
        configManager.setFavorites(new ArrayList<>(favoritos));

        if (currentPlaylist.equals("Favoritos")) loadPlaylistSongs("Favoritos");

        updateFavoriteButton(currentSong);
        songListView.refresh();
    }

    private void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;
        updateShuffleButtonStyle();
    }

    private void updateShuffleButtonStyle() {
        shuffleButton.setStyle(shuffleEnabled ? "-fx-background-color: #00cc00;" : "");
    }

    private void playNextSong() {
        if (filteredSongList.isEmpty()) return;
        if (shuffleEnabled) {
            int size = filteredSongList.size();
            int index = random.nextInt(size);
            playSong(filteredSongList.get(index), false);
        } else {
            if (musicManager.hasNextInHistory()) {
                String nextSong = musicManager.getNextFromHistory();
                playSong(nextSong, true);
                int index = filteredSongList.indexOf(nextSong);
                if (index >= 0) {
                    songListView.getSelectionModel().select(index);
                    songListView.scrollTo(index);
                }
            } else {
                int currentIndex = songListView.getSelectionModel().getSelectedIndex();
                int nextIndex = (currentIndex + 1) % filteredSongList.size();
                String nextSong = filteredSongList.get(nextIndex);
                playSong(nextSong, false);
                songListView.getSelectionModel().select(nextIndex);
                songListView.scrollTo(nextIndex);
            }
        }
    }

    private void playPreviousSong() {
        if (shuffleEnabled && musicManager.hasPreviousInHistory()) {
            String previousSong = musicManager.getPreviousFromHistory();
            playSong(previousSong, true);
            int index = filteredSongList.indexOf(previousSong);
            if (index >= 0) {
                songListView.getSelectionModel().select(index);
                songListView.scrollTo(index);
            }
        } else if (musicManager.hasPreviousInHistory()) {
            String previousSong = musicManager.getPreviousFromHistory();
            playSong(previousSong, true);
            int index = filteredSongList.indexOf(previousSong);
            if (index >= 0) {
                songListView.getSelectionModel().select(index);
                songListView.scrollTo(index);
            }
        } else {
            int currentIndex = songListView.getSelectionModel().getSelectedIndex();
            int prevIndex = currentIndex - 1;
            if (prevIndex < 0) prevIndex = filteredSongList.size() - 1;
            String prevSong = filteredSongList.get(prevIndex);
            playSong(prevSong, false);
            songListView.getSelectionModel().select(prevIndex);
            songListView.scrollTo(prevIndex);
        }
    }

    private void scrollToCurrentSong() {
        Platform.runLater(() -> {
            String currentSong = currentSongLabel.getText();
            if (currentSong != null) {
                int index = filteredSongList.indexOf(currentSong);
                if (index >= 0) {
                    songListView.scrollTo(index);
                    songListView.getSelectionModel().select(index);
                }
            }
        });
    }

    private void loadSongs(File folder) {
        Map<String, String> loadedSongs = musicManager.loadSongsFromFolder(folder);
        playlists.put("Todo", new ArrayList<>(loadedSongs.keySet()));

        for (Map.Entry<String, List<String>> entry : playlists.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("Todo") && !key.equals("Favoritos") && !key.equals("Historial")) {
                List<String> filtered = new ArrayList<>();
                for (String song : entry.getValue()) {
                    if (loadedSongs.containsKey(song)) filtered.add(song);
                }
                playlists.put(key, filtered);
            }
        }

        playlists.put("Favoritos", new ArrayList<>(favoritos));
        playlists.put("Historial", new ArrayList<>(musicManager.getHistory()));

        configManager.setPlaylists(playlists);
        loadPlaylistSongs("Todo");
    }

    private void updateCoverImage(String songName) {
        Image coverImage = coverManager.getCoverImage(songName);
        if (coverImage != null) {
            coverImageView.setImage(coverImage);
        } else {
            coverImageView.setImage(null);
        }
    }

    private void createNewPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva Playlist");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre de la playlist:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String trimmedName = name.trim();
            if (trimmedName.isEmpty() || playlists.containsKey(trimmedName) ||
                    trimmedName.equalsIgnoreCase("Todo") || trimmedName.equalsIgnoreCase("Favoritos") || trimmedName.equalsIgnoreCase("Historial")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Nombre inválido o ya existe la playlist.");
                alert.showAndWait();
            } else {
                playlists.put(trimmedName, new ArrayList<>());
                configManager.setPlaylists(playlists);
                updatePlaylistListViewItems();
            }
        });
    }

    private void updateTimeLabel(double currentSeconds, double totalSeconds) {
        int currentMin = (int) (currentSeconds / 60);
        int currentSec = (int) (currentSeconds % 60);
        int totalMin = (int) (totalSeconds / 60);
        int totalSec = (int) (totalSeconds % 60);
        timeLabel.setText(String.format("%02d:%02d / %02d:%02d", currentMin, currentSec, totalMin, totalSec));
    }
}
