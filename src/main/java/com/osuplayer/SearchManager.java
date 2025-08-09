package com.osuplayer;

import java.util.List;

import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TextField;

public class SearchManager {

    private final MusicManager musicManager;

    public SearchManager(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    public void attachSearchField(TextField searchField, FilteredList<String> filteredSongList) {
        searchField.textProperty().addListener((obs, o, n) -> {
            String query = n == null ? "" : n.toLowerCase();
            filteredSongList.setPredicate(item -> matchesQuery(item, query));
        });
    }

    public boolean matchesQuery(String songName, String query) {
        if (query == null || query.isEmpty()) return true;
        if (songName == null) return false;

        if (songName.toLowerCase().contains(query)) return true;

        List<String> tags = musicManager.getTags(songName);
        if (tags != null) {
            for (String tag : tags) {
                if (tag.toLowerCase().contains(query)) return true;
            }
        }

        List<String> creators = musicManager.getCreators(songName);
        if (creators != null) {
            for (String creator : creators) {
                if (creator.toLowerCase().contains(query)) return true;
            }
        }

        return false;
    }
}
