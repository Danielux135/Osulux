package com.osuplayer;

import java.util.*;

public class PlaylistManager {

    private final ConfigManager configManager;
    private final Map<String, List<String>> playlists;

    public PlaylistManager() {
        this.configManager = new ConfigManager();
        this.playlists = new HashMap<>();
        initPlaylists();
    }

    private void initPlaylists() {
        Map<String, List<String>> loaded = configManager.getPlaylists();
        if (loaded != null) {
            playlists.putAll(loaded);
        }
        if (!playlists.containsKey("Default")) {
            playlists.put("Default", new ArrayList<>());
            savePlaylists();
        }
    }

    public void createPlaylist(String name) {
        if (!playlists.containsKey(name)) {
            playlists.put(name, new ArrayList<>());
            savePlaylists();
        }
    }

    public void deletePlaylist(String name) {
        if (playlists.containsKey(name)) {
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
        return playlists.getOrDefault(name, new ArrayList<>());
    }

    public Set<String> getAllPlaylists() {
        return playlists.keySet();
    }

    public void savePlaylists() {
        configManager.setPlaylists(playlists);
    }
}
