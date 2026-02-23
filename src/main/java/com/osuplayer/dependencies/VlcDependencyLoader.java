package com.osuplayer.dependencies;

import com.sun.jna.NativeLibrary;
import uk.co.caprica.vlcj.binding.RuntimeUtil;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

import java.nio.file.Path;

public class VlcDependencyLoader extends DependencyLoader {
    public static final VlcDependencyLoader GLOBAL = new VlcDependencyLoader();

    @Override
    protected void loadCore() {
        registerLibraries();
        throwIfLibrariesBroken();
    }

    private void registerLibraries() {
        var librariesGlobalDirectory = DependencyDirectoryProvider.getOrThrow("vlc");
        var librariesLocalDirectory = RuntimeDependencyLocator.directory();
        var librariesDirectory = librariesGlobalDirectory.resolve(librariesLocalDirectory);

        System.setProperty("VLC_PLUGIN_PATH", Path.of(librariesDirectory.toString(), "plugins").toString());
        System.setProperty("jna.library.path", librariesGlobalDirectory.toString());
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), librariesDirectory.toString());
    }

    private void throwIfLibrariesBroken() {
        var found = new NativeDiscovery().discover();

        if (!found) {
            throw new RuntimeException("Failed to discover VLC native libraries");
        }
    }
}