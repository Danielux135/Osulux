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
        }
    }

    private void saveProperties() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            props.store(output, "Configuración de OSU! Music Player");
        } catch (IOException e) {
        }
    }

    public double getVolume() {
        String v = props.getProperty("volume", "0.5");
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 0.5;
        }
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
        String favs = props.getProperty("favorites", "");
        if (favs.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(favs.split(",")));
    }

    public void setFavorites(List<String> favorites) {
        String joined = String.join(",", favorites);
        props.setProperty("favorites", joined);
        saveProperties();
    }

    public Map<String, List<String>> getPlaylists() {
        Map<String, List<String>> playlists = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("playlist.")) {
                String name = key.substring("playlist.".length());
                String value = props.getProperty(key);
                List<String> songs = new ArrayList<>();
                if (value != null && !value.isEmpty()) {
                    songs = new ArrayList<>(Arrays.asList(value.split(",")));
                }
                playlists.put(name, songs);
            }
        }
        return playlists;
    }

    public void setPlaylists(Map<String, List<String>> playlists) {
        List<String> keysToRemove = new ArrayList<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("playlist.")) {
                keysToRemove.add(key);
            }
        }
        for (String key : keysToRemove) {
            props.remove(key);
        }
        for (Map.Entry<String, List<String>> entry : playlists.entrySet()) {
            String key = "playlist." + entry.getKey();
            String value = String.join(",", entry.getValue());
            props.setProperty(key, value);
        }
        saveProperties();
    }

    public String getCurrentSong() {
        return props.getProperty("currentSong", "");
    }

    public double getCurrentSongPosition() {
        String posStr = props.getProperty("currentSongPosition", "0");
        try {
            return Double.parseDouble(posStr);
        } catch (NumberFormatException e) {
            return 0;
        }
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
        String joined = String.join(";", history);
        props.setProperty("playHistory", joined);
        saveProperties();
    }

    public List<String> getPlayHistory() {
        String joined = props.getProperty("playHistory", "");
        if (joined.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(joined.split(";")));
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
        String w = props.getProperty("window.width", "1200");
        try {
            return Double.parseDouble(w);
        } catch (NumberFormatException e) {
            return 1200;
        }
    }

    public void setWindowWidth(double width) {
        props.setProperty("window.width", Double.toString(width));
        saveProperties();
    }

    public double getWindowHeight() {
        String h = props.getProperty("window.height", "720");
        try {
            return Double.parseDouble(h);
        } catch (NumberFormatException e) {
            return 720;
        }
    }

    public void setWindowHeight(double height) {
        props.setProperty("window.height", Double.toString(height));
        saveProperties();
    }
}
