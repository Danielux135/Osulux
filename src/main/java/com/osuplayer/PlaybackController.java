package com.osuplayer;

import java.util.List;
import java.util.Random;

import uk.co.caprica.vlcj.player.base.MediaPlayer;

public class PlaybackController {

    private final MediaPlayer mediaPlayer;
    private String currentSongPath;
    private boolean shuffle = false;
    private final Random random = new Random();

    public PlaybackController(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public void playSong(String songPath) {
        if (songPath == null) return;
        if (mediaPlayer.status().isPlaying()) {
            mediaPlayer.controls().stop();
        }
        currentSongPath = songPath;
        mediaPlayer.media().start(songPath);
    }

    public void pause() {
        mediaPlayer.controls().pause();
    }

    public void stop() {
        mediaPlayer.controls().stop();
        currentSongPath = null;
    }

    public void toggleShuffle() {
        shuffle = !shuffle;
    }

    public boolean isShuffleEnabled() {
        return shuffle;
    }

    public String getCurrentSong() {
        return currentSongPath;
    }

    public void playNext(List<String> playlist) {
        if (playlist == null || playlist.isEmpty()) return;
        int nextIndex;
        if (shuffle) {
            nextIndex = random.nextInt(playlist.size());
        } else {
            int currentIndex = playlist.indexOf(currentSongPath);
            nextIndex = (currentIndex + 1) % playlist.size();
        }
        String nextSong = playlist.get(nextIndex);
        playSong(nextSong);
    }

    public void playPrevious(List<String> playlist) {
        if (playlist == null || playlist.isEmpty()) return;
        int prevIndex;
        if (shuffle) {
            prevIndex = random.nextInt(playlist.size());
        } else {
            int currentIndex = playlist.indexOf(currentSongPath);
            prevIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        }
        String prevSong = playlist.get(prevIndex);
        playSong(prevSong);
    }
}
