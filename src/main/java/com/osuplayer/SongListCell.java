package com.osuplayer;

import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class SongListCell extends ListCell<String> {

    private final PlaylistManager playlistManager;
    private final FavoritesManager favoritesManager;
    private final ExportManager exportManager;
    private final Runnable refreshUICallback;

    public SongListCell(PlaylistManager playlistManager, FavoritesManager favoritesManager, ExportManager exportManager, Runnable refreshUICallback) {
        this.playlistManager = playlistManager;
        this.favoritesManager = favoritesManager;
        this.exportManager = exportManager;
        this.refreshUICallback = refreshUICallback;
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty ? null : item);

        if (empty || item == null) {
            setContextMenu(null);
            return;
        }
        
        setContextMenu(createSongContextMenu(item));
    }

    private ContextMenu createSongContextMenu(String song) {
        ContextMenu contextMenu = new ContextMenu();
        
        Menu addToPlaylistMenu = new Menu("Añadir a playlist");
        MenuItem addFavItem = new MenuItem("Favoritos");
        addFavItem.setOnAction(e -> {
            favoritesManager.addFavorite(song);
            refreshUICallback.run();
        });
        addToPlaylistMenu.getItems().add(addFavItem);

        for (String playlistName : playlistManager.getAllPlaylists()) {
            if (!playlistManager.isSpecialPlaylist(playlistName)) {
                MenuItem playlistItem = new MenuItem(playlistName);
                playlistItem.setOnAction(ev -> {
                    playlistManager.addToPlaylist(playlistName, song);
                    refreshUICallback.run();
                });
                addToPlaylistMenu.getItems().add(playlistItem);
            }
        }

        Menu removeFromPlaylistMenu = new Menu("Eliminar de");
        boolean inAnyPlaylist = false;
        for (String playlistName : playlistManager.getAllPlaylists()) {
            if (!playlistName.equalsIgnoreCase("Todo")) {
                List<String> list = playlistManager.getPlaylist(playlistName);
                if (list != null && list.contains(song)) {
                    inAnyPlaylist = true;
                    MenuItem removeFromItem = new MenuItem(playlistName);
                    removeFromItem.setOnAction(e -> {
                        if (playlistName.equals("Favoritos")) {
                            favoritesManager.removeFavorite(song);
                        } else {
                            playlistManager.removeFromPlaylist(playlistName, song);
                        }
                        refreshUICallback.run();
                    });
                    removeFromPlaylistMenu.getItems().add(removeFromItem);
                }
            }
        }

        MenuItem exportSongItem = new MenuItem("Exportar canción");
        exportSongItem.setOnAction(e -> exportManager.exportSong(song));

        contextMenu.getItems().add(addToPlaylistMenu);
        if (inAnyPlaylist) {
            contextMenu.getItems().add(removeFromPlaylistMenu);
        }
        contextMenu.getItems().add(exportSongItem);

        return contextMenu;
    }
}