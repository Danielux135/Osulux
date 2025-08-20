package com.osuplayer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import java.util.List;
import java.util.Map;

public class PlaylistContextMenuHelper {

    public static ContextMenu createContextMenu(Map<String, List<String>> playlists) {
        ContextMenu menu = new ContextMenu();
        for (String name : playlists.keySet()) {
            MenuItem item = new MenuItem(name);
            menu.getItems().add(item);
        }
        return menu;
    }

    public static ContextMenu createContextMenu(List<String> playlists) {
        ContextMenu menu = new ContextMenu();
        for (String name : playlists) {
            MenuItem item = new MenuItem(name);
            menu.getItems().add(item);
        }
        return menu;
    }
}
