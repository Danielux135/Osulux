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

    /**
     * Alterna entre reproducir y pausar la música y el vídeo sincronizadamente.
     * Ejecuta los callbacks correspondientes para actualizar la interfaz de usuario.
     * 
     * @param onPlay  Una acción a ejecutar cuando la reproducción comienza o se reanuda.
     * @param onPause Una acción a ejecutar cuando la reproducción se pausa.
     */
    public void togglePlayPause(Runnable onPlay, Runnable onPause) {
        State state = audioPlayer.status().state();

        if (state == State.PLAYING) {
            audioPlayer.controls().pause();
            videoPlayer.controls().pause();
            onPause.run();
        }
        else {
            audioPlayer.controls().play();
            videoPlayer.controls().play();
            onPlay.run();
        }
    }
}