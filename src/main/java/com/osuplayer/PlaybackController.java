package com.osuplayer;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.State;

public class PlaybackController {

    private final MediaPlayer mediaPlayer;
    private String currentMediaPath;

    public PlaybackController(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public void play(String mediaPath, double startSeconds) {
        if (mediaPath == null) return;

        if (!mediaPath.equals(currentMediaPath)) {
            currentMediaPath = mediaPath;
            mediaPlayer.controls().stop();
            mediaPlayer.media().startPaused(mediaPath);

            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
                long length = mediaPlayer.status().length();
                if (length > 0) {
                    mediaPlayer.controls().setTime((long) (startSeconds * 1000));
                    mediaPlayer.controls().play();
                }
            }).start();
        } else {
            State state = mediaPlayer.status().state();
            if (state == State.PAUSED) {
                mediaPlayer.controls().play();
            } else if (state == State.STOPPED) {
                mediaPlayer.media().startPaused(mediaPath);
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                    mediaPlayer.controls().setTime((long) (startSeconds * 1000));
                    mediaPlayer.controls().play();
                }).start();
            }
        }
    }

    public void pause() {
        if (isPlaying()) {
            mediaPlayer.controls().pause();
        }
    }

    public void play() {
        State state = mediaPlayer.status().state();
        if (state == State.PAUSED) {
            mediaPlayer.controls().play();
        } else if (state == State.STOPPED && currentMediaPath != null) {
            play(currentMediaPath, 0);
        }
    }

    public void stop() {
        if (!isStopped()) {
            mediaPlayer.controls().stop();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.status().state() == State.PLAYING;
    }

    public boolean isPaused() {
        return mediaPlayer.status().state() == State.PAUSED;
    }

    public boolean isStopped() {
        return mediaPlayer.status().state() == State.STOPPED;
    }

    public void setCurrentPosition(double seconds) {
        mediaPlayer.controls().setTime((long) (seconds * 1000));
    }

    public String getCurrentMediaPath() {
        return currentMediaPath;
    }
}
