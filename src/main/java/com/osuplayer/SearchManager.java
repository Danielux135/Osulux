package com.osuplayer;

import java.util.List;

import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class SearchManager {

    private final MusicManager musicManager;
    private FilteredList<String> filteredSongList;

    public SearchManager(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    public void setFilteredList(FilteredList<String> filteredSongList) {
        this.filteredSongList = filteredSongList;
    }

    public void setupSearchField(
            TextField searchField,
            ListView<String> songListView,
            Label currentSongLabel) {
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredSongList == null) return;

            String lower = newVal == null ? "" : newVal.toLowerCase().trim();
            
            filteredSongList.setPredicate(song -> {
                if (lower.isEmpty()) return true;
                return song != null && matchesQuery(song, lower);
            });

            String currentSong = currentSongLabel.getText();
            if (currentSong != null && !currentSong.isEmpty()) {
                int index = filteredSongList.indexOf(currentSong);
                if (index >= 0) {
                    songListView.getSelectionModel().select(index);
                    songListView.scrollTo(index);
                } else {
                    songListView.getSelectionModel().clearSelection();
                }
            }
        });
    }

    public boolean matchesQuery(String songName, String query) {
        if (query == null || query.isEmpty()) return true;
        if (songName == null) return false;

        // 1. Buscar en "Artista - Título"
        String lowerCaseSong = songName.toLowerCase();
        if (lowerCaseSong.contains(query)) return true;

        // 2. Buscar en la lista de Creadores
        List<String> creators = musicManager.getCreators(songName);
        if (creators != null) {
            for (String creator : creators) {
                if (creator.toLowerCase().contains(query)) return true;
            }
        }
        
        // 3. Buscar en la lista de Tags
        List<String> tags = musicManager.getTags(songName);
        if (tags != null) {
            for (String tag : tags) {
                if (tag.toLowerCase().contains(query)) return true;
            }
        }
        
        return false;
    }
}