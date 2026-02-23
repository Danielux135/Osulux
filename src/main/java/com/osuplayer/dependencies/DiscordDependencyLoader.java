package com.osuplayer.dependencies;

import de.jcm.discordgamesdk.Core;

import java.io.File;
import java.nio.file.Path;

public class DiscordDependencyLoader extends DependencyLoader {
    public static final DiscordDependencyLoader GLOBAL = new DiscordDependencyLoader();

    @Override
    protected void loadCore() {
        Core.init(getLibraryFile());
    }

    private File getLibraryFile() {
        var libraryGlobalDirectory = DependencyDirectoryProvider.getOrThrow("discord");
        var libraryLocalDirectory = RuntimeDependencyLocator.file("discord_game_sdk");
        var libraryFile = Path.of(libraryGlobalDirectory.toString(), libraryLocalDirectory.toString()).toFile();

        if (!libraryFile.exists()) {
            throw new RuntimeException("Can't find Discord Game SDK library at " + libraryFile);
        }

        return libraryFile;
    }
}
