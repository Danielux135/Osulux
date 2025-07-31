package com.osuplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javafx.application.Application;
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

public class OsuMusicPlayer extends Application {

    private ListView<String> songListView = new ListView<>();
    private MediaPlayer mediaPlayer;

    private Slider progressSlider = new Slider();
    private Label timeLabel = new Label("00:00 / 00:00");
    private Slider volumeSlider = new Slider(0, 1, 0.5);

    private Map<String, String> songPathMap = new HashMap<>();
    private Set<String> songsAdded = new HashSet<>();

    private static final String CONFIG_FILE = "config.properties";
    private String lastFolderPath = null;
    private boolean isSeeking = false;

    // Para filtrar canciones sin perder la lista original
    private ObservableList<String> masterSongList = FXCollections.observableArrayList();
    private FilteredList<String> filteredSongList;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("OSU! Music Player");

        loadConfig(); // cargar volumen y última carpeta

        Button chooseFolderButton = new Button("Seleccionar carpeta de canciones");

        // Nuevo campo de texto para buscar
        TextField searchField = new TextField();
        searchField.setPromptText("Buscar canciones o artistas...");

        Button previousButton = new Button("Anterior");
        Button playButton = new Button("Play");
        Button pauseButton = new Button("Pause");
        Button stopButton = new Button("Stop");
        Button nextButton = new Button("Siguiente");

        chooseFolderButton.setOnAction(e -> chooseFolder(primaryStage));
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
            if (event.getClickCount() == 2) {
                playSelectedSong();
            }
        });

        progressSlider.setMin(0);
        progressSlider.setMax(1);
        progressSlider.setValue(0);

        progressSlider.setOnMousePressed(e -> {
            isSeeking = true;
        });

        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            }
            isSeeking = false;
        });

        volumeSlider.setShowTickMarks(false);
        volumeSlider.setShowTickLabels(false);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
            saveConfigVolume(newVal.doubleValue());
        });

        HBox controlBox = new HBox(10, previousButton, playButton, pauseButton, stopButton, nextButton, volumeSlider);
        HBox progressBox = new HBox(10, progressSlider, timeLabel);

        // Layout para la parte superior: botón elegir carpeta a la izquierda y buscador a la derecha
        HBox topBox = new HBox(10, chooseFolderButton, searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS); // que el buscador se expanda

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(songListView);
        root.setBottom(new VBox(controlBox, progressBox));

        Scene scene = new Scene(root, 700, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Inicializar la lista filtrada con la lista maestra
        filteredSongList = new FilteredList<>(masterSongList, s -> true);
        songListView.setItems(filteredSongList);

        // Listener para actualizar la lista filtrada según texto en el buscador
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String lowerFilter = newValue.toLowerCase();
            filteredSongList.setPredicate(item -> {
                if (item == null) return false;
                return item.toLowerCase().contains(lowerFilter);
            });
        });

        if (lastFolderPath != null) {
            File folder = new File(lastFolderPath);
            if (folder.exists() && folder.isDirectory()) {
                loadSongsFromFolder(folder);
            }
        }
    }

    private void chooseFolder(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Selecciona la carpeta de canciones de OSU!");
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            lastFolderPath = selectedDirectory.getAbsolutePath();
            saveConfigFolder(lastFolderPath);
            loadSongsFromFolder(selectedDirectory);
        }
    }

    private void loadSongsFromFolder(File folder) {
        masterSongList.clear();
        songPathMap.clear();
        songsAdded.clear();
        findMusicFiles(folder);

        if (masterSongList.isEmpty()) {
            masterSongList.add("No se encontraron canciones en esta carpeta");
        }
    }

    private void findMusicFiles(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findMusicFiles(file);
                } else if (file.getName().toLowerCase().endsWith(".osu")) {
                    try {
                        String title = null;
                        String artist = null;
                        for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                            if (line.startsWith("Title:")) {
                                title = line.substring(6).trim();
                            } else if (line.startsWith("Artist:")) {
                                artist = line.substring(7).trim();
                            }
                            if (title != null && artist != null) break;
                        }
                        if (title != null && artist != null) {
                            String displayName = title + " — " + artist;
                            if (!songsAdded.contains(displayName)) {
                                File parent = file.getParentFile();
                                File[] mp3s = parent.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
                                if (mp3s != null && mp3s.length > 0) {
                                    masterSongList.add(displayName);
                                    songPathMap.put(displayName, mp3s[0].getAbsolutePath());
                                    songsAdded.add(displayName);
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void playSelectedSong() {
        String selectedSongName = songListView.getSelectionModel().getSelectedItem();
        if (selectedSongName != null && songPathMap.containsKey(selectedSongName)) {
            String fullPath = songPathMap.get(selectedSongName);

            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
            Media media = new Media(new File(fullPath).toURI().toString());
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
    }

    private void playPreviousSong() {
        int currentIndex = songListView.getSelectionModel().getSelectedIndex();
        if (currentIndex > 0) {
            int previousIndex = currentIndex - 1;
            songListView.getSelectionModel().select(previousIndex);
            playSelectedSong();
        }
    }

    private void playNextSong() {
        int currentIndex = songListView.getSelectionModel().getSelectedIndex();
        if (currentIndex < songListView.getItems().size() - 1) {
            int nextIndex = currentIndex + 1;
            songListView.getSelectionModel().select(nextIndex);
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

    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(input);
                String volumeStr = props.getProperty("volume");
                String folderStr = props.getProperty("lastFolder");
                if (volumeStr != null) {
                    volumeSlider.setValue(Double.parseDouble(volumeStr));
                }
                if (folderStr != null && !folderStr.isBlank()) {
                    lastFolderPath = folderStr;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveConfigVolume(double volume) {
        saveConfig(lastFolderPath, volume);
    }

    private void saveConfigFolder(String folder) {
        saveConfig(folder, volumeSlider.getValue());
    }

    private void saveConfig(String folder, double volume) {
        Properties props = new Properties();
        props.setProperty("volume", String.valueOf(volume));
        if (folder != null) {
            props.setProperty("lastFolder", folder);
        }
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            props.store(output, "OSU Music Player Config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
