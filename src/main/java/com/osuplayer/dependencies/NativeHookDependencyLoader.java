package com.osuplayer.dependencies;

import java.io.File;
import java.nio.file.Path;

public class NativeHookDependencyLoader extends DependencyLoader {
    public static final NativeHookDependencyLoader GLOBAL = new NativeHookDependencyLoader();

    @Override
    protected void loadCore() {
        registerLibrary(getLibraryFile());
    }

    private File getLibraryFile() {
        var libraryGlobalDirectory = DependencyDirectoryProvider.getOrThrow("native_hook");
        var libraryLocalDirectory = RuntimeDependencyLocator.file("native_hook");
        var libraryFile = Path.of(libraryGlobalDirectory.toString(), libraryLocalDirectory.toString()).toFile();

        if (!libraryFile.exists()) {
            throw new RuntimeException("Can't find JNativeHook library at " + libraryFile);
        }

        return libraryFile;
    }

    private void registerLibrary(File libraryFile) {
        System.load(libraryFile.getPath());
        System.setProperty("jnativehook.lib.location", libraryFile.getParent());
    }
}
