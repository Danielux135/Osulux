package com.osuplayer;

import java.util.Random;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class PlaybackManager {

    private final EmbeddedMediaPlayer audioPlayer;
    private final EmbeddedMediaPlayer videoPlayer;
    private final ConfigManager configManager;
    private final UIController uiController;

    private Slider progressSlider;
    private Label timeLabel;
    private Button playPauseButton;
    private Button shuffleButton;

    private boolean isSeeking = false;
    private boolean shuffleEnabled = false;
    private final Random random = new Random();
    private long currentMediaDuration = -1;

    public PlaybackManager(EmbeddedMediaPlayer audioPlayer, EmbeddedMediaPlayer videoPlayer, ConfigManager configManager, UIController uiController) {
        this.audioPlayer = audioPlayer;
        this.videoPlayer = videoPlayer;
        this.configManager = configManager;
        this.uiController = uiController;
    }

    public void initializeControls(Slider progressSlider, Label timeLabel, Slider volumeSlider, Button playPauseButton, Button shuffleButton, Button previousButton, Button stopButton, Button nextButton) {
        this.progressSlider = progressSlider;
        this.timeLabel = timeLabel;
        this.playPauseButton = playPauseButton;
        this.shuffleButton = shuffleButton;

        setupAudioPlayerListeners();
        setupUIControlListeners(volumeSlider, previousButton, stopButton, nextButton);
        updateShuffleButtonStyle();
    }

    private void setupAudioPlayerListeners() {
        audioPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                if (!isSeeking && currentMediaDuration > 0) {
                    Platform.runLater(() -> {
                        progressSlider.setValue(newTime / 1000.0);
                        updateTimeLabel(newTime / 1000.0, currentMediaDuration / 1000.0);
                    });
                }
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Platform.runLater(uiController::playNextSong);
            }

            @Override
            public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
                currentMediaDuration = newLength;
                Platform.runLater(() -> {
                    if (newLength > 0) {
                        progressSlider.setMax(newLength / 1000.0);
                    }
                });
            }
        });
    }
    
    public void onNewMedia() {
        currentMediaDuration = -1;
        Platform.runLater(() -> {
            progressSlider.setValue(0);
            progressSlider.setMax(0);
            updateTimeLabel(0, 0);
        });
    }

    private void setupUIControlListeners(Slider volumeSlider, Button previousButton, Button stopButton, Button nextButton) {
        progressSlider.setOnMousePressed(e -> isSeeking = true);
        progressSlider.setOnMouseReleased(e -> {
            isSeeking = false;
            long time = (long) (progressSlider.getValue() * 1000);
            audioPlayer.controls().setTime(time);
            if (videoPlayer.status().isPlaying()) {
                videoPlayer.controls().setTime(time);
            }
        });

        volumeSlider.setValue(configManager.getVolume() * 100);
        audioPlayer.audio().setVolume((int) volumeSlider.getValue());
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            audioPlayer.audio().setVolume(newVal.intValue());
            configManager.setVolume(newVal.doubleValue() / 100.0);
        });

        playPauseButton.setOnAction(e -> togglePlayPause());
        shuffleButton.setOnAction(e -> toggleShuffle());
        previousButton.setOnAction(e -> uiController.playPreviousFromHistory());
        stopButton.setOnAction(e -> stopPlayback());
        nextButton.setOnAction(e -> uiController.playNextFromHistoryOrNormal());
    }

    public void togglePlayPause() {
        State state = audioPlayer.status().state();
        if (state == State.PLAYING) {
            audioPlayer.controls().pause();
            videoPlayer.controls().pause();
            playPauseButton.setText("▶");
        } else {
            if (state == State.STOPPED || state == State.ENDED || state == null) {
                 uiController.playSelectedSong();
            } else {
                audioPlayer.controls().play();
                videoPlayer.controls().play();
                playPauseButton.setText("⏸");
            }
        }
    }

    public void stopPlayback() {
        audioPlayer.controls().stop();
        videoPlayer.controls().stop();
        playPauseButton.setText("▶");
        onNewMedia();
        uiController.hideVideo();
        uiController.prepareCurrentSongForReplay();
    }

    public void updatePlayPauseButton(boolean isPlaying) {
        playPauseButton.setText(isPlaying ? "⏸" : "▶");
    }

    public void setProgressMax(double maxSeconds) {
        if(maxSeconds > 0) {
            progressSlider.setMax(maxSeconds);
        }
    }

    private void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;
        updateShuffleButtonStyle();
    }

    private void updateShuffleButtonStyle() {
        shuffleButton.setStyle(shuffleEnabled ? "-fx-background-color: #00cc00;" : "");
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public int getRandomIndex(int bound) {
        if (bound <= 0) return 0;
        return random.nextInt(bound);
    }

    private void updateTimeLabel(double currentSeconds, double totalSeconds) {
        timeLabel.setText(formatTime(currentSeconds) + " / " + formatTime(totalSeconds));
    }

    private String formatTime(double seconds) {
        if (Double.isNaN(seconds) || seconds < 0) seconds = 0;
        int s = (int) Math.round(seconds);
        int mins = s / 60;
        int secs = s % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}