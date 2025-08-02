package com.osuplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfigManager {

    private final Properties properties = new Properties();
    private final File configFile;

    public ConfigManager() {
        // Archivo config.properties en el directorio actual de ejecución
        configFile = Paths.get(System.getProperty("user.dir"), "config.properties").toFile();
        loadConfig();
    }

    /** ==================== CARGA & GUARDADO ==================== **/
    public void loadConfig() {
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Error cargando config.properties:");
                e.printStackTrace();
            }
        }
    }

    private void saveProperties() {
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "OSU! Music Player Config");
        } catch (IOException e) {
            System.err.println("Error guardando config.properties:");
            e.printStackTrace();
        }
    }

    /** ==================== AJUSTES DE VOLUMEN ==================== **/
    public void saveConfig(double volume) {
        if (volume < 0) volume = 0;
        if (volume > 1) volume = 1;
        properties.setProperty("volume", Double.toString(volume));
        saveProperties();
    }

    public double getVolume() {
        try {
            double v = Double.parseDouble(properties.getProperty("volume", "0.5"));
            if (v < 0 || v > 1) return 0.5;
            return v;
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }

    /** ==================== CANCIÓN ACTUAL ==================== **/
    public void saveCurrentSong(String songName, double positionSeconds) {
        if (songName != null && !songName.trim().isEmpty()) {
            properties.setProperty("currentSong", songName.trim());
            properties.setProperty("currentPosition", Double.toString(Math.max(0, positionSeconds)));
            saveProperties();
        }
    }

    public String getCurrentSong() {
        return properties.getProperty("currentSong", "");
    }

    public double getCurrentPosition() {
        try {
            double pos = Double.parseDouble(properties.getProperty("currentPosition", "0"));
            return pos < 0 ? 0 : pos;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** ==================== ÚLTIMA CARPETA ==================== **/
    public void setLastFolder(String path) {
        if (path != null && !path.trim().isEmpty()) {
            properties.setProperty("lastFolder", path.trim());
            saveProperties();
        }
    }

    public String getLastFolder() {
        return properties.getProperty("lastFolder", "");
    }

    /** ==================== FAVORITOS ==================== **/
    public List<String> getFavorites() {
        String favString = properties.getProperty("favorites", "").trim();
        if (favString.isEmpty()) return new ArrayList<>();
        return Arrays.stream(favString.split(";"))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toList());
    }

    public void saveFavorites(List<String> favorites) {
        if (favorites == null) return;
        String favString = favorites.stream()
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .collect(Collectors.joining(";"));
        properties.setProperty("favorites", favString);
        saveProperties();
    }

    /** ==================== RESET CONFIGURACIÓN ==================== **/
    public void resetConfig() {
        properties.clear();
        saveProperties();
    }
}
