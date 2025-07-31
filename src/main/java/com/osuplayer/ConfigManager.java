package com.osuplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_FILE = System.getenv("APPDATA") + File.separator + "Osulux" + File.separator + "config.properties";

    private String lastFolder;
    private double volume = 0.5;

    public void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) return;

        try (InputStream input = new FileInputStream(configFile)) {
            Properties props = new Properties();
            props.load(input);
            String volumeStr = props.getProperty("volume");
            String folderStr = props.getProperty("lastFolder");
            if (volumeStr != null) volume = Double.parseDouble(volumeStr);
            if (folderStr != null && !folderStr.isBlank()) lastFolder = folderStr;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig(double volume) {
        saveConfig(lastFolder, volume);
    }

    public void saveConfig(String folder, double volume) {
        try {
            File configFile = new File(CONFIG_FILE);
            File parentDir = configFile.getParentFile();
            if (!parentDir.exists()) parentDir.mkdirs();

            Properties props = new Properties();
            props.setProperty("volume", String.valueOf(volume));
            if (folder != null) props.setProperty("lastFolder", folder);

            try (OutputStream output = new FileOutputStream(configFile)) {
                props.store(output, "OSU Music Player Config");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLastFolder() {
        return lastFolder;
    }

    public void setLastFolder(String folder) {
        this.lastFolder = folder;
    }

    public double getVolume() {
        return volume;
    }
}
