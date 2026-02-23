package com.osuplayer.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import com.osuplayer.beatmapbrowser.OsuBeatmapBrowserDialog;
import com.osuplayer.beatmaps.BeatmapDeletionHelper;
import com.osuplayer.config.ConfigManager;
import com.osuplayer.discord.DiscordRichPresence;
import com.osuplayer.exporting.ExportManager;
import com.osuplayer.favorites.FavoritesManager;
import com.osuplayer.history.HistoryManager;
import com.osuplayer.lang.I18n;
import com.osuplayer.lang.LanguageManager;
import com.osuplayer.playback.MusicManager;
import com.osuplayer.playback.PlaybackManager;
import com.osuplayer.playback.PlaylistHelper;
import com.osuplayer.playback.PlaylistManager;
import com.osuplayer.playback.SongListCell;
import com.osuplayer.playback.VideoSynchronizer;
import com.osuplayer.playback.VideoVisibilityHelper;
import com.osuplayer.search.SearchManager;
import com.osuplayer.shortcuts.GlobalMediaKeyService;
import com.osuplayer.shortcuts.ShortcutAction;
import com.osuplayer.shortcuts.ShortcutManager;
import com.osuplayer.shortcuts.ShortcutPreferencesDialog;
import com.osuplayer.update.UpdateService;
import com.osuplayer.dependencies.IconDependencyProvider;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.EventTarget;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
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
    private final VideoSynchronizer videoSynchronizer;
    private final FavoritesManager favoritesManager;
    private final SearchManager searchManager;
    private final OsuBeatmapBrowserDialog beatmapBrowserDialog;
    private final Label currentSongLabel = new Label();
    private final Button favoriteButton = new Button("♡");
    private final ImageView coverImageView;
    private final ImageView videoImageView;
    private final StackPane mediaDisplayStack;
    private Label speedLabel;
    private PauseTransition speedHideDelay;
    private BorderPane mediaContainer;
    private final ExportManager exportManager;
    private final CoverManager coverManager;
    private final BeatmapDeletionHelper beatmapDeletionHelper;
    private final HistoryManager historyManager = new HistoryManager();
    private final GlobalMediaKeyService globalMediaKeyService;
    private final ShortcutManager shortcutManager;
    private final ShortcutPreferencesDialog shortcutPreferencesDialog;
    private final SettingsDialog settingsDialog;
    private final UpdateService updateService;
    private Scene scene;
    private TextField searchField;
    private static final String NO_SONG_KEY = "Sin canción";
    private String noSongText = "";
    private String pendingLastSongSelection;
    private boolean lastSongSelectionApplied;
    private int selectionFreezeDepth;

    public UIController(EmbeddedMediaPlayer audioPlayer, EmbeddedMediaPlayer videoPlayer, ConfigManager configManager, MusicManager musicManager, DiscordRichPresence discord) {
        this.audioPlayer = audioPlayer;
        this.videoPlayer = videoPlayer;
        this.configManager = configManager;
        this.musicManager = musicManager;
        this.beatmapDeletionHelper = new BeatmapDeletionHelper(configManager);
        
        this.coverImageView = createCoverImageView();
        this.videoImageView = createVideoImageView();
        
        this.videoVisibilityHelper = new VideoVisibilityHelper(videoImageView, coverImageView);
        this.videoSynchronizer = new VideoSynchronizer(audioPlayer, this.videoPlayer);
        this.videoSynchronizer.setCallbacks(
            () -> Platform.runLater(videoVisibilityHelper::showVideo),
            () -> Platform.runLater(videoVisibilityHelper::hideVideo)
        );
        this.playlistManager = new PlaylistManager(configManager);
        this.favoritesManager = new FavoritesManager(configManager, playlistManager);
        this.searchManager = new SearchManager(musicManager);
        initializeNoSongLabel();
        this.songListView.setFixedCellSize(28);
        this.songListView.setStyle(String.join("",
            "-fx-selection-bar: -fx-accent;",
            "-fx-selection-bar-non-focused: -fx-accent;",
            "-fx-focus-color: transparent;",
            "-fx-faint-focus-color: transparent;"
        ));

        this.playbackManager = new PlaybackManager(audioPlayer, configManager, this, discord, videoSynchronizer);
        this.beatmapBrowserDialog = new OsuBeatmapBrowserDialog(configManager, this::handleLibraryUpdate);
        this.shortcutManager = new ShortcutManager(configManager);
        this.globalMediaKeyService = new GlobalMediaKeyService(this::executeShortcutAction);
        this.shortcutPreferencesDialog = new ShortcutPreferencesDialog(shortcutManager);
        this.settingsDialog = new SettingsDialog(configManager, this::applyTheme);
        this.settingsDialog.setOnHistoryRetentionChanged(this::handleHistoryRetentionPreferenceChanged);
        this.settingsDialog.setOnLanguageChanged(this::refreshOpenWindowsLanguage);
        this.updateService = new UpdateService();
        
        this.exportManager = new ExportManager(musicManager);
        this.coverManager = new CoverManager(musicManager);
        this.playlistHelper = new PlaylistHelper(playlistManager, exportManager);

        this.playlistHelper.setOnPlaylistsChangedCallback(() -> songListView.refresh());

        restorePersistentHistoryIfEnabled();

        this.videoPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
                Platform.runLater(() -> {
                    videoSynchronizer.reset();
                    videoVisibilityHelper.hideVideo();
                    updateCoverImage(currentSongLabel.getText());
                });
            }
        });

        favoriteButton.setStyle("-fx-font-size: 28px; -fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #ff69b4;");
        favoriteButton.setFocusTraversable(false);
        favoriteButton.setOnAction(e -> toggleFavorito());
        
        this.videoPlayer.videoSurface().set(new ImageViewVideoSurface(videoImageView));

        mediaDisplayStack = new StackPane();
        mediaDisplayStack.getChildren().addAll(coverImageView, videoImageView);
        mediaDisplayStack.setMaxWidth(Double.MAX_VALUE);
        mediaDisplayStack.setMinWidth(100);
        mediaDisplayStack.setMinHeight(100);
    }
    
    private void initializeNoSongLabel() {
        updateNoSongPlaceholder(true);
        LanguageManager.getInstance().languageIdProperty().addListener((obs, oldId, newId) -> updateNoSongPlaceholder(false));
    }
    
    private void updateNoSongPlaceholder(boolean force) {
        String translated = I18n.tr(NO_SONG_KEY);
        String currentText = currentSongLabel.getText();
        boolean isPlaceholder = isNoSongLabel(currentText);
        boolean shouldUpdate = (force && isPlaceholder)
            || currentText == null
            || currentText.isBlank()
            || currentText.equals(noSongText);
        noSongText = translated;
        if (shouldUpdate) {
            currentSongLabel.setText(translated);
        }
    }
    
    private boolean isNoSongLabel(String value) {
        return value == null || value.isBlank() || value.equals(noSongText);
    }
    
    private void showNoSongText() {
        updateNoSongPlaceholder(true);
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
        if (primaryStage.getIcons().isEmpty()) {
            Image appIcon = IconDependencyProvider.getOrNull();
            if (appIcon != null) {
                primaryStage.getIcons().add(appIcon);
            }
        }
        ArrayList<Image> iconCopies = new ArrayList<>(primaryStage.getIcons());
        beatmapBrowserDialog.setIconImages(iconCopies);
        shortcutPreferencesDialog.setIconImages(iconCopies);
        settingsDialog.setIconImages(iconCopies);
    
        UIHelper.TopBarComponents topBar = UIHelper.createTopBar(
            primaryStage,
            this::handleFolderSelection,
            () -> beatmapBrowserDialog.show(primaryStage),
            () -> shortcutPreferencesDialog.show(primaryStage),
            () -> settingsDialog.show(primaryStage),
            this::handleUpdateCheck,
            this::showAboutDialog
        );
        UIHelper.ControlBarComponents controlBar = UIHelper.createControlBar();
        this.speedLabel = controlBar.speedLabel();
        if (this.speedLabel != null) {
            this.speedLabel.setText("");
            this.speedLabel.setOpacity(0);
        }
        this.speedHideDelay = new PauseTransition(Duration.seconds(2));
        this.speedHideDelay.setOnFinished(e -> {
            if (speedLabel != null) {
                speedLabel.setOpacity(0);
            }
        });
    
        this.searchField = topBar.searchField();
        searchManager.setupSearchField(searchField, songListView, currentSongLabel);
        playbackManager.initializeControls(controlBar.progressSlider(), controlBar.timeLabel(), controlBar.volumeSlider(), controlBar.playPauseButton(), controlBar.shuffleButton(), controlBar.loopButton(), controlBar.previousButton(), controlBar.stopButton(), controlBar.nextButton());
    
        songListView.setCellFactory(lv -> new SongListCell(playlistManager, favoritesManager, exportManager, musicManager, this::refreshUIStatePreservingSelection, this::handleBeatmapDeletionFromLibrary));
        songListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                playSelectedSong();
            }
        });
    
        mediaContainer = UIHelper.createMediaPanel(mediaDisplayStack, currentSongLabel, favoriteButton);
    
        coverImageView.fitWidthProperty().bind(mediaDisplayStack.widthProperty());
        coverImageView.fitHeightProperty().bind(mediaDisplayStack.heightProperty());
        
        videoImageView.fitWidthProperty().bind(mediaDisplayStack.widthProperty());
        videoImageView.fitHeightProperty().bind(mediaDisplayStack.heightProperty());

        BorderPane root = new BorderPane();
        root.setTop(topBar.bar());
        root.setBottom(controlBar.bar());
    
        this.scene = new Scene(root, configManager.getWindowWidth(), configManager.getWindowHeight());
        setupGlobalShortcuts();
    
        VBox playlistBox = playlistHelper.initialize(this::selectPlaylist, scene.getStylesheets());
        final VBox songListContainer = new VBox(songListView);
        VBox.setVgrow(songListView, Priority.ALWAYS);
    
        SplitPane splitPane = new SplitPane(playlistBox, songListContainer, mediaContainer);
        LayoutUtils.setupDynamicSplitPane(splitPane, playlistBox, songListContainer, mediaContainer);
    
        root.setCenter(splitPane);
    
        primaryStage.setScene(scene);
    
        String savedTheme = configManager.getTheme();
        applyTheme(savedTheme);
    
        globalMediaKeyService.start();

        primaryStage.show();
    
        Platform.runLater(() -> {
            songListView.prefHeightProperty().bind(splitPane.heightProperty());
    
            String lastFolder = configManager.getLastFolder();
            File folder = (lastFolder != null && !lastFolder.isEmpty()) ? new File(lastFolder) : null;
    
            if (folder == null || !folder.isDirectory()) {
                promptForSongFolder(primaryStage);
            } else {
                handleFolderSelection(folder);
            }
    
            String lastSong = configManager.getLastSong();
            pendingLastSongSelection = (lastSong == null || lastSong.isBlank()) ? null : lastSong;
            lastSongSelectionApplied = false;
            attemptRestoreLastSongFromLibrary();
        });
    }

    private void setupGlobalShortcuts() {
        if (scene == null) {
            return;
        }

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isConsumed()) {
                return;
            }

            if (event.getCode() == KeyCode.TAB) {
                focusSearchField();
                event.consume();
                return;
            }

            if (shouldIgnoreShortcut(event)) {
                return;
            }

            if (shortcutManager.handleKeyEvent(event, this::executeShortcutAction)) {
                event.consume();
            }
        });
    }

    private void executeShortcutAction(ShortcutAction action) {
        if (action == null) {
            return;
        }
        switch (action) {
            case PREVIOUS_TRACK -> playPreviousFromHistory();
            case NEXT_TRACK -> playNextFromHistoryOrNormal();
            case PLAY_PAUSE -> playbackManager.togglePlayPause();
            case STOP -> playbackManager.stopPlayback();
            case VOLUME_MUTE -> playbackManager.toggleMuteWithMemory();
            case VOLUME_DOWN -> playbackManager.adjustVolumeByPercent(-5);
            case VOLUME_UP -> playbackManager.adjustVolumeByPercent(5);
            case SEEK_BACK_SHORT -> playbackManager.seekBySeconds(-10);
            case SEEK_FORWARD_SHORT -> playbackManager.seekBySeconds(10);
            case SEEK_BACK_LONG -> playbackManager.seekBySeconds(-30);
            case SEEK_FORWARD_LONG -> playbackManager.seekBySeconds(30);
            case SHUFFLE_TOGGLE -> playbackManager.toggleShuffle();
            case TEMPO_DOWN -> {
                playbackManager.adjustPlaybackRateBy(-0.05);
                showSpeedFeedback(playbackManager.getPlaybackRate());
            }
            case TEMPO_UP -> {
                playbackManager.adjustPlaybackRateBy(0.05);
                showSpeedFeedback(playbackManager.getPlaybackRate());
            }
            case DELETE_SONG -> deleteSelectedSong();
            default -> {
            }
        }
    }

    private void deleteSelectedSong() {
        String selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong == null || selectedSong.isBlank()) {
            return;
        }
        handleBeatmapDeletionFromLibrary(selectedSong);
    }

    private void showSpeedFeedback(double rate) {
        if (speedLabel == null) {
            return;
        }
        speedLabel.setText(I18n.tr("Velocidad") + " " + String.format(Locale.ROOT, "%.2fx", rate));
        speedLabel.setOpacity(1);
        if (speedHideDelay != null) {
            speedHideDelay.stop();
            speedHideDelay.playFromStart();
        }
    }

    private void focusSearchField() {
        if (searchField == null) {
            return;
        }
        searchField.requestFocus();
        searchField.selectAll();
    }

    private boolean shouldIgnoreShortcut(KeyEvent event) {
        if (event == null) {
            return true;
        }
        EventTarget target = event.getTarget();
        if (target instanceof TextInputControl textInput) {
            return textInput.isEditable();
        }
        return false;
    }
    
    private void applyTheme(String themeId) {
        boolean applied = ThemeHelper.applyTheme(scene, themeId);
        if (applied) {
            configManager.setTheme(themeId);
        } else if (themeId != null && !themeId.isBlank()) {
            System.err.println("No se pudo encontrar el archivo de tema: /themes/theme-" + themeId + ".css");
        }
        beatmapBrowserDialog.applyTheme(themeId);
        shortcutPreferencesDialog.applyTheme(themeId);
        settingsDialog.applyTheme(themeId);
    }

    private void refreshOpenWindowsLanguage() {
        updateNoSongPlaceholder(false);
        songListView.refresh();
        beatmapBrowserDialog.refreshLanguage();
        shortcutPreferencesDialog.refreshLanguage();
        settingsDialog.refreshLanguage();
    }
    
    private void promptForSongFolder(Stage ownerStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Selecciona tu carpeta de canciones de osu!");
        String userHome = System.getProperty("user.home");
        File defaultDir = new File(userHome, "AppData/Local/osu!/Songs");
        if (defaultDir.exists() && defaultDir.isDirectory()) {
            directoryChooser.setInitialDirectory(defaultDir);
        }
        File selectedDirectory = directoryChooser.showDialog(ownerStage);
        if (selectedDirectory != null) {
            handleFolderSelection(selectedDirectory);
        }
    }

    private void handleFolderSelection(File folder) {
        musicManager.setLastFolderPath(folder.getAbsolutePath());
        configManager.setLastFolder(folder.getAbsolutePath());
        loadSongs(folder, this::selectInitialPlaylist);
    }

    private void selectInitialPlaylist() {
        String desired = configManager.getLastPlaylist();
        if (desired == null || desired.isBlank() || !playlistManager.getAllPlaylists().contains(desired)) {
            desired = PlaylistManager.PLAYLIST_ALL;
        }
        selectPlaylist(desired);
    }

    private void refreshUIState() {
        String currentPlaylist = playlistHelper.getSelectedPlaylist();
        if (currentPlaylist != null) {
            loadPlaylistSongs(currentPlaylist);
        }
        updateFavoriteButton(currentSongLabel.getText());
        songListView.refresh();
    }

    private void refreshUIStatePreservingSelection() {
        runWithSelectionPreserved(this::refreshUIState);
    }

    private void runWithSelectionPreserved(Runnable task) {
        if (task == null) {
            return;
        }
        selectionFreezeDepth++;
        try {
            task.run();
        } finally {
            if (selectionFreezeDepth > 0) {
                selectionFreezeDepth--;
            }
        }
    }

    private boolean isSelectionFrozen() {
        return selectionFreezeDepth > 0;
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
        if (lastSong == null || lastSong.isEmpty() || masterSongList.isEmpty()) {
            return;
        }
        Platform.runLater(() -> {
            int index = masterSongList.indexOf(lastSong);
            if (index >= 0) {
                scrollToAndSelect(index);
                currentSongLabel.setText(lastSong);
                updateFavoriteButton(lastSong);
                updateCoverImage(lastSong);
                String path = musicManager.getSongPath(lastSong);
                if (path != null) {
                    playbackManager.onNewMedia();
                    audioPlayer.media().prepare(path);
                }
                playbackManager.updatePlayPauseButton(false);
                if (!configManager.isHistoryRetentionEnabled()) {
                    musicManager.clearHistory();
                    historyManager.clear();
                }
                musicManager.addToHistory(lastSong);
                playlistManager.setPlaylistSongs(PlaylistManager.PLAYLIST_HISTORY, new ArrayList<>(musicManager.getHistory()));
                synchronizeUiHistoryFromMusicManager();
                persistHistoryIfEnabled();
            }
        });
    }

    private void attemptRestoreLastSongFromLibrary() {
        if (lastSongSelectionApplied) {
            return;
        }
        if (pendingLastSongSelection == null || pendingLastSongSelection.isBlank()) {
            lastSongSelectionApplied = true;
            pendingLastSongSelection = null;
            return;
        }
        if (masterSongList.isEmpty()) {
            return;
        }
        if (!masterSongList.contains(pendingLastSongSelection)) {
            pendingLastSongSelection = null;
            lastSongSelectionApplied = true;
            return;
        }
        selectAndPreloadLastSong(pendingLastSongSelection);
        pendingLastSongSelection = null;
        lastSongSelectionApplied = true;
    }

    public void playNextFromHistoryOrNormal() {
        if (playQueuedSongIfAvailable()) {
            return;
        }
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
        if (!playlistManager.getAllPlaylists().contains(playlistName)) {
            playlistName = PlaylistManager.PLAYLIST_ALL;
        }
        playlistHelper.selectPlaylist(playlistName);
        configManager.setLastPlaylist(playlistName);
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
        songListView.refresh();
        Platform.runLater(songListView::layout);
        scrollToCurrentSong();
        attemptRestoreLastSongFromLibrary();
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
        if (isNoSongLabel(currentSong)) {
            return;
        }

        favoritesManager.toggleFavorite(currentSong);
        refreshUIState();
    }

    public void playNextSong() {
        if (playQueuedSongIfAvailable()) {
            return;
        }
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

    public void replayCurrentSongFromLoop() {
        String currentSong = currentSongLabel.getText();
        if (isNoSongLabel(currentSong) || currentSong == null || currentSong.isBlank()) {
            return;
        }
        playSong(currentSong, true);
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

        playbackManager.setCurrentSongForDiscord(songName);
        playbackManager.onNewMedia();

        audioPlayer.controls().stop();

        String videoPath = musicManager.getVideoPath(songName);
        if (videoPath != null && new File(videoPath).exists()) {
            long offset = musicManager.getVideoOffset(songName);
            videoSynchronizer.loadVideo(videoPath, offset);
        } else {
            videoSynchronizer.reset();
        }

        audioPlayer.media().play(songPath);

        currentSongLabel.setText(songName);
        updateFavoriteButton(songName);
        updateCoverImage(songName);
        
        playbackManager.updatePlayPauseButton(true);

        if (!fromHistory) {
            historyManager.addSong(songName);
            musicManager.addToHistory(songName);
            playlistManager.setPlaylistSongs(PlaylistManager.PLAYLIST_HISTORY, new ArrayList<>(musicManager.getHistory()));
            songListView.refresh();
            persistHistoryIfEnabled();
        } else {
            List<String> hm = historyManager.getHistory();
            int idx = hm.indexOf(songName);
            if (idx >= 0) historyManager.setIndex(idx);
        }

        configManager.setLastSong(songName);
        scrollToCurrentSong();
        refreshQueuePlaylistView();
    }

    private boolean playQueuedSongIfAvailable() {
        String queuedSong = playlistManager.pollQueue();
        if (queuedSong == null) {
            return false;
        }
        playSong(queuedSong, false);
        return true;
    }

    private void refreshQueuePlaylistView() {
        if (PlaylistManager.PLAYLIST_QUEUE.equals(playlistHelper.getSelectedPlaylist())) {
            loadPlaylistSongs(PlaylistManager.PLAYLIST_QUEUE);
        }
    }

    private void scrollToCurrentSong() {
        if (isSelectionFrozen()) {
            return;
        }
        Platform.runLater(() -> {
            if (isSelectionFrozen()) {
                return;
            }
            String currentSong = currentSongLabel.getText();
            if (currentSong != null && filteredSongList != null && !filteredSongList.isEmpty()) {
                int index = filteredSongList.indexOf(currentSong);
                scrollToAndSelect(index);
            }
        });
    }

    private void scrollToAndSelect(int index) {
        if (index < 0) return;

        int scrollToIndex = Math.max(0, index - 5); 
        songListView.scrollTo(scrollToIndex);

        Platform.runLater(() -> {
            songListView.getSelectionModel().select(index);
            songListView.getFocusModel().focus(index);
            songListView.requestFocus();
        });
    }

    public void highlightCurrentSong() {
        Platform.runLater(() -> {
            String currentSong = currentSongLabel.getText();
            if (currentSong == null || filteredSongList == null) {
                return;
            }
            int index = filteredSongList.indexOf(currentSong);
            if (index >= 0) {
                songListView.getSelectionModel().select(index);
                songListView.getFocusModel().focus(index);
                songListView.requestFocus();
            }
        });
    }

    private void loadSongs(File folder, Runnable onSuccess) {
        String loadingTitle = I18n.tr("Cargando canciones");
        ProgressDialog loadingDialog = new ProgressDialog(scene == null ? null : scene.getWindow(),
            loadingTitle,
            loadingTitle,
            configManager.getTheme());
        Task<Map<String, String>> loadTask = new Task<>() {
            @Override
            protected Map<String, String> call() {
                return musicManager.loadSongsFromFolder(folder, loadingDialog::updateProgress);
            }
        };

        loadTask.setOnRunning(evt -> loadingDialog.show());
        loadTask.setOnSucceeded(evt -> {
            loadingDialog.updateProgress(1.0, I18n.tr("Completado"));
            loadingDialog.close();
            Map<String, String> loadedSongs = loadTask.getValue();
            Platform.runLater(() -> {
                applyLoadedSongs(loadedSongs);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            });
        });
        loadTask.setOnFailed(evt -> {
            loadingDialog.close();
            Throwable error = loadTask.getException();
            showAlert(Alert.AlertType.ERROR, I18n.tr("Error al cargar canciones"), error == null ? I18n.tr("Causa desconocida") : error.getMessage());
        });

        Thread loader = new Thread(loadTask, "osu-song-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void applyLoadedSongs(Map<String, String> loadedSongs) {
        if (loadedSongs == null) {
            return;
        }
        Set<String> allSongs = loadedSongs.keySet();

        playlistManager.setPlaylistSongs(PlaylistManager.PLAYLIST_ALL, new ArrayList<>(allSongs));

        for (String playlistName : playlistManager.getAllPlaylists()) {
            if (!playlistManager.isSpecialPlaylist(playlistName)) {
                List<String> currentSongs = new ArrayList<>(playlistManager.getPlaylist(playlistName));
                currentSongs.removeIf(song -> !allSongs.contains(song));
                playlistManager.setPlaylistSongs(playlistName, currentSongs);
            }
        }

        favoritesManager.validateFavorites(allSongs);

        List<String> currentHistory = new ArrayList<>(musicManager.getHistory());
        boolean removedFromHistory = currentHistory.removeIf(song -> !allSongs.contains(song));
        if (removedFromHistory) {
            int currentIndex = musicManager.getHistoryIndex();
            int adjustedIndex = currentHistory.isEmpty() ? -1 : Math.min(Math.max(currentIndex, 0), currentHistory.size() - 1);
            musicManager.setHistory(currentHistory, adjustedIndex);
        }
        playlistManager.setPlaylistSongs(PlaylistManager.PLAYLIST_HISTORY, new ArrayList<>(musicManager.getHistory()));

        playlistManager.pruneQueue(allSongs);
        refreshQueuePlaylistView();

        synchronizeUiHistoryFromMusicManager();
        persistHistoryIfEnabled();

        playlistManager.savePlaylists();
        playlistHelper.refreshPlaylistList();
    }

    private void reloadCurrentFolder() {
        String lastFolder = configManager.getLastFolder();
        if (lastFolder == null || lastFolder.isBlank()) return;
        File folder = new File(lastFolder);
        if (!folder.isDirectory()) return;

        final String selectedPlaylist = playlistHelper.getSelectedPlaylist();
        loadSongs(folder, () -> {
            if (selectedPlaylist != null && playlistManager.getAllPlaylists().contains(selectedPlaylist)) {
                selectPlaylist(selectedPlaylist);
            } else {
                selectInitialPlaylist();
            }
        });
    }

    private void handleLibraryUpdate(Path importedFolder) {
        if (importedFolder == null) {
            reloadCurrentFolder();
            return;
        }
        File folder = importedFolder.toFile();
        if (!folder.exists() || !folder.isDirectory()) {
            reloadCurrentFolder();
            return;
        }
        List<String> addedSongs = musicManager.importBeatmapFolder(folder);
        if (addedSongs.isEmpty()) {
            return;
        }
        for (String song : addedSongs) {
            playlistManager.addToPlaylist(PlaylistManager.PLAYLIST_ALL, song);
        }
        playlistHelper.refreshPlaylistList();
        refreshUIState();
    }

    private void applyRemovedSongs(List<String> removedSongs) {
        runWithSelectionPreserved(() -> {
            if (removedSongs == null || removedSongs.isEmpty()) {
                refreshUIState();
                return;
            }
            for (String song : removedSongs) {
                favoritesManager.removeFavorite(song);
            }
            playlistManager.removeSongsFromAllPlaylists(removedSongs);
            pruneUiHistory(removedSongs);
            playlistHelper.refreshPlaylistList();
            refreshQueuePlaylistView();
            refreshUIState();
            persistHistoryIfEnabled();
        });
    }

    private void pruneUiHistory(List<String> removedSongs) {
        if (removedSongs == null || removedSongs.isEmpty()) {
            return;
        }
        List<String> historyEntries = historyManager.getHistory();
        if (historyEntries.isEmpty()) {
            return;
        }
        List<String> updated = new ArrayList<>(historyEntries);
        if (!updated.removeAll(removedSongs)) {
            return;
        }
        int currentIndex = historyManager.getIndex();
        int newIndex = updated.isEmpty() ? -1 : Math.min(Math.max(0, currentIndex), updated.size() - 1);
        historyManager.setHistory(updated, newIndex);
    }

    private void handleHistoryRetentionPreferenceChanged(boolean enabled) {
        if (enabled) {
            persistHistoryIfEnabled();
        } else {
            configManager.clearStoredHistory();
        }
    }

    private void restorePersistentHistoryIfEnabled() {
        if (!configManager.isHistoryRetentionEnabled()) {
            configManager.clearStoredHistory();
            return;
        }
        List<String> storedHistory = configManager.getPlayHistory();
        if (storedHistory == null || storedHistory.isEmpty()) {
            return;
        }
        List<String> historySnapshot = new ArrayList<>(storedHistory);
        int storedIndex = configManager.getHistoryIndex();
        int clampedIndex = historySnapshot.isEmpty()
            ? -1
            : Math.min(Math.max(storedIndex, 0), historySnapshot.size() - 1);
        musicManager.setHistory(historySnapshot, clampedIndex);
        playlistManager.setPlaylistSongs(PlaylistManager.PLAYLIST_HISTORY, new ArrayList<>(historySnapshot));
        synchronizeUiHistoryFromMusicManager();
    }

    private void synchronizeUiHistoryFromMusicManager() {
        List<String> historyCopy = new ArrayList<>(musicManager.getHistory());
        Collections.reverse(historyCopy);
        int index = historyCopy.isEmpty() ? -1 : historyCopy.size() - 1;
        historyManager.setHistory(historyCopy, index);
    }

    private void persistHistoryIfEnabled() {
        if (!configManager.isHistoryRetentionEnabled()) {
            return;
        }
        List<String> historySnapshot = new ArrayList<>(musicManager.getHistory());
        configManager.setPlayHistory(historySnapshot);
        configManager.setHistoryIndex(musicManager.getHistoryIndex());
    }

    private void handleUpdateCheck() {
        Window owner = scene == null ? null : scene.getWindow();
        updateService.checkForUpdates(owner, configManager.getTheme());
    }

    private void showAboutDialog() {
        Window owner = scene == null ? null : scene.getWindow();
        AboutDialog.show(owner, musicManager, configManager);
    }

    private void handleBeatmapDeletionFromLibrary(String songDisplayName) {
        if (songDisplayName == null) {
            return;
        }
        Window owner = scene == null ? null : scene.getWindow();
        if (!beatmapDeletionHelper.confirmDeletionIfNeeded(owner)) {
            return;
        }
        final Path folder;
        try {
            String folderPath = musicManager.getSongBaseFolder(songDisplayName);
            folder = beatmapDeletionHelper.validateExistingFolder(folderPath);
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "No se pudo eliminar el beatmap", ex.getMessage());
            return;
        }

        if (isDeletingCurrentSong(folder, songDisplayName)) {
            playbackManager.stopPlayback(false);
            clearCurrentSongState();
        }

        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                beatmapDeletionHelper.deleteFolder(folder);
                return null;
            }
        };

        deleteTask.setOnSucceeded(evt -> {
            List<String> removedSongs = musicManager.removeSongsByFolder(folder);
            applyRemovedSongs(removedSongs);
            showAlert(Alert.AlertType.INFORMATION, "Beatmap eliminado", "Se eliminó \"" + songDisplayName + "\".");
        });
        deleteTask.setOnFailed(evt -> {
            Throwable ex = deleteTask.getException();
            showAlert(Alert.AlertType.ERROR, "No se pudo eliminar el beatmap", ex == null ? "Causa desconocida" : ex.getMessage());
        });

        Thread worker = new Thread(deleteTask, "library-beatmap-delete");
        worker.setDaemon(true);
        worker.start();
    }

    private boolean isDeletingCurrentSong(Path folderToDelete, String songDisplayName) {
        if (folderToDelete == null) {
            return false;
        }
        String currentSong = currentSongLabel.getText();
        if (isNoSongLabel(currentSong)) {
            return false;
        }
        if (currentSong.equals(songDisplayName)) {
            return true;
        }
        String currentFolderPath = musicManager.getSongBaseFolder(currentSong);
        if (currentFolderPath == null) {
            return false;
        }
        try {
            Path currentFolder = Path.of(currentFolderPath).normalize();
            return currentFolder.equals(folderToDelete.normalize());
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    private void clearCurrentSongState() {
        showNoSongText();
        favoriteButton.setText("♡");
        songListView.getSelectionModel().clearSelection();
        updateCoverImage(null);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (scene != null && scene.getWindow() != null) {
                alert.initOwner(scene.getWindow());
            }
            alert.showAndWait();
        });
    }

    private void updateCoverImage(String songName) {
        Image coverImage = coverManager.getCoverImage(songName);
        coverImageView.setImage(coverImage);
    }

    public void hideVideo() {
        videoVisibilityHelper.hideVideo();
    }

    public void shutdown() {
        persistHistoryIfEnabled();
        globalMediaKeyService.close();
        beatmapBrowserDialog.shutdown();
    }
}