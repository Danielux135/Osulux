package com.osuplayer;

import javafx.scene.control.Button;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public final class PlayToggleHelper {
    private PlayToggleHelper() {}

    public static void toggle(EmbeddedMediaPlayer audioPlayer,
                              EmbeddedMediaPlayer videoPlayer,
                              Button playPauseButton) {
        if (audioPlayer == null) return;
        State s = audioPlayer.status().state();
        if (s == State.PLAYING) {
            audioPlayer.controls().pause();
            if (videoPlayer != null) {
                videoPlayer.controls().pause();
            }
            playPauseButton.setText("\u25B6"); // ▶
        } else {
            audioPlayer.controls().play();
            if (videoPlayer != null) {
                videoPlayer.controls().play();
            }
            playPauseButton.setText("\u23F8"); // ⏸
        }
    }
}