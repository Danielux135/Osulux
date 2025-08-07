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
    private final Map<String, List<String>> songCreators = new HashMap<>();
    private final Map<String, List<String>> songTags = new HashMap<>();

    private final HistoryManager historyManager = new HistoryManager();

    public Map<String, String> loadSongsFromFolder(File folder) {
        songs.clear();
        songBaseFolders.clear();
        songTags.clear();
        songCreators.clear();

        if (folder == null || !folder.exists() || !folder.isDirectory()) return songs;

        File[] beatmapFolders = folder.listFiles(File::isDirectory);
        if (beatmapFolders == null) return songs;

        for (File beatmapFolder : beatmapFolders) {
            File[] osuFiles = beatmapFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".osu"));
            if (osuFiles == null || osuFiles.length == 0) continue;

            for (File osuFile : osuFiles) {
                SongMetadata meta = parseOsuFile(osuFile);
                if (meta != null && meta.audioFilename != null) {
                    File audioFile = new File(beatmapFolder, meta.audioFilename);
                    if (audioFile.exists()) {
                        String displayName = meta.artist + " - " + meta.title;
                        if (!songs.containsKey(displayName)) {
                            songs.put(displayName, audioFile.getAbsolutePath());
                            songBaseFolders.put(displayName, beatmapFolder.getAbsolutePath());
                            songTags.put(displayName, parseTags(osuFile));
                            songCreators.put(displayName, parseCreators(osuFile));
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
                System.err.println("Error leyendo archivo " + osuFile.getAbsolutePath() + ": " + e.getMessage());
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

    public String getVideoPath(String songName) {
        String baseFolder = getSongBaseFolder(songName);
        if (baseFolder == null) return null;
    
        File folder = new File(baseFolder);
        File[] videoFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp4"));
        if (videoFiles != null && videoFiles.length > 0) {
            return videoFiles[0].getAbsolutePath();
        }
    
        return null;
    }

    private SongMetadata parseOsuFile(File osuFile) {
        String title = null, artist = null, audioFilename = null;

        Pattern titlePattern = Pattern.compile("^Title:(.*)$", Pattern.CASE_INSENSITIVE);
        Pattern artistPattern = Pattern.compile("^Artist:(.*)$", Pattern.CASE_INSENSITIVE);
        Pattern audioPattern = Pattern.compile("^AudioFilename:(.*)$", Pattern.CASE_INSENSITIVE);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(osuFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher mTitle = titlePattern.matcher(line);
                Matcher mArtist = artistPattern.matcher(line);
                Matcher mAudio = audioPattern.matcher(line);

                if (mTitle.find()) title = mTitle.group(1).trim();
                if (mArtist.find()) artist = mArtist.group(1).trim();
                if (mAudio.find()) audioFilename = mAudio.group(1).trim();

                if (title != null && artist != null && audioFilename != null) break;
            }
        } catch (IOException e) {
            System.err.println("Error leyendo archivo " + osuFile.getAbsolutePath() + ": " + e.getMessage());
        }

        if (title != null && artist != null && audioFilename != null) {
            return new SongMetadata(title, artist, audioFilename);
        } else {
            return null;
        }
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
                        if (!tagLine.isEmpty()) {
                            String[] splitTags = tagLine.split("\\s+");
                            Collections.addAll(tags, splitTags);
                        }
                        break;
                    } else if (line.startsWith("[") && line.endsWith("]")) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo archivo " + osuFile.getAbsolutePath() + ": " + e.getMessage());
        }
        return tags;
    }

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
                        return creator.isEmpty() ? Collections.emptyList() : Collections.singletonList(creator);
                    } else if (line.startsWith("[") && line.endsWith("]")) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo archivo " + osuFile.getAbsolutePath() + ": " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<String> getTags(String songName) {
        return songTags.getOrDefault(songName, Collections.emptyList());
    }

    public List<String> getCreators(String songName) {
        return songCreators.getOrDefault(songName, Collections.emptyList());
    }

    public List<String> searchSongs(String query) {
        if (query == null || query.isEmpty()) return new ArrayList<>(songs.keySet());

        String lowerQuery = query.toLowerCase();
        List<String> results = new ArrayList<>();

        for (String name : songs.keySet()) {
            if (name.toLowerCase().contains(lowerQuery)) {
                results.add(name);
                continue;
            }

            List<String> tags = songTags.getOrDefault(name, Collections.emptyList());
            for (String tag : tags) {
                if (tag.toLowerCase().contains(lowerQuery)) {
                    results.add(name);
                    break;
                }
            }
        }
        return results;
    }

    public void addToHistory(String songName) {
        historyManager.addSong(songName);
    }

    public String getPreviousFromHistory() {
        return historyManager.getPrevious();
    }

    public String getNextFromHistory() {
        return historyManager.getNext();
    }

    public boolean hasPreviousInHistory() {
        return historyManager.hasPrevious();
    }

    public boolean hasNextInHistory() {
        return historyManager.hasNext();
    }

    public String getCurrentHistorySong() {
        return historyManager.getCurrent();
    }

    public void clearHistory() {
        historyManager.clear();
    }

    public List<String> getHistory() {
        return historyManager.getHistory();
    }

    public int getHistoryIndex() {
        return historyManager.getIndex();
    }

    public void setHistoryIndex(int index) {
        historyManager.setIndex(index);
    }

    public void setHistory(List<String> history, int index) {
        historyManager.setHistory(history, index);
    }

    private static class SongMetadata {
        final String title;
        final String artist;
        final String audioFilename;

        SongMetadata(String title, String artist, String audioFilename) {
            this.title = title;
            this.artist = artist;
            this.audioFilename = audioFilename;
        }
    }
}
