package com.osuplayer;

import java.util.HashSet;
import java.util.Set;

public class FavoritesManager {
    private final Set<String> favorites = new HashSet<>();

    public void setFavorites(Set<String> favs) {
        favorites.clear();
        if (favs != null) favorites.addAll(favs);
    }

    public void addFavorite(String song) {
        favorites.add(song);
    }

    public void removeFavorite(String song) {
        favorites.remove(song);
    }

    public boolean isFavorite(String song) {
        return favorites.contains(song);
    }

    public Set<String> getFavorites() {
        return new HashSet<>(favorites);
    }
}
