package com.osuplayer.update;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.osuplayer.common.ApplicationMetadata;
import com.osuplayer.lang.I18n;
import com.osuplayer.ui.ProgressDialog;
import com.osuplayer.ui.ThemeHelper;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.Window;

public class UpdateService {

    private static final String RELEASES_URL = "https://api.github.com/repos/%s/%s/releases/latest";

    public void checkForUpdates(Window owner, String themeId) {
        ProgressDialog dialog = new ProgressDialog(owner,
            I18n.tr("Buscar actualizaciones"),
            I18n.tr("Buscando actualizaciones"),
            themeId);
        Task<UpdateResult> task = new Task<>() {
            @Override
            protected UpdateResult call() throws Exception {
                dialog.updateProgress(0.05, I18n.tr("Consultando la última versión..."));
                UpdateInfo info = fetchLatestRelease();
                if (info == null) {
                    return UpdateResult.noRelease();
                }
                if (!isNewerVersion(info.version())) {
                    return UpdateResult.upToDate(info.version());
                }
                if (info.downloadUrl() == null || info.downloadUrl().isBlank()) {
                    return UpdateResult.noAsset(info.version(), info.pageUrl());
                }
                dialog.updateProgress(0.15, I18n.trf("Descargando %s", info.assetName()));
                Path downloaded = downloadAsset(info, dialog);
                installDownloadedFile(downloaded);
                return UpdateResult.updated(info.version(), downloaded);
            }
        };

        task.setOnRunning(evt -> dialog.show());
        task.setOnSucceeded(evt -> {
            dialog.close();
            UpdateResult result = task.getValue();
            switch (result.status()) {
                case UP_TO_DATE -> showAlert(owner, Alert.AlertType.INFORMATION,
                        I18n.tr("Actualizaciones"),
                        I18n.trf("Ya tienes la última versión (%s).", ApplicationMetadata.VERSION),
                        themeId);
                case UPDATED -> showAlert(owner, Alert.AlertType.INFORMATION,
                        I18n.tr("Actualizaciones"),
                        I18n.trf("Se descargó la versión %s en:%n%s", result.latestVersion(), result.downloadedFile()),
                        themeId);
                case NO_RELEASE -> showAlert(owner, Alert.AlertType.WARNING,
                        I18n.tr("Actualizaciones"),
                        I18n.tr("No se pudo obtener la última versión. Intenta de nuevo más tarde."),
                        themeId);
                case NO_ASSET -> {
                    String message = I18n.trf("La versión %s no tiene un instalador disponible.", result.latestVersion());
                    if (result.pageUrl() != null && !result.pageUrl().isBlank()) {
                        message += " " + I18n.trf("Puedes descargarla manualmente desde: %s", result.pageUrl());
                    }
                    showAlert(owner, Alert.AlertType.WARNING, I18n.tr("Actualizaciones"), message, themeId);
                }
            }
        });
        task.setOnFailed(evt -> {
            dialog.close();
            Throwable error = task.getException();
            String message = error == null ? I18n.tr("Error desconocido") : error.getMessage();
            showAlert(owner, Alert.AlertType.ERROR, I18n.tr("Actualizaciones"), I18n.trf("No se pudo completar la actualización: %s", message), themeId);
        });

        Thread worker = new Thread(task, "osulux-update-check");
        worker.setDaemon(true);
        worker.start();
    }

