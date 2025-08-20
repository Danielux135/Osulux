package com.osuplayer;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.State;

public class PlaybackHelper {

    private final MediaPlayer audioPlayer;
    private final MediaPlayer videoPlayer;

    public PlaybackHelper(MediaPlayer audioPlayer, MediaPlayer videoPlayer) {
        this.audioPlayer = audioPlayer;
        this.videoPlayer = videoPlayer;
    }

    public void togglePlayPause(Runnable onPlay, Runnable onPause) {
        State state = audioPlayer.status().state();
        if (state == null || state == State.STOPPED || state == State.ENDED) {
            audioPlayer.controls().play();
            videoPlayer.controls().pause();
            onPlay.run();
        } else if (state == State.PLAYING) {
            audioPlayer.controls().pause();
            videoPlayer.controls().pause();
            onPause.run();
        } else {
            audioPlayer.controls().play();
            videoPlayer.controls().pause();
            onPlay.run();
        }
    }
}
