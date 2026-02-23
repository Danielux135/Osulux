package com.osuplayer.dependencies;

import javafx.scene.image.Image;

import java.util.function.Supplier;

public final class IconDependencyProvider {
    private static final Supplier<Image> ICON_SUPPLIER = IconDependencyProvider::loadIcon;

    public static Image getOrNull() {
        return ICON_SUPPLIER.get();
    }

    private static Image loadIcon() {
        for (String path : getIconPaths()) {
            var imagePath = IconDependencyProvider.class.getResource(path);
            if (imagePath != null) {
                return new Image(imagePath.toExternalForm());
            }
        }
        return null;
    }

    private static String[] getIconPaths() {
        return new String[] {
            "/jpg/Icon.jpg",
            "/Icon.jpg"
        };
    }
}
