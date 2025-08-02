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

    /**
     * Carga todas las canciones leyendo archivos .osu recursivamente
     * desde la carpeta indicada. Para cada canción, guarda
     * "Título — Artista" como clave y la ruta del archivo de audio (mp3/ogg) como valor.
     */
    public Map<String, String> loadSongsFromFolder(File folder) {
        songPathMap.clear();
        songsAdded.clear();
        if (folder != null && folder.exists() && folder.isDirectory()) {
            findMusicFiles(folder);
            lastFolderPath = folder.getAbsolutePath();
        }
        return songPathMap;
    }

    /**
     * Busca recursivamente archivos .osu para extraer título y artista,
     * luego asocia con el archivo de audio mp3/ogg en la misma carpeta.
     */
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

                    // Leer archivo .osu con UTF-8
                    try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("Title:")) title = line.substring(6).trim();
                            else if (line.startsWith("Artist:")) artist = line.substring(7).trim();
                            if (title != null && artist != null) break;
                        }
                    } catch (MalformedInputException e) {
                        // Si falla UTF-8, intentar con ISO-8859-1
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
                            File[] audioFiles = parent.listFiles((dir, name) ->
                                name.toLowerCase().endsWith(".mp3") ||
                                name.toLowerCase().endsWith(".ogg")
                            );
                            if (audioFiles != null && audioFiles.length > 0) {
                                songPathMap.put(displayName, audioFiles[0].getAbsolutePath());
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

    /**
     * Devuelve la ruta absoluta del archivo de audio de la canción.
     */
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
