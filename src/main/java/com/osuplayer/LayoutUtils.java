package com.osuplayer;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class LayoutUtils {

    private static final double MIN_PLAYLIST_WIDTH = 180;
    private static final double MIN_SONGS_WIDTH = 400;
    private static final double MIN_RIGHT_PANEL_WIDTH = 170;
    private static double lastDivider0 = 0.15;
    private static double lastDivider1 = 0.75;

    private LayoutUtils() {}

    public static void setupDynamicSplitPane(SplitPane splitPane, Node leftPane, Node middlePane, Node rightPane) {
        splitPane.setDividerPositions(lastDivider0, lastDivider1);

        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            if (!splitPane.isPressed()) return;
            updateDividerPositions(splitPane, leftPane, middlePane);
        });

        splitPane.getDividers().get(1).positionProperty().addListener((obs, oldVal, newVal) -> {
            if (!splitPane.isPressed()) return;
            updateDividerPositions(splitPane, leftPane, middlePane);
        });

        splitPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                double total = newVal.doubleValue();
                if (total <= 0) return;

                double leftPref = ((Region) leftPane).getPrefWidth();
                double middlePref = ((Region) middlePane).getPrefWidth();
                
                leftPref = Math.max(MIN_PLAYLIST_WIDTH, leftPref);
                middlePref = Math.max(MIN_SONGS_WIDTH, middlePref);
                
                if (leftPref + middlePref + MIN_RIGHT_PANEL_WIDTH > total) {
                    double overflow = leftPref + middlePref + MIN_RIGHT_PANEL_WIDTH - total;
                    if (middlePref - overflow >= MIN_SONGS_WIDTH) {
                        middlePref = middlePref - overflow;
                    } else {
                        double rem = overflow - (middlePref - MIN_SONGS_WIDTH);
                        middlePref = MIN_SONGS_WIDTH;
                        leftPref = Math.max(MIN_PLAYLIST_WIDTH, leftPref - rem);
                    }
                }
                
                lastDivider0 = Math.min(0.49, Math.max(0.01, leftPref / total));
                lastDivider1 = Math.min(0.99, Math.max(lastDivider0 + 0.01, (leftPref + middlePref) / total));
                
                splitPane.setDividerPositions(lastDivider0, lastDivider1);
            });
        });
    }

    private static void updateDividerPositions(SplitPane splitPane, Node leftPane, Node middlePane) {
        double total = splitPane.getWidth();
        if (total <= 0) return;

        double d0 = splitPane.getDividers().get(0).getPosition();
        double d1 = splitPane.getDividers().get(1).getPosition();

        double leftWidth = d0 * total;
        double middleWidth = (d1 - d0) * total;
        
        leftWidth = Math.max(MIN_PLAYLIST_WIDTH, Math.min(leftWidth, total - MIN_SONGS_WIDTH - MIN_RIGHT_PANEL_WIDTH));
        middleWidth = Math.max(MIN_SONGS_WIDTH, Math.min(middleWidth, total - leftWidth - MIN_RIGHT_PANEL_WIDTH));

        ((Region) leftPane).setPrefWidth(leftWidth);
        ((Region) middlePane).setPrefWidth(middleWidth);

        lastDivider0 = leftWidth / total;
        lastDivider1 = (leftWidth + middleWidth) / total;
        splitPane.setDividerPositions(lastDivider0, lastDivider1);
    }

    public static void updateMediaDisplaySize(
            double containerWidth, 
            double containerHeight, 
            ImageView coverImageView, 
            ImageView videoImageView, 
            StackPane mediaDisplayStack,
            boolean isVideoVisible) {

        if (containerWidth <= 0 || containerHeight <= 0) return;
        
        double availableWidth = Math.max(1, mediaDisplayStack.getWidth());
        double availableHeight = Math.max(1, mediaDisplayStack.getHeight());

        Image img = isVideoVisible ? videoImageView.getImage() : coverImageView.getImage();
        
        coverImageView.setFitWidth(availableWidth);
        coverImageView.setFitHeight(availableHeight);
        videoImageView.setFitWidth(availableWidth);
        videoImageView.setFitHeight(availableHeight);

        if (img == null) {
            mediaDisplayStack.setPrefSize(availableWidth, availableHeight);
        }
    }
}