    private UpdateInfo fetchLatestRelease() throws IOException {
        String url = String.format(Locale.ROOT, RELEASES_URL, ApplicationMetadata.REPOSITORY_OWNER, ApplicationMetadata.REPOSITORY_NAME);
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "Osulux-" + ApplicationMetadata.VERSION);
        int status = connection.getResponseCode();
        if (status != 200) {
            throw new IOException(I18n.trf("GitHub respondió con código %s", status));
        }
        try (InputStream input = connection.getInputStream();
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String tag = root.has("tag_name") ? root.get("tag_name").getAsString() : null;
            String pageUrl = root.has("html_url") ? root.get("html_url").getAsString() : null;
            JsonArray assets = root.has("assets") && root.get("assets").isJsonArray()
                    ? root.getAsJsonArray("assets")
                    : new JsonArray();
            if (tag == null) {
                return null;
            }
            if (assets.isEmpty()) {
                return new UpdateInfo(cleanVersion(tag), null, null, pageUrl);
            }
            JsonObject firstAsset = assets.get(0).getAsJsonObject();
            String assetName = firstAsset.has("name") ? firstAsset.get("name").getAsString() : "osulux-update";
            String downloadUrl = firstAsset.has("browser_download_url")
                    ? firstAsset.get("browser_download_url").getAsString()
                    : null;
            return new UpdateInfo(cleanVersion(tag), assetName, downloadUrl, pageUrl);
        }
    }

    private Path downloadAsset(UpdateInfo info, ProgressDialog dialog) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(info.downloadUrl()).toURL().openConnection();
        connection.setRequestProperty("User-Agent", "Osulux-" + ApplicationMetadata.VERSION);
        long contentLength = connection.getContentLengthLong();
        Path tempDir = Files.createTempDirectory("osulux-update");
        Path targetFile = tempDir.resolve(info.assetName());
        try (InputStream input = connection.getInputStream()) {
            byte[] buffer = new byte[8192];
            long downloaded = 0;
            try (var output = Files.newOutputStream(targetFile)) {
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    downloaded += read;
                    if (contentLength > 0) {
                        double progress = 0.15 + (downloaded / (double) contentLength) * 0.8;
                        dialog.updateProgress(Math.min(progress, 0.95), I18n.trf("Descargando %s", info.assetName()));
                    }
                }
            }
        }
        dialog.updateProgress(0.99, I18n.tr("Preparando instalación..."));
        return targetFile;
    }

    private void installDownloadedFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if ((name.endsWith(".exe") || name.endsWith(".msi")) && isWindows()) {
                new ProcessBuilder(file.toString()).start();
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file.toFile());
            }
        } catch (IOException ignored) {
        }
    }

    private void showAlert(Window owner, Alert.AlertType type, String title, String message, String themeId) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (owner != null) {
            alert.initOwner(owner);
        }
        ThemeHelper.applyTheme(alert.getDialogPane().getStylesheets(), themeId);
        alert.showAndWait();
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    private boolean isNewerVersion(String latestVersion) {
        String[] latestParts = cleanVersion(latestVersion).split("\\.");
        String[] currentParts = cleanVersion(ApplicationMetadata.VERSION).split("\\.");
        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latest = i < latestParts.length ? parseInt(latestParts[i]) : 0;
            int current = i < currentParts.length ? parseInt(currentParts[i]) : 0;
            if (latest > current) {
                return true;
            }
            if (latest < current) {
                return false;
            }
        }
        return false;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String cleanVersion(String version) {
        if (version == null) {
            return "0.0.0";
        }
        return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
    }

    private record UpdateInfo(String version, String assetName, String downloadUrl, String pageUrl) {}

    private enum UpdateStatus { UP_TO_DATE, UPDATED, NO_RELEASE, NO_ASSET }

    private record UpdateResult(UpdateStatus status, String latestVersion, Path downloadedFile, String pageUrl) {
        static UpdateResult upToDate(String version) {
            return new UpdateResult(UpdateStatus.UP_TO_DATE, version, null, null);
        }

        static UpdateResult updated(String version, Path file) {
            return new UpdateResult(UpdateStatus.UPDATED, version, file, null);
        }

        static UpdateResult noRelease() {
            return new UpdateResult(UpdateStatus.NO_RELEASE, null, null, null);
        }

        static UpdateResult noAsset(String version, String pageUrl) {
            return new UpdateResult(UpdateStatus.NO_ASSET, version, null, pageUrl);
        }
    }
}
