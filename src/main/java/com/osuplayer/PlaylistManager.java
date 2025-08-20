package com.osuplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlaylistManager {

    private final ConfigManager configManager;
    private final Map<String, List<String>> playlists;
    private final Set<String> specialPlaylists = Set.of("Todo", "Favoritos", "Historial");

    public PlaylistManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.playlists = new LinkedHashMap<>();
        loadPlaylists();
    }

    private void loadPlaylists() {
        Map<String, List<String>> loaded = configManager.getPlaylists();
        if (loaded != null) {
            playlists.putAll(loaded);
        }
        for (String special : specialPlaylists) {
            playlists.putIfAbsent(special, new ArrayList<>());
        }
        savePlaylists();
    }

    public boolean isSpecialPlaylist(String name) {
        return specialPlaylists.contains(name);
    }

    public boolean createPlaylist(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || playlists.containsKey(trimmed) || isSpecialPlaylist(trimmed)) {
            return false;
        }
        playlists.put(trimmed, new ArrayList<>());
        savePlaylists();
        return true;
    }

    public void deletePlaylist(String name) {
        if (!isSpecialPlaylist(name)) {
            playlists.remove(name);
            savePlaylists();
        }
    }

    public void addToPlaylist(String playlist, String song) {
        playlists.computeIfAbsent(playlist, k -> new ArrayList<>());
        if (!playlists.get(playlist).contains(song)) {
            playlists.get(playlist).add(song);
            savePlaylists();
        }
    }

    public void removeFromPlaylist(String playlist, String song) {
        if (playlists.containsKey(playlist)) {
            playlists.get(playlist).remove(song);
            savePlaylists();
        }
    }

    public List<String> getPlaylist(String name) {
        return playlists.getOrDefault(name, Collections.emptyList());
    }

    public Set<String> getAllPlaylists() {
        return playlists.keySet();
    }

    public Map<String, List<String>> getPlaylistsAsMap() {
        return Collections.unmodifiableMap(playlists);
    }

    public void setPlaylistSongs(String playlist, List<String> songs) {
        if (playlist != null) {
            playlists.put(playlist, new ArrayList<>(songs));
            savePlaylists();
        }
    }

    public void savePlaylists() {
        configManager.setPlaylists(playlists);
    }
}