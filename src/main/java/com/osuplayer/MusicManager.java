package com.osuplayer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicManager {

    private final Map<String, String> songs = new LinkedHashMap<>();
    private String lastFolderPath;

    // Nueva estructura para mapear el nombre de la canción a la carpeta beatmap
    private final Map<String, String> songBaseFolders = new HashMap<>();

    public Map<String, String> loadSongsFromFolder(File folder) {
        songs.clear();
        songBaseFolders.clear();
        if (folder == null || !folder.exists() || !folder.isDirectory()) return songs;

        File[] beatmapFolders = folder.listFiles(File::isDirectory);
        if (beatmapFolders == null) return songs;

        for (File beatmap : beatmapFolders) {
            File[] osuFiles = beatmap.listFiles((dir, name) -> name.toLowerCase().endsWith(".osu"));
            if (osuFiles == null || osuFiles.length == 0) continue;

            // Intentar extraer datos del primer archivo .osu válido
            for (File osuFile : osuFiles) {
                SongMetadata meta = parseOsuFile(osuFile);
                if (meta != null && meta.audioFilename != null) {
                    File audioFile = new File(beatmap, meta.audioFilename);
                    if (audioFile.exists()) {
                        String displayName = meta.artist + " - " + meta.title;
                        if (!songs.containsKey(displayName)) { // Evitar duplicados
                            songs.put(displayName, audioFile.getAbsolutePath());
                            // Guardar carpeta base para la canción
                            songBaseFolders.put(displayName, beatmap.getAbsolutePath());
                        }
                        break; // tomar solo la primera canción válida
                    }
                }
            }
        }
        return songs;
    }

    public String getSongPath(String songName) {
        return songs.get(songName);
    }

    public void exportSongPath(String songName) {
        String path = getSongPath(songName);
        if (path != null) {
            try (FileWriter writer = new FileWriter("exported_song_path.txt", false)) {
                writer.write(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setLastFolderPath(String path) {
        this.lastFolderPath = path;
    }

    public String getLastFolderPath() {
        return lastFolderPath;
    }

    /**
     * Devuelve la carpeta base (beatmap folder) donde está la canción.
     * Retorna null si no se encuentra.
     */
    public String getSongBaseFolder(String songName) {
        return songBaseFolders.get(songName);
    }

    /**
     * NUEVO MÉTODO:
     * Devuelve la ruta completa de la imagen de portada (background) del beatmap de la canción.
     * Retorna null si no se encuentra.
     */
    public String getCoverImagePath(String songName) {
        String baseFolder = getSongBaseFolder(songName);
        if (baseFolder == null) return null;

        File beatmapFolder = new File(baseFolder);
        File[] osuFiles = beatmapFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".osu"));
        if (osuFiles == null || osuFiles.length == 0) return null;

        String backgroundImageFileName = null;

        for (File osuFile : osuFiles) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(osuFile), StandardCharsets.UTF_8))) {
                String line;
                boolean inEventsSection = false;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.equals("[Events]")) {
                        inEventsSection = true;
                    } else if (inEventsSection && line.startsWith("0,")) {
                        String[] parts = line.split(",");
                        if (parts.length >= 3) {
                            backgroundImageFileName = parts[2].replaceAll("\"", "");
                            break;
                        }
                    }
                }
                if (backgroundImageFileName != null) break;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        if (backgroundImageFileName != null) {
            File bgFile = new File(beatmapFolder, backgroundImageFileName);
            if (bgFile.exists()) {
                return bgFile.getAbsolutePath();
            }
        }
        return null;
    }

    // ---------- MÉTODO PRIVADO PARA PARSEAR ARCHIVO OSU ----------
    private SongMetadata parseOsuFile(File osuFile) {
        String title = null;
        String artist = null;
        String audioFilename = null;

        Pattern titlePattern = Pattern.compile("^Title:(.*)$", Pattern.CASE_INSENSITIVE);
        Pattern artistPattern = Pattern.compile("^Artist:(.*)$", Pattern.CASE_INSENSITIVE);
        Pattern audioPattern = Pattern.compile("^AudioFilename:(.*)$", Pattern.CASE_INSENSITIVE);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(osuFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m1 = titlePattern.matcher(line);
                Matcher m2 = artistPattern.matcher(line);
                Matcher m3 = audioPattern.matcher(line);

                if (m1.find()) title = m1.group(1).trim();
                if (m2.find()) artist = m2.group(1).trim();
                if (m3.find()) audioFilename = m3.group(1).trim();

                if (title != null && artist != null && audioFilename != null) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (title != null && artist != null && audioFilename != null) {
            return new SongMetadata(title, artist, audioFilename);
        }
        return null;
    }

    // Clase interna para guardar la metadata
    private static class SongMetadata {
        String title;
        String artist;
        String audioFilename;

        SongMetadata(String title, String artist, String audioFilename) {
            this.title = title;
            this.artist = artist;
            this.audioFilename = audioFilename;
        }
    }
}
