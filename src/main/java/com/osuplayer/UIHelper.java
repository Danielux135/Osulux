package com.osuplayer;

import java.io.File;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public final class UIHelper {

    private UIHelper() {}

    public record TopBarComponents(HBox bar, TextField searchField, ToggleGroup themeToggleGroup) {}
    public record ControlBarComponents(VBox bar, Slider progressSlider, Label timeLabel, Slider volumeSlider, Button previousButton, Button playPauseButton, Button stopButton, Button nextButton, Button shuffleButton) {}

    public static TopBarComponents createTopBar(Stage ownerStage, Consumer<File> onFolderChosen, Consumer<String> onThemeChosen) {
        MenuItem openFolderItem = new MenuItem("Abrir carpeta de canciones...");
        openFolderItem.setOnAction(e -> {
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

        Menu themeMenu = new Menu("Elegir tema");
        ToggleGroup themeToggleGroup = new ToggleGroup();

        // --- INICIO DE LA MODIFICACIÓN ---
        // Temas Claros
        RadioMenuItem lightThemeItem = createThemeMenuItem("Blanco", "light", themeToggleGroup, onThemeChosen);
        RadioMenuItem blueThemeItem = createThemeMenuItem("Azul claro", "blue", themeToggleGroup, onThemeChosen);
        RadioMenuItem lightRedThemeItem = createThemeMenuItem("Rojo claro", "light-red", themeToggleGroup, onThemeChosen);
        RadioMenuItem orangeThemeItem = createThemeMenuItem("Naranja claro", "light-orange", themeToggleGroup, onThemeChosen);
        
        // Temas Oscuros
        RadioMenuItem darkThemeItem = createThemeMenuItem("Oscuro", "dark", themeToggleGroup, onThemeChosen);
        // Se actualiza el ID a "orange"
        RadioMenuItem naranjaThemeItem = createThemeMenuItem("Naranja", "orange", themeToggleGroup, onThemeChosen);
        RadioMenuItem redThemeItem = createThemeMenuItem("Rojo", "red", themeToggleGroup, onThemeChosen);
        RadioMenuItem greenThemeItem = createThemeMenuItem("Verde", "green", themeToggleGroup, onThemeChosen);


        // Se actualiza la lista del menú
        themeMenu.getItems().addAll(
            lightThemeItem, blueThemeItem, lightRedThemeItem, orangeThemeItem, 
            new SeparatorMenuItem(), 
            darkThemeItem, naranjaThemeItem, redThemeItem, greenThemeItem
        );
        // --- FIN DE LA MODIFICACIÓN ---

        Menu optionsMenu = new Menu("Opciones");
        optionsMenu.setStyle("-fx-font-weight: bold;");
        optionsMenu.getItems().addAll(openFolderItem, new SeparatorMenuItem(), themeMenu);

        MenuBar menuBar = new MenuBar(optionsMenu);

        TextField searchField = new TextField();
        searchField.setPromptText("Buscar canciones, artistas, creadores o tags...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox topBox = new HBox(10, menuBar, searchField);
        topBox.setPadding(new Insets(10));
        topBox.setAlignment(Pos.CENTER_LEFT);

        return new TopBarComponents(topBox, searchField, themeToggleGroup);
    }

    private static RadioMenuItem createThemeMenuItem(String text, String themeId, ToggleGroup group, Consumer<String> onThemeChosen) {
        RadioMenuItem item = new RadioMenuItem(text);
        item.setToggleGroup(group);
        item.setUserData(themeId);
        item.setOnAction(e -> onThemeChosen.accept(themeId));
        return item;
    }

    public static BorderPane createMediaPanel(StackPane mediaDisplayStack, Label currentSongLabel, Button favoriteButton) {
        currentSongLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        currentSongLabel.setWrapText(true);
        currentSongLabel.setAlignment(Pos.CENTER);

        VBox titleFavoriteBox = new VBox(5);
        titleFavoriteBox.setAlignment(Pos.CENTER);
        titleFavoriteBox.getChildren().addAll(currentSongLabel, favoriteButton);

        BorderPane mediaContainer = new BorderPane();
        mediaContainer.setPadding(new Insets(10));
        mediaContainer.setCenter(mediaDisplayStack);
        mediaContainer.setBottom(titleFavoriteBox);

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