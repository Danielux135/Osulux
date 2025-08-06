package com.osuplayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javafx.scene.image.Image;

/**
 * Encargado de obtener la imagen de portada de las canciones.
 */
public class CoverManager {

    private final MusicManager musicManager;

    public CoverManager(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    /**
     * Devuelve la imagen de portada de una canción. Si no existe, devuelve la imagen por defecto.
     * @param songName nombre de la canción
     * @return Image de portada
     */
    public Image getCoverImage(String songName) {
        String coverPath = musicManager.getCoverImagePath(songName);
        if (coverPath != null) {
            File coverFile = new File(coverPath);
            if (coverFile.exists()) {
                return new Image(coverFile.toURI().toString(), 170, 170, true, true);
            }
        }
        return getDefaultCover();
    }

    /**
     * Devuelve la imagen de portada por defecto.
     * @return Image por defecto
     */
    public Image getDefaultCover() {
        try (InputStream is = getClass().getResourceAsStream("/default_cover.jpg")) {
            if (is != null) {
                return new Image(is, 170, 170, true, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
