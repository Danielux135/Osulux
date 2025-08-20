package com.osuplayer;

import javafx.scene.image.ImageView;

public class VideoVisibilityHelper {

    private final ImageView videoImageView;
    private final ImageView coverImageView;

    public VideoVisibilityHelper(ImageView videoImageView, ImageView coverImageView) {
        this.videoImageView = videoImageView;
        this.coverImageView = coverImageView;
        
        // Estado inicial
        hideVideo();
    }

    public void showVideo() {
        if (videoImageView != null) {
            videoImageView.setVisible(true);
        }
        if (coverImageView != null) {
            coverImageView.setVisible(false);
        }
    }

    public void hideVideo() {
        if (videoImageView != null) {
            videoImageView.setVisible(false);
        }
        if (coverImageView != null) {
            coverImageView.setVisible(true);
        }
    }

    public boolean isVideoVisible() {
        return videoImageView != null && videoImageView.isVisible();
    }
}