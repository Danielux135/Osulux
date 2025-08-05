package com.osuplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicManager {

    private final Map<String, String> songs = new LinkedHashMap<>();
    private String lastFolderPath;

    private final Map<String, String> songBaseFolders = new HashMap<>();

    // Cambiado: ahora mapa de canción a lista de creadores
    private final Map<String, List<String>> songCreators = new HashMap<>();

    private final Map<String, List<String>> songTags = new HashMap<>();

    public Map<String, String> loadSongsFromFolder(File folder) {
        songs.clear();
        songBaseFolders.clear();
        songTags.clear();
        songCreators.clear();

        if (folder == null || !folder.exists() || !folder.isDirectory()) return songs;

        File[] beatmapFolders = folder.listFiles(File::isDirectory);
        if (beatmapFolders == null) return songs;

        for (File beatmap : beatmapFolders) {
            File[] osuFiles = beatmap.listFiles((dir, name) -> name.toLowerCase().endsWith(".osu"));
            if (osuFiles == null || osuFiles.length == 0) continue;

            for (File osuFile : osuFiles) {
                SongMetadata meta = parseOsuFile(osuFile);
                if (meta != null && meta.audioFilename != null) {
                    File audioFile = new File(beatmap, meta.audioFilename);
                    if (audioFile.exists()) {
                        String displayName = meta.artist + " - " + meta.title;
                        if (!songs.containsKey(displayName)) {
                            songs.put(displayName, audioFile.getAbsolutePath());
                            songBaseFolders.put(displayName, beatmap.getAbsolutePath());

                            List<String> tags = parseTags(osuFile);
                            songTags.put(displayName, tags);

                            // Cambiado: guardamos lista con 1 creador o lista vacía
                            List<String> creators = parseCreators(osuFile);
                            songCreators.put(displayName, creators);
                        }
                        break;
                    }
                }
            }
        }
        return songs;
    }

    public String getSongPath(String songName) {
        return songs.get(songName);
    }

    public void setLastFolderPath(String path) {
        this.lastFolderPath = path;
    }

    public String getLastFolderPath() {
        return lastFolderPath;
    }

    public String getSongBaseFolder(String songName) {
        return songBaseFolders.get(songName);
    }

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

    private List<String> parseTags(File osuFile) {
        List<String> tags = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(osuFile), StandardCharsets.UTF_8))) {
            String line;
            boolean inMetadataSection = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("[Metadata]")) {
                    inMetadataSection = true;
                } else if (inMetadataSection) {
                    if (line.startsWith("Tags:")) {
                        String tagLine = line.substring(5).trim();
                        String[] splitTags = tagLine.split("\\s+");
                        for (String tag : splitTags) {
                            if (!tag.isEmpty()) tags.add(tag);
                        }
                        break;
                    } else if (line.startsWith("[") && line.endsWith("]")) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tags;
    }

    // Cambiado: parseCreators devuelve lista con 1 creador (si existe)
    private List<String> parseCreators(File osuFile) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(osuFile), StandardCharsets.UTF_8))) {
            String line;
            boolean inMetadataSection = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("[Metadata]")) {
                    inMetadataSection = true;
                } else if (inMetadataSection) {
                    if (line.startsWith("Creator:")) {
                        String creator = line.substring(8).trim();
                        if (!creator.isEmpty()) {
                            return Collections.singletonList(creator);
                        } else {
                            return Collections.emptyList();
                        }
                    } else if (line.startsWith("[") && line.endsWith("]")) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<String> getTags(String songName) {
        return songTags.getOrDefault(songName, Collections.emptyList());
    }

    // Nuevo método esperado por UIController
    public List<String> getCreators(String songName) {
        return songCreators.getOrDefault(songName, Collections.emptyList());
    }

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
