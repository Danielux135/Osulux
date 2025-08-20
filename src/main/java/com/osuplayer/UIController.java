package com.osuplayer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class UIController {

    private final ListView<String> songListView = new ListView<>();
    private FilteredList<String> filteredSongList;
    private final ObservableList<String> masterSongList = FXCollections.observableArrayList();

    private final EmbeddedMediaPlayer audioPlayer;
    private final EmbeddedMediaPlayer videoPlayer;

    private final MusicManager musicManager;
    private final ConfigManager configManager;
    private final PlaylistManager playlistManager;
    private final PlaybackManager playbackManager;
    private final PlaylistHelper playlistHelper;
    private final VideoVisibilityHelper videoVisibilityHelper;
    private final FavoritesManager favoritesManager;
    private final SearchManager searchManager;

    private final Label currentSongLabel = new Label("Sin canción");
    private final Button favoriteButton = new Button("♡");

    private final ImageView coverImageView;
    private final ImageView videoImageView;
    private final StackPane mediaDisplayStack;
    private VBox mediaContainer;

    private final ExportManager exportManager;
    private final CoverManager coverManager;

    private final HistoryManager historyManager = new HistoryManager();

    public UIController(EmbeddedMediaPlayer audioPlayer, EmbeddedMediaPlayer videoPlayer, ConfigManager configManager, MusicManager musicManager) {
        this.audioPlayer = audioPlayer;
        this.videoPlayer = videoPlayer;
        this.configManager = configManager;
        this.musicManager = musicManager;
        
        this.coverImageView = createCoverImageView();
        this.videoImageView = createVideoImageView();
        
        this.videoVisibilityHelper = new VideoVisibilityHelper(videoImageView, coverImageView);
        this.playlistManager = new PlaylistManager(configManager);
        this.favoritesManager = new FavoritesManager(configManager, playlistManager);
        this.searchManager = new SearchManager(musicManager);

        this.playbackManager = new PlaybackManager(audioPlayer, videoPlayer, configManager, this);
        this.exportManager = new ExportManager(musicManager);
        this.coverManager = new CoverManager(musicManager);
        this.playlistHelper = new PlaylistHelper(playlistManager, exportManager);

        this.playlistHelper.setOnPlaylistsChangedCallback(() -> songListView.refresh());

        videoPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
                Platform.runLater(() -> {
                    videoPlayer.controls().stop();
                    videoVisibilityHelper.hideVideo();
                    updateCoverImage(currentSongLabel.getText());
                });
            }
        });

        favoriteButton.setStyle("-fx-font-size: 28px; -fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #ff69b4;");
        favoriteButton.setFocusTraversable(false);
        favoriteButton.setOnAction(e -> toggleFavorito());
        
        videoPlayer.videoSurface().set(new ImageViewVideoSurface(videoImageView));

        mediaDisplayStack = new StackPane();
        mediaDisplayStack.getChildren().addAll(coverImageView, videoImageView);
        mediaDisplayStack.setMaxWidth(Double.MAX_VALUE);
        mediaDisplayStack.setMinWidth(100);
        mediaDisplayStack.setMinHeight(100);
    }
    
    private ImageView createCoverImageView() {
        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setCache(true);
        return view;
    }

    private ImageView createVideoImageView() {
        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setCache(true);
        return view;
    }
    
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Osulux");

        try (InputStream iconStream = getClass().getResourceAsStream("/Icon.jpg")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {}

        UIHelper.TopBarComponents topBar = UIHelper.createTopBar(primaryStage, this::handleFolderSelection);
        UIHelper.ControlBarComponents controlBar = UIHelper.createControlBar();
        
        searchManager.setupSearchField(topBar.searchField(), songListView, currentSongLabel);
        playbackManager.initializeControls(controlBar.progressSlider(), controlBar.timeLabel(), controlBar.volumeSlider(), controlBar.playPauseButton(), controlBar.shuffleButton(), controlBar.previousButton(), controlBar.stopButton(), controlBar.nextButton());
        
        songListView.setCellFactory(lv -> new SongListCell(playlistManager, favoritesManager, exportManager, this::refreshUIState));
        songListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                playSelectedSong();
            }
        });

        mediaContainer = UIHelper.createMediaPanel(mediaDisplayStack, currentSongLabel, favoriteButton);
        
        VBox playlistBox = playlistHelper.initialize(this::selectPlaylist);
        playlistBox.setPadding(new Insets(10));
        playlistBox.setPrefWidth(200);

        final VBox songListContainer = new VBox(songListView);
        songListContainer.setPadding(new Insets(10));
        songListContainer.setPrefWidth(520);
        VBox.setVgrow(songListView, Priority.ALWAYS);
        
        SplitPane splitPane = new SplitPane(playlistBox, songListContainer, mediaContainer);
        LayoutUtils.setupDynamicSplitPane(splitPane, playlistBox, songListContainer, mediaContainer);
        
        BiConsumer<Double, Double> updateMediaSize = (width, height) -> 
            LayoutUtils.updateMediaDisplaySize(
                mediaContainer.getWidth(), 
                splitPane.getHeight(), 
                coverImageView, 
                videoImageView, 
                mediaDisplayStack, 
                videoVisibilityHelper.isVideoVisible()
            );

        splitPane.heightProperty().addListener((obs, oldVal, newVal) -> updateMediaSize.accept(null, null));
        mediaContainer.widthProperty().addListener((obs, oldVal, newVal) -> updateMediaSize.accept(null, null));
        mediaContainer.heightProperty().addListener((obs, oldVal, newVal) -> updateMediaSize.accept(null, null));
        
        songListView.prefHeightProperty().bind(splitPane.heightProperty().subtract(20));

        String lastFolder = configManager.getLastFolder();
        if (lastFolder != null && !lastFolder.isEmpty()) {
            File folder = new File(lastFolder);
            if (folder.exists() && folder.isDirectory()) {
                handleFolderSelection(folder);
            }
        }

        String lastSong = configManager.getLastSong();
        if (lastSong != null && !lastSong.isEmpty()) {
            selectAndPreloadLastSong(lastSong);
            Platform.runLater(() -> updateMediaSize.accept(null, null));
        }

        BorderPane root = new BorderPane();
        root.setTop(topBar.bar());
        root.setCenter(splitPane);
        root.setBottom(controlBar.bar());

        Scene scene = new Scene(root, 1200, 720);
        primaryStage.setScene(scene);
        primaryStage.show();

        Platform.runLater(() -> updateMediaSize.accept(null, null));
    }

    private void handleFolderSelection(File folder) {
        musicManager.setLastFolderPath(folder.getAbsolutePath());
        configManager.setLastFolder(folder.getAbsolutePath());
        loadSongs(folder);
        selectPlaylist("Todo");
    }

    private void refreshUIState() {
        String currentPlaylist = playlistHelper.getSelectedPlaylist();
        if (currentPlaylist != null) {
            loadPlaylistSongs(currentPlaylist);
        }
        updateFavoriteButton(currentSongLabel.getText());
        songListView.refresh();
    }
    
    public void prepareCurrentSongForReplay() {
        String currentSong = currentSongLabel.getText();
        if (currentSong != null && !currentSong.isEmpty()) {
            String path = musicManager.getSongPath(currentSong);
            if (path != null) {
                audioPlayer.media().prepare(path);
            }
        }
    }
    
    private void selectAndPreloadLastSong(String lastSong) {
        if (lastSong == null || lastSong.isEmpty()) return;
        if (masterSongList.isEmpty()) {
            loadSongs(new File(configManager.getLastFolder()));
        }
        Platform.runLater(() -> {
            int index = masterSongList.indexOf(lastSong);
            if (index >= 0) {
                songListView.getSelectionModel().select(index);
                songListView.scrollTo(index);
                currentSongLabel.setText(lastSong);
                updateFavoriteButton(lastSong);
                updateCoverImage(lastSong);
                String path = musicManager.getSongPath(lastSong);
                if (path != null) {
                    playbackManager.onNewMedia();
                    audioPlayer.media().prepare(path);
                }
                playbackManager.updatePlayPauseButton(false);
                musicManager.clearHistory();
                musicManager.addToHistory(lastSong);
                playlistManager.setPlaylistSongs("Historial", new ArrayList<>(musicManager.getHistory()));
                List<String> hm = new ArrayList<>(musicManager.getHistory());
                Collections.reverse(hm);
                historyManager.setHistory(hm, hm.isEmpty() ? -1 : hm.size() - 1);
            }
        });
    }

    public void playNextFromHistoryOrNormal() {
        if (historyManager.hasNext()) {
            String next = historyManager.getNext();
            if (next != null && !next.isEmpty()) {
                playSong(next, true);
                return;
            }
        }
        playNextSong();
    }

    private void selectPlaylist(String playlistName) {
        if (playlistName == null) return;
        playlistHelper.selectPlaylist(playlistName);
        loadPlaylistSongs(playlistName);
    }

    private void loadPlaylistSongs(String playlistName) {
        List<String> songs = playlistManager.getPlaylist(playlistName);
        masterSongList.setAll(songs);
        
        if (filteredSongList == null) {
            filteredSongList = new FilteredList<>(masterSongList, s -> true);
            searchManager.setFilteredList(filteredSongList);
        }
        
        songListView.setItems(filteredSongList);
        scrollToCurrentSong();
    }
    
    public void playSelectedSong() {
        String selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong == null) return;
        playSong(selectedSong, false);
    }

    private void updateFavoriteButton(String songName) {
        favoriteButton.setText(favoritesManager.isFavorite(songName) ? "♥" : "♡");
    }

    private void toggleFavorito() {
        String currentSong = currentSongLabel.getText();
        if (currentSong == null || currentSong.isEmpty() || "Sin canción".equals(currentSong)) {
            return;
        }

        favoritesManager.toggleFavorite(currentSong);
        refreshUIState();
    }

    public void playNextSong() {
        if (filteredSongList == null || filteredSongList.isEmpty()) return;

        if (playbackManager.isShuffleEnabled()) {
            int index = playbackManager.getRandomIndex(filteredSongList.size());
            playSong(filteredSongList.get(index), false);
        } else {
            int currentIndex = songListView.getSelectionModel().getSelectedIndex();
            int nextIndex = (currentIndex + 1) % filteredSongList.size();
            playSong(filteredSongList.get(nextIndex), false);
        }
    }

    public void playPreviousSong() {
        if (filteredSongList == null || filteredSongList.isEmpty()) return;

        int currentIndex = songListView.getSelectionModel().getSelectedIndex();
        int prevIndex = currentIndex > 0 ? currentIndex - 1 : filteredSongList.size() - 1;
        playSong(filteredSongList.get(prevIndex), false);
    }

    public void playPreviousFromHistory() {
        if (historyManager.hasPrevious()) {
            String previous = historyManager.getPrevious();
            if (previous != null && !previous.isEmpty()) {
                playSong(previous, true);
                return;
            }
        }
        playPreviousSong();
    }

    private void playSong(String songName, boolean fromHistory) {
        if (songName == null) return;
        String songPath = musicManager.getSongPath(songName);
        if (songPath == null) return;

        playbackManager.onNewMedia();

        audioPlayer.controls().stop();
        videoPlayer.controls().stop();

        String videoPath = musicManager.getVideoPath(songName);
        if (videoPath != null && new File(videoPath).exists()) {
            videoPlayer.media().play(videoPath, ":no-audio");
            videoVisibilityHelper.showVideo();
        } else {
            videoVisibilityHelper.hideVideo();
        }

        audioPlayer.media().play(songPath);

        currentSongLabel.setText(songName);
        updateFavoriteButton(songName);
        updateCoverImage(songName);
        
        playbackManager.updatePlayPauseButton(true);

        if (!fromHistory) {
            historyManager.addSong(songName);
            musicManager.addToHistory(songName);
            playlistManager.setPlaylistSongs("Historial", new ArrayList<>(musicManager.getHistory()));
            songListView.refresh();
        } else {
            List<String> hm = historyManager.getHistory();
            int idx = hm.indexOf(songName);
            if (idx >= 0) historyManager.setIndex(idx);
        }

        configManager.setLastSong(songName);
        scrollToCurrentSong();
    }

    private void scrollToCurrentSong() {
        Platform.runLater(() -> {
            String currentSong = currentSongLabel.getText();
            if (currentSong != null && filteredSongList != null && !filteredSongList.isEmpty()) {
                int index = filteredSongList.indexOf(currentSong);
                if (index >= 0) {
                    songListView.getSelectionModel().select(index);
                    songListView.scrollTo(index);
                }
            }
        });
    }

    private void loadSongs(File folder) {
        Map<String, String> loadedSongs = musicManager.loadSongsFromFolder(folder);
        Set<String> allSongs = loadedSongs.keySet();
        
        playlistManager.setPlaylistSongs("Todo", new ArrayList<>(allSongs));

        for (String playlistName : playlistManager.getAllPlaylists()) {
            if (!playlistManager.isSpecialPlaylist(playlistName)) {
                List<String> currentSongs = new ArrayList<>(playlistManager.getPlaylist(playlistName));
                currentSongs.removeIf(song -> !allSongs.contains(song));
                playlistManager.setPlaylistSongs(playlistName, currentSongs);
            }
        }

        favoritesManager.validateFavorites(allSongs);

        List<String> currentHistory = new ArrayList<>(musicManager.getHistory());
        currentHistory.removeIf(song -> !allSongs.contains(song));
        playlistManager.setPlaylistSongs("Historial", currentHistory);
        
        List<String> hm = new ArrayList<>(musicManager.getHistory());
        Collections.reverse(hm);
        historyManager.setHistory(hm, hm.isEmpty() ? -1 : hm.size() - 1);

        playlistManager.savePlaylists();
        playlistHelper.refreshPlaylistList();
        loadPlaylistSongs("Todo");
    }

    private void updateCoverImage(String songName) {
        Image coverImage = coverManager.getCoverImage(songName);
        coverImageView.setImage(coverImage);
        Platform.runLater(() -> {
            if (mediaContainer.getScene() != null && mediaContainer.getScene().getWindow() != null) {
                LayoutUtils.updateMediaDisplaySize(
                    mediaContainer.getWidth(),
                    mediaContainer.getScene().getWindow().getHeight(),
                    coverImageView,
                    videoImageView,
                    mediaDisplayStack,
                    videoVisibilityHelper.isVideoVisible()
                );
            }
        });
    }

    public void hideVideo() {
        videoVisibilityHelper.hideVideo();
        Platform.runLater(() -> {
            if (mediaContainer.getScene() != null && mediaContainer.getScene().getWindow() != null) {
                LayoutUtils.updateMediaDisplaySize(
                    mediaContainer.getWidth(),
                    mediaContainer.getScene().getWindow().getHeight(),
                    coverImageView,
                    videoImageView,
                    mediaDisplayStack,
                    videoVisibilityHelper.isVideoVisible()
                );
            }
        });
    }
}