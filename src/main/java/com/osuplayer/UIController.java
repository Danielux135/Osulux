package com.osuplayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    private final List<String> playHistory = new ArrayList<>();
    private int historyIndex = -1;  // -1 significa que no hay canción en historial seleccionada

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

                        if (progressSlider.getMax() <= 0) {
                            long length = mediaPlayer.media().info() != null ? mediaPlayer.media().info().duration() : 0;
                            if (length > 0) {
                                progressSlider.setMax(length / 1000.0);
                            }
                        }

                        if (seconds <= progressSlider.getMax()) {
                            progressSlider.setValue(seconds);
                        }

                        long length = mediaPlayer.media().info() != null ? mediaPlayer.media().info().duration() : 0;
                        updateTimeLabel(seconds, length / 1000.0);
                    });
                }
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> playNextSong());
            }

            @Override
            public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
                Platform.runLater(() -> {
                    double maxSeconds = newLength / 1000.0;
                    if (maxSeconds > 0) {
                        progressSlider.setMax(maxSeconds);
                    }
                });
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

        playlists.putIfAbsent("Historial", new ArrayList<>());

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
            e.printStackTrace();
        }

        volumeSlider.setValue(configManager.getVolume() * 100);
        mediaPlayer.audio().setVolume((int) volumeSlider.getValue());

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
            exportAllItem.setOnAction(e -> exportPlaylist(cell.getItem()));

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
            if (n != null && !n.equals(currentPlaylist)) selectPlaylist(n);
        });

        progressSlider.setMin(0);
        progressSlider.setMax(0);
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
        searchField.textProperty().addListener((obs, o, n) -> {
            String lower = n.toLowerCase();
            filteredSongList.setPredicate(item -> {
                if (item.toLowerCase().contains(lower)) return true;
                // Filtrar por tags
                List<String> tags = musicManager.getTags(item);
                for (String tag : tags) {
                    if (tag.toLowerCase().contains(lower)) return true;
                }
                // Filtrar por creadores
                List<String> creators = musicManager.getCreators(item);
                for (String creator : creators) {
                    if (creator.toLowerCase().contains(lower)) return true;
                }
                return false;
            });
        });

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
                    mediaPlayer.media().prepare(path);
                    long duration = mediaPlayer.media().info() != null ? mediaPlayer.media().info().duration() : 0;
                    if (duration > 0) {
                        progressSlider.setMax(duration / 1000.0);
                    } else {
                        progressSlider.setMax(0);
                    }
                }

                playPauseButton.setText("▶");

                // Añadimos el último canción al historial y fijamos índice
                playHistory.clear();
                playHistory.add(lastSong);
                historyIndex = 0;
            });
        }
    }

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
        mediaPlayer.controls().stop();
        playPauseButton.setText("▶");
        progressSlider.setValue(0);
    }

    private void playSelectedSong() {
        String selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong == null) return;

        playSong(selectedSong, false);
    }

    /**
     * Reproduce la canción indicada.
     *
     * @param songName   nombre de la canción a reproducir
     * @param fromHistory si es true, indica que la canción se reproduce desde el historial (para no modificar historial internamente)
     */
    private void playSong(String songName, boolean fromHistory) {
        if (songName == null) return;
        String songPath = musicManager.getSongPath(songName);
        if (songPath == null) return;

        mediaPlayer.media().play(songPath);

        currentSongLabel.setText(songName);
        updateFavoriteButton(songName);
        updateCoverImage(songName);

        long duration = mediaPlayer.media().info() != null ? mediaPlayer.media().info().duration() : 0;
        progressSlider.setMax(duration > 0 ? duration / 1000.0 : 0);

        playPauseButton.setText("⏸");

        if (!fromHistory) {
            // Gestionar historial con índice
            if (historyIndex == -1 || playHistory.isEmpty()) {
                playHistory.clear();
                playHistory.add(songName);
                historyIndex = 0;
            } else {
                // Si estamos en mitad del historial y reproducimos una nueva canción, borramos "adelante"
                if (historyIndex < playHistory.size() - 1) {
                    playHistory.subList(historyIndex + 1, playHistory.size()).clear();
                }
                // Añadimos la canción si no es la misma actual
                if (!playHistory.get(historyIndex).equals(songName)) {
                    playHistory.add(songName);
                    historyIndex++;
                }
            }
        }

        configManager.setLastSong(songName);

        // Actualizar historial playlist (sin duplicados y poniendo al principio)
        List<String> historial = playlists.get("Historial");
        if (historial == null) historial = new ArrayList<>();
        historial.remove(songName);
        historial.add(0, songName);
        playlists.put("Historial", historial);
        configManager.setPlaylists(playlists);

        scrollToCurrentSong();
    }

    // Sobrecarga para compatibilidad si quieres llamar sin el parámetro fromHistory
    private void playSong(String songName) {
        playSong(songName, false);
    }

    private void updateFavoriteButton(String songName) {
        if (songName != null && favoritos.contains(songName)) {
            favoriteButton.setText("♥");
        } else {
            favoriteButton.setText("♡");
        }
    }

    private void toggleFavorito() {
        String currentSong = currentSongLabel.getText();
        if (currentSong == null || currentSong.isEmpty()) return;

        if (favoritos.contains(currentSong)) {
            favoritos.remove(currentSong);
        } else {
            favoritos.add(currentSong);
        }

        playlists.put("Favoritos", new ArrayList<>(favoritos));
        configManager.setFavorites(new ArrayList<>(favoritos));

        if (currentPlaylist.equals("Favoritos")) {
            loadPlaylistSongs("Favoritos");
        }

        updateFavoriteButton(currentSong);
        songListView.refresh();
    }

    private void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;
        updateShuffleButtonStyle();
    }

    private void updateShuffleButtonStyle() {
        if (shuffleEnabled) {
            shuffleButton.setStyle("-fx-background-color: #00cc00;");
        } else {
            shuffleButton.setStyle("");
        }
    }

    private void playNextSong() {
        if (shuffleEnabled) {
            int size = filteredSongList.size();
            if (size == 0) return;
            int index = random.nextInt(size);
            String nextSong = filteredSongList.get(index);
            playSong(nextSong, false);
        } else {
            // Si hay una canción siguiente en el historial, la reproducimos SIN modificar historial
            if (historyIndex >= 0 && historyIndex < playHistory.size() - 1) {
                historyIndex++;
                String nextSong = playHistory.get(historyIndex);
                playSong(nextSong, true);  // fromHistory = true para no modificar historial aquí
                int index = filteredSongList.indexOf(nextSong);
                if (index >= 0) {
                    songListView.getSelectionModel().select(index);
                    songListView.scrollTo(index);
                }
            } else {
                // No hay canción siguiente en historial, seguimos con la lista normal
                int currentIndex = songListView.getSelectionModel().getSelectedIndex();
                int nextIndex = currentIndex + 1;
                if (nextIndex >= filteredSongList.size()) nextIndex = 0;
                String nextSong = filteredSongList.get(nextIndex);
                playSong(nextSong, false);
                songListView.getSelectionModel().select(nextIndex);
                songListView.scrollTo(nextIndex);
            }
        }
    }

    private void playPreviousSong() {
        if (shuffleEnabled) {
            if (historyIndex > 0) {
                historyIndex--;
                String previousSong = playHistory.get(historyIndex);
                playSong(previousSong, true);
            }
        } else {
            // Usamos historial para ir atrás si posible
            if (historyIndex > 0) {
                historyIndex--;
                String previousSong = playHistory.get(historyIndex);
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
            if (!entry.getKey().equals("Todo") && !entry.getKey().equals("Favoritos") && !entry.getKey().equals("Historial")) {
                List<String> filtered = new ArrayList<>();
                for (String song : entry.getValue()) {
                    if (loadedSongs.containsKey(song)) filtered.add(song);
                }
                playlists.put(entry.getKey(), filtered);
            }
        }

        playlists.put("Favoritos", new ArrayList<>(favoritos));
        playlists.putIfAbsent("Historial", new ArrayList<>());
        configManager.setPlaylists(playlists);

        loadPlaylistSongs("Todo");
    }

    private void updateCoverImage(String songName) {
        String coverPath = musicManager.getCoverImagePath(songName);
        if (coverPath != null) {
            File coverFile = new File(coverPath);
            if (coverFile.exists()) {
                Image coverImage = new Image(coverFile.toURI().toString(), 170, 170, true, true);
                coverImageView.setImage(coverImage);
                return;
            }
        }
        try (InputStream is = getClass().getResourceAsStream("/default_cover.jpg")) {
            if (is != null) {
                Image defaultImage = new Image(is, 170, 170, true, true);
                coverImageView.setImage(defaultImage);
            } else {
                coverImageView.setImage(null);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            if (name.trim().isEmpty() || playlists.containsKey(name.trim()) ||
                    name.equalsIgnoreCase("Todo") || name.equalsIgnoreCase("Favoritos") || name.equalsIgnoreCase("Historial")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Nombre inválido o ya existe la playlist.");
                alert.showAndWait();
            } else {
                playlists.put(name.trim(), new ArrayList<>());
                configManager.setPlaylists(playlists);
                updatePlaylistListViewItems();
            }
        });
    }

    private void updateTimeLabel(double currentSeconds, double totalSeconds) {
        int  currentMin = (int) (currentSeconds / 60);
        int currentSec = (int) (currentSeconds % 60);
        int totalMin = (int) (totalSeconds / 60);
        int totalSec = (int) (totalSeconds % 60);

        String text = String.format("%02d:%02d / %02d:%02d", currentMin, currentSec, totalMin, totalSec);
        timeLabel.setText(text);
    }
}
