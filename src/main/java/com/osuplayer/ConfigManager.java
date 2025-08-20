package com.osuplayer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private final Properties props = new Properties();

    public ConfigManager() {
        loadProperties();
    }

    private void loadProperties() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            props.load(input);
        } catch (IOException e) {
            System.out.println("No se pudo cargar config.properties, se usarán valores por defecto.");
        }
    }

    private void saveProperties() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            props.store(output, "Configuración de OSU! Music Player");
        } catch (IOException e) {
            System.out.println("No se pudo guardar config.properties.");
        }
    }

    public double getVolume() {
        return parseDouble(props.getProperty("volume", "0.5"), 0.5);
    }

    public void setVolume(double volume) {
        props.setProperty("volume", Double.toString(volume));
        saveProperties();
    }

    public String getLastFolder() {
        return props.getProperty("lastFolder", "");
    }

    public void setLastFolder(String folderPath) {
        props.setProperty("lastFolder", folderPath);
        saveProperties();
    }

    public List<String> getFavorites() {
        return parseList(props.getProperty("favorites", ""), ",");
    }

    public void setFavorites(List<String> favorites) {
        props.setProperty("favorites", String.join(",", favorites));
        saveProperties();
    }

    public Map<String, List<String>> getPlaylists() {
        Map<String, List<String>> playlists = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("playlist.")) {
                String name = key.substring("playlist.".length());
                playlists.put(name, parseList(props.getProperty(key, ""), ","));
            }
        }
        return playlists;
    }

    public void setPlaylists(Map<String, List<String>> playlists) {
        props.keySet().removeIf(k -> k.toString().startsWith("playlist."));
        for (Map.Entry<String, List<String>> entry : playlists.entrySet()) {
            props.setProperty("playlist." + entry.getKey(), String.join(",", entry.getValue()));
        }
        saveProperties();
    }

    public String getCurrentSong() {
        return props.getProperty("currentSong", "");
    }

    public double getCurrentSongPosition() {
        return parseDouble(props.getProperty("currentSongPosition", "0"), 0);
    }

    public void setCurrentSong(String song, double position) {
        props.setProperty("currentSong", song);
        props.setProperty("currentSongPosition", Double.toString(position));
        saveProperties();
    }

    public String getLastSong() {
        return props.getProperty("lastSong", "");
    }

    public void setLastSong(String songName) {
        props.setProperty("lastSong", songName);
        saveProperties();
    }

    public void setPlayHistory(List<String> history) {
        props.setProperty("playHistory", String.join(";", history));
        saveProperties();
    }

    public List<String> getPlayHistory() {
        return parseList(props.getProperty("playHistory", ""), ";");
    }

    public void setHistoryIndex(int index) {
        props.setProperty("historyIndex", String.valueOf(index));
        saveProperties();
    }

    public int getHistoryIndex() {
        try {
            return Integer.parseInt(props.getProperty("historyIndex", "-1"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getSongsDirectory() {
        return props.getProperty("songsDirectory", "");
    }

    public double getWindowWidth() {
        return parseDouble(props.getProperty("window.width", "1200"), 1200);
    }

    public void setWindowWidth(double width) {
        props.setProperty("window.width", Double.toString(width));
        saveProperties();
    }

    public double getWindowHeight() {
        return parseDouble(props.getProperty("window.height", "720"), 720);
    }

    public void setWindowHeight(double height) {
        props.setProperty("window.height", Double.toString(height));
        saveProperties();
    }

    private double parseDouble(String val, double def) {
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private List<String> parseList(String val, String sep) {
        if (val == null || val.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(val.split(sep)));
    }
}
