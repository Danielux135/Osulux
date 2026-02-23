package com.osuplayer.dependencies;

import com.osuplayer.runtimes.RuntimeContext;

import java.nio.file.Path;

public class RuntimeDependencyLocator {
    private static final Path DIRECTORY = createDirectory();

    public static Path directory() {
        return DIRECTORY;
    }

    public static Path file(String name) {
        var runtime = RuntimeContext.CURRENT;

        var extension = switch (runtime.platform()) {
            case WINDOWS -> ".dll";
            case LINUX -> ".so";
            case OSX -> ".dylib";
            default -> throw new UnsupportedOperationException("Unsupported operating system: " + runtime.platform());
        };

        return Path.of(DIRECTORY.toString(), name + extension);
    }

    private static Path createDirectory() {
        var runtime = RuntimeContext.CURRENT;

        return Path.of(
            runtime.platform().toString().toLowerCase(),
            runtime.architecture().toString().toLowerCase()
        );
    }
}
