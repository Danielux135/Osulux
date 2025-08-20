package com.osuplayer;

import java.util.function.Consumer;

import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

public class PlaylistCell extends ListCell<String> {

    private final Button openButton = new Button("Abrir");

    public PlaylistCell(Consumer<String> selectPlaylist) {
        openButton.setOnAction(e -> {
            String item = getItem();
            if (item != null) {
                selectPlaylist.accept(item);
            }
        });
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(item);
            HBox box = new HBox(5, openButton);
            setGraphic(box);
        }
    }
}
