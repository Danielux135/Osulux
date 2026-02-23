package com.osuplayer.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.osuplayer.config.ConfigManager;
import com.osuplayer.lang.I18n;
import com.osuplayer.lang.LanguageManager;
import com.osuplayer.playback.MusicManager;
import com.osuplayer.common.ApplicationMetadata;
import com.osuplayer.dependencies.IconDependencyProvider;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

public final class AboutDialog {

    private static final int ICON_SIZE = 28;
    private static final double EMAIL_DIALOG_MIN_WIDTH = 380d;
    private static final String CONTACT_EMAIL = "danielux135@gmail.com";
    private static final String PAYPAL_URL = "https://www.paypal.com/donate?business=" + CONTACT_EMAIL;
    private static final String TWITCH_URL = "https://www.twitch.tv/" + ApplicationMetadata.REPOSITORY_OWNER.toLowerCase(Locale.ROOT);
    private static final String YOUTUBE_URL = "https://www.youtube.com/@" + ApplicationMetadata.REPOSITORY_OWNER;
    private static final String DISCORD_URL = "https://discord.com/users/289066436301946880";
    private static Image fallbackAppIcon;

    private AboutDialog() { }

    public static void show(Window owner,
                            MusicManager musicManager,
                            ConfigManager configManager) {
        Stage stage = new Stage(StageStyle.UTILITY);
        stage.initModality(Modality.NONE); 
        stage.setAlwaysOnTop(true);
        applyStageIcons(stage, owner);
        stage.setTitle(I18n.tr("Acerca de Osulux"));
        String themeId = configManager.getTheme();

        Label versionLabel = new Label(I18n.trf("Versión: %s", ApplicationMetadata.VERSION));
        versionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        versionLabel.setMaxWidth(Double.MAX_VALUE);
        versionLabel.setAlignment(Pos.CENTER);
        versionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Label authorLabel = new Label(I18n.trf("Autor: %s", "Danielux"));
        authorLabel.setMaxWidth(Double.MAX_VALUE);
        authorLabel.setAlignment(Pos.CENTER);
        authorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox socialBox = new VBox(6);
        socialBox.setAlignment(Pos.CENTER);
        Label socialHeading = new Label(I18n.tr("Sígueme o contáctame en:"));
        socialHeading.setMaxWidth(Double.MAX_VALUE);
        socialHeading.setAlignment(Pos.CENTER);
        socialHeading.setStyle("-fx-font-weight: bold;");

        HBox socialRow = buildSocialRow(stage, themeId);

        Label donateHeading = new Label(I18n.tr("Considera donar"));
        donateHeading.setMaxWidth(Double.MAX_VALUE);
        donateHeading.setAlignment(Pos.CENTER);
        donateHeading.setStyle("-fx-font-weight: bold;");

        Hyperlink donateLink = createIconLink("paypal.png", I18n.tr("Donar con PayPal"),
            () -> openUri(PAYPAL_URL, stage));
        HBox donateLinkRow = new HBox(donateLink);
        donateLinkRow.setAlignment(Pos.CENTER);

        socialBox.getChildren().addAll(socialHeading, socialRow, donateHeading, donateLinkRow);

        Label songsLabel = new Label(I18n.trf("Canciones cargadas: %s", musicManager.getLoadedSongCount()));
        Label diffLabel = new Label(I18n.trf("Dificultades detectadas: %s", musicManager.getLoadedDifficultyCount()));
        String folderPath = musicManager.getLastFolderPath();
        Region folderValueNode;
        if (folderPath == null || folderPath.isBlank()) {
            Label noFolder = new Label(I18n.tr("No seleccionada"));
            noFolder.setWrapText(true);
            folderValueNode = noFolder;
        } else {
            Hyperlink folderLink = new Hyperlink(folderPath);
            folderLink.setWrapText(true);
            folderLink.setOnAction(e -> openSongsFolder(folderPath, stage));
            folderValueNode = folderLink;
        }
        folderValueNode.setMaxWidth(Double.MAX_VALUE);
        HBox folderRow = new HBox(6, new Label(I18n.tr("Carpeta Songs:")), folderValueNode);
        folderRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(folderValueNode, Priority.ALWAYS);

        Label folderSizeLabel = new Label(folderPath == null || folderPath.isBlank()
            ? I18n.tr("Tamaño carpeta Songs: No disponible")
            : I18n.tr("Tamaño carpeta Songs: Calculando..."));

        VBox content = new VBox(8,
            versionLabel,
            authorLabel,
            socialBox,
            new Separator(),
            songsLabel,
            diffLabel,
            folderRow,
            folderSizeLabel
        );
        content.setPadding(new Insets(15));

        Button closeButton = new Button(I18n.tr("Cerrar"));
        closeButton.setDefaultButton(true);
        closeButton.setOnAction(e -> stage.close());
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setPadding(new Insets(0, 15, 15, 15));
        buttonBox.setSpacing(10);
        buttonBox.setStyle("-fx-alignment: center-right;");
        HBox.setHgrow(closeButton, Priority.NEVER);

        VBox root = new VBox(content, buttonBox);
        Scene scene = new Scene(root);
        ThemeHelper.applyTheme(scene, themeId);
        stage.setScene(scene);
        stage.setResizable(false); 
        stage.show();

        if (folderPath != null && !folderPath.isBlank()) {
            Task<Long> sizeTask = new Task<>() {
                @Override
                protected Long call() throws Exception {
                    return computeFolderSize(Path.of(folderPath));
                }
            };
            sizeTask.setOnSucceeded(e -> folderSizeLabel.setText(I18n.trf("Tamaño carpeta Songs: %s", formatBytes(sizeTask.getValue()))));
            sizeTask.setOnFailed(e -> folderSizeLabel.setText(I18n.tr("Tamaño carpeta Songs: No disponible")));
            Thread worker = new Thread(sizeTask, "songs-folder-size");
            worker.setDaemon(true);
            worker.start();
        }
    }

