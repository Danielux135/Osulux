package com.osuplayer;

import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class DialogHelper {

    // Diálogo para pedir texto al usuario
    public static String showTextInputDialog(String title, String content) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(content);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    // Diálogo para mostrar alerta con botón OK
    public static void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Diálogo para mostrar información (OK)
    public static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Diálogo para selección de opción (de lista)
    public static String showChoiceDialog(String title, String content, List<String> options) {
        if (options == null || options.isEmpty()) return null;

        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(content);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    // Diálogo para elegir carpeta
    public static File showDirectoryChooser(String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        // El diálogo necesita un Stage, por eso se pasa null, que usa la ventana principal por defecto
        return directoryChooser.showDialog(null);
    }
}
