package com.osuplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesManager {

    private final Set<String> favorites;
    private final ConfigManager configManager;
    private final PlaylistManager playlistManager;

    public FavoritesManager(ConfigManager configManager, PlaylistManager playlistManager) {
        this.configManager = configManager;
        this.playlistManager = playlistManager;
        this.favorites = new HashSet<>(configManager.getFavorites());
    }

    public void addFavorite(String song) {
        if (song != null && favorites.add(song)) {
            saveFavorites();
        }
    }

    public void removeFavorite(String song) {
        if (song != null && favorites.remove(song)) {
            saveFavorites();
        }
    }

    public void toggleFavorite(String song) {
        if (isFavorite(song)) {
            removeFavorite(song);
        } else {
            addFavorite(song);
        }
    }

    public boolean isFavorite(String song) {
        return song != null && favorites.contains(song);
    }

    public Set<String> getFavorites() {
        return Collections.unmodifiableSet(favorites);
    }
    
    public void validateFavorites(Set<String> allAvailableSongs) {
        if (favorites.retainAll(allAvailableSongs)) {
            saveFavorites();
        }
    }

    private void saveFavorites() {
        List<String> favList = new ArrayList<>(favorites);
        configManager.setFavorites(favList);
        playlistManager.setPlaylistSongs("Favoritos", favList);
    }
}