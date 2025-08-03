package com.osuplayer;

import java.io.*;
import java.nio.file.*;
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

    // Constructor
    public UIController(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        this.musicManager = new MusicManager();
        this.configManager = new ConfigManager();
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

        // Configuraciones UI
        favoriteButton.setStyle("-fx-font-size: 24;");
        favoriteButton.setFocusTraversable(false);
        favoriteButton.setOnAction(e -> toggleFavorito());

        coverImageView.setFitWidth(170);
        coverImageView.setFitHeight(170);
        coverImageView.setPreserveRatio(true);
    }

    // Métodos
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
        TextField searchField = new TextField();
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

            // Ruta por defecto con usuario dinámico:
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

        // Playlist con menú contextual eliminar
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
            cell.itemProperty().addListener((obs, oldVal, newVal) -> {
                contextMenu.getItems().clear();
                if (newVal != null && !newVal.equals("Todo") && !newVal.equals("Favoritos")) {
                    contextMenu.getItems().add(deleteItem);
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

        // Configuración del volumeSlider
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            mediaPlayer.audio().setVolume(newVal.intValue());
            configManager.setVolume(newVal.doubleValue() / 100.0);
        });

        // Configuración del label de la canción actual
        currentSongLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        currentSongLabel.setMaxWidth(170);
        currentSongLabel.setWrapText(true);
        currentSongLabel.setAlignment(Pos.CENTER);

        // Caja con carátula, nombre canción y botón favorito
        VBox coverBox = new VBox(5, coverImageView, currentSongLabel, favoriteButton);
        coverBox.setAlignment(Pos.CENTER);
        coverBox.setPrefWidth(170);

        // Controles de reproducción
        HBox controlBox = new HBox(10, previousButton, playPauseButton, stopButton, nextButton, shuffleButton, volumeSlider);
        controlBox.setAlignment(Pos.CENTER);

        // Barra de progreso y tiempo
        HBox progressBox = new HBox(10, progressSlider, timeLabel);
        progressBox.setAlignment(Pos.CENTER);

        // Botón para nueva playlist
        newPlaylistButton = new Button("Nueva Playlist");
        newPlaylistButton.setOnAction(e -> createNewPlaylist());

        // Caja de playlists con botón para crear nueva playlist
        VBox playlistBox = new VBox(5);
        playlistBox.setPrefWidth(170);
        playlistBox.getChildren().addAll(playlistListView);
        HBox newPlaylistButtonBox = new HBox(newPlaylistButton);
        newPlaylistButtonBox.setAlignment(Pos.CENTER);
        playlistBox.getChildren().add(newPlaylistButtonBox);

        // Layout superior: botón a la izquierda, buscador centrado
        HBox topLeftBox = new HBox(chooseFolderButton);
        topLeftBox.setAlignment(Pos.CENTER_LEFT);
        topLeftBox.setPrefWidth(170);

        HBox searchBox = new HBox(searchField);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setPrefWidth(600);

        HBox topBox = new HBox(10, topLeftBox, searchBox);
        topBox.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(topBox, new Insets(10));

        // Root layout principal
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(songListView);
        root.setRight(coverBox);
        root.setBottom(new VBox(5, controlBox, progressBox));
        root.setLeft(playlistBox);

        // Crear y mostrar escena
        Scene scene = new Scene(root, 900, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Filtro para búsqueda en la lista de canciones
        filteredSongList = new FilteredList<>(masterSongList, s -> true);
        songListView.setItems(filteredSongList);
        searchField.textProperty().addListener((obs, o, n) ->
                filteredSongList.setPredicate(item -> item.toLowerCase().contains(n.toLowerCase()))
        );

        // Cargar carpeta guardada y canciones al inicio
        String lastFolder = configManager.getLastFolder();
        if (lastFolder != null && !lastFolder.isEmpty()) {
            File folder = new File(lastFolder);
            if (folder.exists() && folder.isDirectory()) {
                musicManager.setLastFolderPath(lastFolder);
                loadSongs(folder);
                selectPlaylist("Todo"); // Aquí cargamos las canciones visualmente
            }
        }

        // Restaurar la última canción reproducida pero NO reproducir automáticamente
        String lastSong = configManager.getLastSong();
        if (lastSong != null && !lastSong.isEmpty()) {
            for (String song : songListView.getItems()) {
                if (song.equals(lastSong)) {
                    songListView.getSelectionModel().select(song);
                    songListView.scrollTo(song);
                    playPauseButton.setText("▶"); // Botón Play en estado "play"
                    currentSongLabel.setText(lastSong);
                    updateFavoriteButton(lastSong);
                    break;
                }
            }
        }
    }

    private void exportSong(String song) {
        String songPath = musicManager.getSongPath(song);
        if (songPath == null) return;
        try {
            File exportDir = new File("Exportado");
            if (!exportDir.exists()) exportDir.mkdirs();

            String extension = songPath.substring(songPath.lastIndexOf('.'));
            String exportName = song + extension;
            File exportFile = new File(exportDir, exportName);
            Files.copy(Paths.get(songPath), exportFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Canción exportada a: " + exportFile.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
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
            // Reproducir desde el inicio porque se había detenido
            playSelectedSong();
            playPauseButton.setText("⏸");
        } else {
            // Solo continuar la canción pausada
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
        shuffleButton.setStyle(shuffleEnabled ? "-fx-background-color: #00cc00; -fx-text-fill: white;" : "");
    }

    private void loadSongs(File folder) {
        Map<String, String> loadedSongs = musicManager.loadSongsFromFolder(folder);
        playlists.put("Todo", new ArrayList<>(loadedSongs.keySet()));
        playlists.put("Favoritos", new ArrayList<>(favoritos));
        if (currentPlaylist.equals("Todo")) loadPlaylistSongs("Todo");
        updatePlaylistListViewItems();
    }

    private void playSelectedSong() {
        String selected = songListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Guardar la última canción reproducida en la configuración
        configManager.setLastSong(selected);

        if (playHistory.isEmpty() || !playHistory.peek().equals(selected))
            playHistory.push(selected);

        playSelectedSongFromHistory(selected, 0);
    }

    private void playSelectedSongFromHistory(String song, double startPosition) {
        String path = musicManager.getSongPath(song);
        if (path == null) return;
        mediaPlayer.controls().stop();
        mediaPlayer.media().startPaused(path);
        Platform.runLater(() -> {
            progressSlider.setMax(mediaPlayer.status().length() / 1000.0);
            mediaPlayer.controls().setTime((long) (startPosition * 1000));
            updateTimeLabel(startPosition, mediaPlayer.status().length() / 1000.0);
            mediaPlayer.controls().play();
            configManager.setCurrentSong(song, startPosition);
            updateCoverImage(path);
            scrollToCurrentSong();
        });
        currentSongLabel.setText(song);
        updateFavoriteButton(song);
    }

    private void toggleFavorito() {
        String cancion = currentSongLabel.getText();
        if (favoritos.contains(cancion)) favoritos.remove(cancion);
        else favoritos.add(cancion);
        updateFavoriteButton(cancion);
        configManager.setFavorites(new ArrayList<>(favoritos));
        playlists.put("Favoritos", new ArrayList<>(favoritos));
        updatePlaylistListViewItems();
        if (currentPlaylist.equals("Favoritos")) loadPlaylistSongs("Favoritos");
        songListView.refresh();
    }

    private void updateFavoriteButton(String cancion) {
        favoriteButton.setText(favoritos.contains(cancion) ? "♥" : "♡");
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
        if (!playHistory.isEmpty()) configManager.setCurrentSong(playHistory.peek(), currentSeconds);
    }

    private String formatTime(double seconds) {
        int minutes = (int) seconds / 60;
        int secs = (int) seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void createNewPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Crear nueva playlist");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre de la nueva playlist:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty() && !playlists.containsKey(name)) {
                playlists.put(name, new ArrayList<>());
                configManager.setPlaylists(playlists);
                updatePlaylistListViewItems();
                playlistListView.getSelectionModel().select(name);
                currentPlaylist = name;
            }
        });
    }

    private void updateCoverImage(String songPath) {
        try {
            File songFile = new File(songPath);
            File beatmapFolder = songFile.getParentFile();
            File[] osuFiles = beatmapFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".osu"));
            if (osuFiles == null || osuFiles.length == 0) {
                coverImageView.setImage(null);
                return;
            }
            String backgroundImageFileName = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(osuFiles[0]), "UTF-8"))) {
                String line;
                boolean inEventsSection = false;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.equals("[Events]")) {
                        inEventsSection = true;
                    } else if (inEventsSection && line.startsWith("0,")) {
                        String[] parts = line.split(",");
                        if (parts.length >= 3) {
                            backgroundImageFileName = parts[2].replaceAll("\"", "");
                            break;
                        }
                    }
                }
            }
            if (backgroundImageFileName != null) {
                File bgFile = new File(beatmapFolder, backgroundImageFileName);
                if (bgFile.exists()) {
                    Image img = new Image(bgFile.toURI().toString());
                    coverImageView.setImage(img);
                    return;
                }
            }
            coverImageView.setImage(null);
        } catch (Exception e) {
            coverImageView.setImage(null);
            e.printStackTrace();
        }
    }

    private void scrollToCurrentSong() {
        Platform.runLater(() -> {
            if (!playHistory.isEmpty()) {
                String currentSong = playHistory.peek();
                if (currentSong != null) {
                    songListView.getSelectionModel().select(currentSong);
                    songListView.scrollTo(currentSong);
                }
            }
        });
    }
}
