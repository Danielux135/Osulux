package com.osuplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = new File("config.properties").getAbsolutePath();

    private Properties properties = new Properties();

    public void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                properties.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveConfig(double volume) {
        properties.setProperty("volume", String.valueOf(volume));
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "Configuración del reproductor OSU!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getVolume() {
        return Double.parseDouble(properties.getProperty("volume", "0.5"));
    }

    public void setLastFolder(String folderPath) {
        properties.setProperty("lastFolder", folderPath);
    }

    public String getLastFolderPath() {
        return properties.getProperty("lastFolder");
    }

    // ---- NUEVOS MÉTODOS PARA GUARDAR CANCIÓN Y POSICIÓN ----
    public void saveCurrentSong(String songName, double position) {
        properties.setProperty("currentSong", songName);
        properties.setProperty("currentPosition", String.valueOf(position));
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "Configuración del reproductor OSU!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCurrentSong() {
        return properties.getProperty("currentSong", "");
    }

    public double getCurrentPosition() {
        return Double.parseDouble(properties.getProperty("currentPosition", "0.0"));
    }
}
