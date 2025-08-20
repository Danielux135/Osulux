package com.osuplayer;

import java.io.File;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public final class UIHelper {

    private UIHelper() {}

    public record TopBarComponents(HBox bar, TextField searchField) {}
    public record ControlBarComponents(VBox bar, Slider progressSlider, Label timeLabel, Slider volumeSlider, Button previousButton, Button playPauseButton, Button stopButton, Button nextButton, Button shuffleButton) {}

    public static TopBarComponents createTopBar(Stage ownerStage, Consumer<File> onFolderChosen) {
        Button chooseFolderButton = new Button("Abrir carpeta Songs");
        chooseFolderButton.setMaxWidth(Double.MAX_VALUE);

        TextField searchField = new TextField();
        searchField.setPromptText("Buscar canciones, artistas, creadores o tags...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        chooseFolderButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Selecciona la carpeta de canciones de OSU!");
            String userHome = System.getProperty("user.home");
            File defaultDir = new File(userHome, "AppData/Local/osu!/Songs");
            if (defaultDir.exists() && defaultDir.isDirectory()) {
                directoryChooser.setInitialDirectory(defaultDir);
            }
            File selectedDirectory = directoryChooser.showDialog(ownerStage);
            if (selectedDirectory != null) {
                onFolderChosen.accept(selectedDirectory);
            }
        });

        HBox topBox = new HBox(10, chooseFolderButton, searchField);
        topBox.setPadding(new Insets(10));
        topBox.setAlignment(Pos.CENTER_LEFT);

        return new TopBarComponents(topBox, searchField);
    }
    
    public static VBox createMediaPanel(StackPane mediaDisplayStack, Label currentSongLabel, Button favoriteButton) {
        currentSongLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        currentSongLabel.setWrapText(true);
        currentSongLabel.setAlignment(Pos.CENTER);

        VBox coverBox = new VBox(10);
        coverBox.setAlignment(Pos.TOP_CENTER);
        
        StackPane.setAlignment(mediaDisplayStack, Pos.CENTER);
        coverBox.getChildren().add(mediaDisplayStack);

        VBox titleFavoriteBox = new VBox(5);
        titleFavoriteBox.setAlignment(Pos.CENTER);
        titleFavoriteBox.getChildren().addAll(currentSongLabel, favoriteButton);
        coverBox.getChildren().add(titleFavoriteBox);
        
        VBox.setVgrow(mediaDisplayStack, Priority.ALWAYS);
        coverBox.setPadding(new Insets(10));

        VBox mediaContainer = new VBox(coverBox);
        mediaContainer.setPadding(new Insets(10));
        VBox.setVgrow(coverBox, Priority.ALWAYS);

        return mediaContainer;
    }

    public static ControlBarComponents createControlBar() {
        Slider progressSlider = new Slider(0, 0, 0);
        Label timeLabel = new Label("00:00 / 00:00");
        Slider volumeSlider = new Slider(0, 100, 50);
        Button previousButton = createControlButton("⏮");
        Button playPauseButton = createControlButton("▶");
        Button stopButton = createControlButton("⏹");
        Button nextButton = createControlButton("⏭");
        Button shuffleButton = createControlButton("🔀");
        
        HBox controlBox = new HBox(10, previousButton, playPauseButton, stopButton, nextButton, shuffleButton, volumeSlider);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setPadding(new Insets(5, 10, 5, 10));

        HBox progressBox = new HBox(10, progressSlider, timeLabel);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(0, 10, 5, 10));
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        
        VBox bottomBox = new VBox(controlBox, progressBox);

        return new ControlBarComponents(bottomBox, progressSlider, timeLabel, volumeSlider, previousButton, playPauseButton, stopButton, nextButton, shuffleButton);
    }

    private static Button createControlButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(40);
        btn.setPrefHeight(30);
        btn.setFocusTraversable(false);
        return btn;
    }
}