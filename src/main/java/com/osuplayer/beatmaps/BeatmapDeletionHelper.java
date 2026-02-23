package com.osuplayer.beatmaps;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.stream.Stream;

import com.osuplayer.config.ConfigManager;
import com.osuplayer.lang.I18n;
import com.osuplayer.ui.ThemeHelper;
import com.osuplayer.dependencies.IconDependencyProvider;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

public class BeatmapDeletionHelper {

    private final ConfigManager configManager;

    public BeatmapDeletionHelper(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean confirmDeletionIfNeeded(Window owner) {
        if (!configManager.isBeatmapDeleteConfirmationEnabled()) {
            return true;
        }
        CheckBox dontShowAgain = new CheckBox(I18n.tr("No volver a mostrar"));
        Label message = new Label(I18n.tr("¿Seguro que quieres eliminarlo? Esto es permanente."));
        message.setWrapText(true);
        VBox content = new VBox(10, message, dontShowAgain);
        content.setPadding(new Insets(5, 0, 0, 0));
        content.setPrefWidth(360);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.tr("Eliminar beatmap"));
        alert.setHeaderText(null);
        alert.setContentText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        ButtonType yes = new ButtonType(I18n.tr("Sí"), ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType(I18n.tr("No"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yes, no);
        alert.getDialogPane().setContent(content);
        ThemeHelper.applyTheme(alert.getDialogPane().getStylesheets(), configManager.getTheme());
        applyAlertIcons(alert, owner);
        Optional<ButtonType> result = alert.showAndWait();
        if (dontShowAgain.isSelected()) {
            configManager.setBeatmapDeleteConfirmationEnabled(false);
        }
        return result.isPresent() && result.get() == yes;
    }

    public Path resolveSongsDirectory() throws IOException {
        String configuredPath = configManager.getSongsDirectory();
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IOException("Configura la carpeta de canciones de osu! antes de eliminar beatmaps.");
        }
        final Path songsDirectory;
        try {
            songsDirectory = Paths.get(configuredPath);
        } catch (InvalidPathException ex) {
            throw new IOException("La ruta configurada es inválida: " + ex.getMessage(), ex);
        }
        if (!Files.isDirectory(songsDirectory)) {
            throw new IOException("La carpeta configurada ya no existe o no es válida.");
        }
        return songsDirectory;
    }

    public Path validateExistingFolder(String folderPath) throws IOException {
        if (folderPath == null || folderPath.isBlank()) {
            throw new IOException("No se pudo localizar la carpeta del beatmap seleccionado.");
        }
        final Path folder;
        try {
            folder = Paths.get(folderPath);
        } catch (InvalidPathException ex) {
            throw new IOException("La ruta del beatmap es inválida: " + ex.getMessage(), ex);
        }
        if (!Files.isDirectory(folder)) {
            throw new IOException("La carpeta del beatmap ya no existe en disco.");
        }
        return folder;
    }

    public Path findBeatmapFolderById(Path songsDirectory, long beatmapsetId) throws IOException {
        String marker = "[" + beatmapsetId + "]";
        try (Stream<Path> stream = Files.list(songsDirectory)) {
            return stream
                .filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().contains(marker))
                .findFirst()
                .orElse(null);
        }
    }

    public void deleteFolder(Path folder) throws IOException {
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static Image fallbackIcon;

    private void applyAlertIcons(Alert alert, Window owner) {
        if (alert == null) {
            return;
        }
        Window alertWindow = alert.getDialogPane().getScene().getWindow();
        if (!(alertWindow instanceof Stage alertStage)) {
            return;
        }
        if (owner instanceof Stage ownerStage && !ownerStage.getIcons().isEmpty()) {
            alertStage.getIcons().setAll(ownerStage.getIcons());
            return;
        }
        if (fallbackIcon == null) {
            fallbackIcon = IconDependencyProvider.getOrNull();
        }
        if (fallbackIcon != null) {
            alertStage.getIcons().setAll(fallbackIcon);
        }
    }
}
