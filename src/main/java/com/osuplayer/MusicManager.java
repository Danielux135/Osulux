package com.osuplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MusicManager {

    private Map<String, String> songPathMap = new HashMap<>();
    private Set<String> songsAdded = new HashSet<>();
    private String lastFolderPath;

    public Map<String, String> loadSongsFromFolder(File folder) {
        songPathMap.clear();
        songsAdded.clear();
        findMusicFiles(folder);
        return songPathMap;
    }

    private void findMusicFiles(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findMusicFiles(file);
            } else if (file.getName().toLowerCase().endsWith(".osu")) {
                try {
                    String title = null;
                    String artist = null;

                    try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("Title:")) title = line.substring(6).trim();
                            else if (line.startsWith("Artist:")) artist = line.substring(7).trim();
                            if (title != null && artist != null) break;
                        }
                    } catch (MalformedInputException e) {
                        title = null;
                        artist = null;
                        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.ISO_8859_1)) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("Title:")) title = line.substring(6).trim();
                                else if (line.startsWith("Artist:")) artist = line.substring(7).trim();
                                if (title != null && artist != null) break;
                            }
                        }
                    }

                    if (title != null && artist != null) {
                        String displayName = title + " — " + artist;
                        if (!songsAdded.contains(displayName)) {
                            File parent = file.getParentFile();
                            File[] mp3s = parent.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
                            if (mp3s != null && mp3s.length > 0) {
                                songPathMap.put(displayName, mp3s[0].getAbsolutePath());
                                songsAdded.add(displayName);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getSongPath(String songName) {
        return songPathMap.get(songName);
    }

    public void setLastFolderPath(String path) {
        this.lastFolderPath = path;
    }

    public String getLastFolderPath() {
        return lastFolderPath;
    }
}
