package com.osuplayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javafx.scene.image.Image;

public class CoverManager {

    private final MusicManager musicManager;

    public CoverManager(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    public Image getCoverImage(String songName) {
        String coverPath = musicManager.getCoverImagePath(songName);
        if (coverPath != null) {
            File coverFile = new File(coverPath);
            if (coverFile.exists()) {
                // Carga la imagen en tamaño original, con preservación de ratio y suavizado
                return new Image(coverFile.toURI().toString(), 0, 0, true, true);
            }
        }
        return getDefaultCover();
    }

    public Image getDefaultCover() {
        try (InputStream is = getClass().getResourceAsStream("/default_cover.jpg")) {
            if (is != null) {
                // Carga la imagen por defecto con preservación de ratio y suavizado
                return new Image(is, 0, 0, true, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
