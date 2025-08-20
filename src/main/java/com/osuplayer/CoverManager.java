package com.osuplayer;

import java.io.File;
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
                return new Image(coverFile.toURI().toString(), 0, 0, true, true);
            }
        }
        return getDefaultCover();
    }

    public Image getDefaultCover() {
        InputStream is = getClass().getResourceAsStream("/default_cover.jpg");
        if (is != null) {
            return new Image(is, 0, 0, true, true);
        }
        // Devuelve una imagen vacía de 1x1 px para no romper la UI
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIW2P8z/C/HwAF/gL+gOsk2QAAAABJRU5ErkJggg==");
    }
}
