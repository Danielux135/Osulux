package com.osuplayer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";

    private Properties properties = new Properties();

    public void loadConfig() {
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            properties.load(in);
        } catch (IOException e) {
            // archivo no existe o no se pudo leer, usar valores por defecto
        }
    }

    public void saveConfig(double volume) {
        properties.setProperty("volume", Double.toString(volume));
        savePropertiesToFile();
    }

    public double getVolume() {
        String vol = properties.getProperty("volume");
        if (vol != null) {
            try {
                return Double.parseDouble(vol);
            } catch (NumberFormatException e) {
                return 0.5;
            }
        }
        return 0.5;
    }

    public void setLastFolder(String path) {
        properties.setProperty("lastFolder", path);
        savePropertiesToFile();
    }

    public String getLastFolder() {
        return properties.getProperty("lastFolder");
    }

    // Métodos para canción actual y posición
    public void saveCurrentSong(String songName, double position) {
        properties.setProperty("currentSong", songName);
        properties.setProperty("currentPosition", Double.toString(position));
        savePropertiesToFile();
    }

    public String getCurrentSong() {
        return properties.getProperty("currentSong");
    }

    public double getCurrentPosition() {
        String pos = properties.getProperty("currentPosition");
        if (pos != null) {
            try {
                return Double.parseDouble(pos);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private void savePropertiesToFile() {
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "OSU! Music Player config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