    private static HBox buildSocialRow(Window owner, String themeId) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(
            createIconLink("twitch.png", I18n.tr("Abrir Twitch"), () -> openUri(TWITCH_URL, owner)),
            createIconLink("youtube.png", I18n.tr("Abrir YouTube"), () -> openUri(YOUTUBE_URL, owner)),
            createIconLink("discord.png", I18n.tr("Agregarme en Discord"), () -> openUri(DISCORD_URL, owner)),
            createIconLink("gmail.png", I18n.tr("Enviar correo"), () -> showEmailDialog(owner, themeId))
        );
        return row;
    }

    private static Hyperlink createIconLink(String iconName,
                                            String tooltipText,
                                            Runnable action) {
        Hyperlink link = new Hyperlink();
        link.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        link.setPadding(new Insets(4));
        link.setMinSize(ICON_SIZE + 12, ICON_SIZE + 12);
        link.setFocusTraversable(false);
        link.setVisited(false);

        ImageView icon = loadIcon(iconName);
        if (icon != null) {
            link.setGraphic(icon);
        } else {
            link.setText(tooltipText);
        }

        Tooltip tooltip = new Tooltip(tooltipText);
        link.setTooltip(tooltip);
        link.setOnAction(e -> {
            link.setVisited(false);
            if (action != null) {
                action.run();
            }
        });
        return link;
    }

    private static void showEmailDialog(Window owner, String themeId) {
        Stage dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        applyStageIcons(dialog, owner);
        dialog.setTitle(I18n.tr("Contacto"));

        Label instructionLabel = new Label(I18n.tr("Haz clic en el correo para copiarlo"));
        Label emailLabel = new Label(CONTACT_EMAIL);
        emailLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-accent;");
        emailLabel.setUnderline(true);

        Label feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-font-style: italic;");

        HBox emailRow = new HBox(10, emailLabel, feedbackLabel);
        emailRow.setAlignment(Pos.CENTER_LEFT);

        emailLabel.setOnMouseClicked(event -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(CONTACT_EMAIL);
            boolean success = clipboard.setContent(content);
            feedbackLabel.setText(success ? getCopyFeedbackText() : I18n.tr("Error"));
        });

        Button closeButton = new Button(I18n.tr("Cerrar"));
        closeButton.setDefaultButton(true);
        closeButton.setOnAction(e -> dialog.close());
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(10, instructionLabel, emailRow, buttonBox);
        content.setPadding(new Insets(15));
        content.setMinWidth(EMAIL_DIALOG_MIN_WIDTH);
        content.setPrefWidth(EMAIL_DIALOG_MIN_WIDTH);

        Scene scene = new Scene(content);
        ThemeHelper.applyTheme(scene, themeId);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.sizeToScene();
        dialog.show();
    }

    private static String getCopyFeedbackText() {
        String languageId = LanguageManager.getInstance().getLanguageId();
        if ("en".equalsIgnoreCase(languageId)) {
            return "Copied";
        }
        return "Copiado";
    }

    private static ImageView loadIcon(String fileName) {
        var resource = AboutDialog.class.getResource("/png/" + fileName);
        if (resource == null) {
            return null;
        }
        Image image = new Image(resource.toExternalForm(), ICON_SIZE, ICON_SIZE, true, true);
        ImageView view = new ImageView(image);
        view.setFitWidth(ICON_SIZE);
        view.setFitHeight(ICON_SIZE);
        view.setPreserveRatio(true);
        return view;
    }

    private static void openUri(String target, Window owner) {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new UnsupportedOperationException(I18n.tr("La plataforma no soporta abrir enlaces desde la aplicación."));
            }
            Desktop desktop = Desktop.getDesktop();
            URI uri = URI.create(target);
            if ("mailto".equalsIgnoreCase(uri.getScheme())) {
                if (desktop.isSupported(Desktop.Action.MAIL)) {
                    desktop.mail(uri);
                } else {
                    throw new UnsupportedOperationException(I18n.tr("La plataforma no soporta abrir enlaces desde la aplicación."));
                }
            } else {
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(uri);
                } else {
                    throw new UnsupportedOperationException(I18n.tr("La plataforma no soporta abrir enlaces desde la aplicación."));
                }
            }
        } catch (IllegalArgumentException | IOException | UnsupportedOperationException | SecurityException ex) {
            Alert alert = new Alert(AlertType.ERROR);
            if (owner != null) {
                alert.initOwner(owner);
            }
            applyAlertIcons(alert, owner);
            alert.setTitle(I18n.tr("No se pudo abrir el enlace"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.trf("Error al abrir el enlace: %s", ex.getMessage()));
            alert.showAndWait();
        }
    }

    private static void openSongsFolder(String folderPath, Window owner) {
        if (folderPath == null || folderPath.isBlank()) {
            return;
        }

        try {
            if (!Desktop.isDesktopSupported()) {
                throw new UnsupportedOperationException(I18n.tr("La plataforma no soporta abrir carpetas desde la aplicación."));
            }
            Desktop.getDesktop().open(new File(folderPath));
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            Alert alert = new Alert(AlertType.ERROR);
            if (owner != null) {
                alert.initOwner(owner);
            }
            applyAlertIcons(alert, owner);
            alert.setTitle(I18n.tr("No se pudo abrir la carpeta"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.trf("Error al abrir la carpeta de canciones: %s", ex.getMessage()));
            alert.showAndWait();
        }
    }

    private static long computeFolderSize(Path folder) throws IOException {
        try (var walk = Files.walk(folder)) {
            return walk
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException ignored) {
                        return 0L;
                    }
                })
                .sum();
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024d;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.2f %s", value, units[unitIndex]);
    }

    private static void applyStageIcons(Stage stage, Window owner) {
        if (stage == null) {
            return;
        }
        List<Image> icons = new ArrayList<>();
        if (owner instanceof Stage ownerStage && !ownerStage.getIcons().isEmpty()) {
            icons.addAll(ownerStage.getIcons());
        }
        if (icons.isEmpty()) {
            Image fallback = getFallbackIcon();
            if (fallback != null) {
                icons.add(fallback);
            }
        }
        if (!icons.isEmpty()) {
            stage.getIcons().setAll(icons);
        }
    }

    private static Image getFallbackIcon() {
        if (fallbackAppIcon == null) {
            fallbackAppIcon = IconDependencyProvider.getOrNull();
        }
        return fallbackAppIcon;
    }

    private static void applyAlertIcons(Alert alert, Window owner) {
        if (alert == null) {
            return;
        }
        Window alertWindow = alert.getDialogPane().getScene().getWindow();
        if (alertWindow instanceof Stage alertStage) {
            applyStageIcons(alertStage, owner);
        }
    }
}
