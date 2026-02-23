package com.osuplayer.dependencies;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class DependencyDirectoryProvider {
    private static final Logger LOGGER = Logger.getLogger(DependencyDirectoryProvider.class.getName());

    public static Path getOrThrow(String name) {
        var dependencyDirectory = getOrNull(name);

        if (dependencyDirectory == null) {
            throw new IllegalStateException("Dependency "+ name + " directory not found");
        }

        return dependencyDirectory;
    }

    public static Path getOrNull(String name) {
        // checking first for environment variable overrides by user
        // only if not found then extracting from resources
        // if null returned then dependency is not available anywhere

        var environmentDirectory = getDependencyFromEnvironment(name);
        if (environmentDirectory != null) return environmentDirectory;

        var storageDirectory = getDependencyFromStorage(name);
        if (storageDirectory != null) return storageDirectory;

        return getDependencyFromResources(name);
    }

    private static Path getDependencyFromResources(String name) {
        var resourceDirectory = getResourceDirectory(name);
        if (resourceDirectory == null) return null;

        var temporalDirectory = getTemporalDirectory(name);
        if (temporalDirectory == null) return null;

        var extractionResult = extractResourceDirectory(resourceDirectory, temporalDirectory);
        if (!extractionResult) return null;

        // we need to schedule deletion of the temporal directory on program closure
        // that not important if deletion fails for some reason
        // however we try our faster cleanup of the temporal directory
        // to avoid littering the user's temp folder with unused data
        scheduleTemporalDirectoryDeletion(temporalDirectory);

        return temporalDirectory;
    }

    private static Boolean extractResourceDirectory(URL resourceDirectory, Path temporalDirectory) {
        // there implemented support for jar and file protocols only
        // the jar protocol is used when running from packaged jar
        // the file protocol is used when running from ide or unpacked classes folder
        // without file protocol running in ide would be impossible and can get errors, idk

        var protocol = resourceDirectory.getProtocol();

        if (protocol.equals("jar")) return extractJavaResourceDirectory(resourceDirectory, temporalDirectory);
        else if (protocol.equals("file")) return extractFileResourceDirectory(resourceDirectory, temporalDirectory);
        else return false;
    }

    private static Boolean extractJavaResourceDirectory(URL resourceDirectory, Path temporalDirectory) {
        var resourcePath = resourceDirectory.toString();

        var jarFilePath = getJavaFilePath(resourcePath);
        if (jarFilePath == null) return false;

        var internalRootPath = getJavaInternalPath(resourcePath);

        try (JarFile jarFile = new JarFile(jarFilePath)) {
            var entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                Path entryPath = Paths.get(entry.getName());

                // skip all except ones that are inside the internal root
                // for example lib/vlc/somefile.dll will be inside lib/vlc
                // but lib/otherfile.txt will be outside
                if (!entryPath.startsWith(internalRootPath)) continue;

                // create relative path from internal root to entry
                // for example lib/vlc/somefile.dll to somefile.dll where internal root is lib/vlc
                Path relativePath = internalRootPath.relativize(entryPath);
                Path destinationPath = temporalDirectory.resolve(relativePath);

                // entry can be directory or file
                if (entry.isDirectory()) {
                    Files.createDirectories(destinationPath);
                } else {
                    Files.createDirectories(destinationPath.getParent());

                    // coping with replacement if already exists
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ignored) {
                        LOGGER.warning("Can't extract file from jar resource: " + entry.getName());
                        return false;
                    }
                }
            }

            return true;
        } catch (IOException ignored) {
            LOGGER.warning("Can't extract jar resource directory: " + resourceDirectory);
            return false;
        }
    }

    private static Boolean extractFileResourceDirectory(URL resourceDirectory, Path temporalDirectory) {
        try {
            var sourceDirectory = Paths.get(resourceDirectory.toURI());

            try (var walk = Files.walk(sourceDirectory)) {
                walk.forEach(fileOrDirectory -> {
                    var relativeFileOrDirectory = sourceDirectory.relativize(fileOrDirectory);
                    var destinationFileOrDirectory = temporalDirectory.resolve(relativeFileOrDirectory);

                    try {
                        if (Files.isDirectory(fileOrDirectory)) {
                            Files.createDirectories(destinationFileOrDirectory);
                            return;
                        }

                        Files.createDirectories(destinationFileOrDirectory.getParent());
                        Files.copy(fileOrDirectory, destinationFileOrDirectory, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }

            return true;
        } catch (IOException | URISyntaxException | RuntimeException ignored) {
            LOGGER.warning("Can't extract file resource directory: " + resourceDirectory);
            return false;
        }
    }

    private static void scheduleTemporalDirectoryDeletion(Path temporalDirectory) {
        try {
            // schedule deletion of the temporal directory on closure of the program
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (!Files.exists(temporalDirectory)) return;

                // using try for cleanup of the walk stream automatically
                try (var walk = Files.walk(temporalDirectory)) {
                    // sorting as reverseOrder to delete files before directories
                    walk.sorted(Comparator.reverseOrder()).forEach(fileOrDirectory -> {
                        try {
                            Files.delete(fileOrDirectory);
                        } catch (IOException ignored) {
                            LOGGER.warning("Can't delete temporary file or directory: " + fileOrDirectory);
                        }
                    });
                } catch (IOException ignored) {
                    LOGGER.warning("Can't cleanup temporary directory: " + temporalDirectory);
                }
            }));
        } catch (Throwable ignored) {
            LOGGER.warning("Can't schedule deletion of temporary directory: " + temporalDirectory);
        }
    }

    private static URL getResourceDirectory(String name)
    {
        var resourceName = "/lib/" + name;

        return DependencyDirectoryProvider.class.getResource(resourceName);
    }

    private static Path getTemporalDirectory(String name) {
        try {
            // temporal directory living only during the program execution
            // and then deleted potentially on program closure or jvm closure or system shutdown
            // also that has randomized postfix to avoid conflicts of same named directories
            return Files.createTempDirectory("osulux_" + name + "_extract_");
        } catch (IOException ignored) {
            LOGGER.warning("Can't create temporal directory for dependency " + name);
            return null;
        }
    }

    private static Path getJavaInternalPath(String path) {
        // from url like: jar:file:/path/to/jarfile.jar!/lib/vlc
        // (that is are specific format that java uses for resources inside jars)
        // that is why needed to extract only the internal path: lib/vlc

        var internalPath = path;

        var internalSeparatorIndex = internalPath.lastIndexOf('!');
        if (internalSeparatorIndex >= 1) {
            internalPath = internalPath.substring(internalSeparatorIndex + 1);
        }

        if (internalPath.startsWith("/")) {
            internalPath = internalPath.substring(1);
        }

        return Paths.get(FormaSystemPath(internalPath));
    }

    private static File getJavaFilePath(String path) {
        // from url like: jar:file:/path/to/jarfile.jar!/lib/vlc
        // (that is are specific format that java uses for resources inside jars)
        // that is why needed to extract only the jar file path: /path/to/jarfile.jar

        var javaFilePath = path;

        var internalSeparatorIndex = javaFilePath.lastIndexOf('!');
        if (internalSeparatorIndex >= 1) {
            javaFilePath = javaFilePath.substring(0, internalSeparatorIndex);
        }

        if (!javaFilePath.endsWith(".jar")) return null;

        var protocolSeparatorIndex = javaFilePath.lastIndexOf(':');
        if (protocolSeparatorIndex >= 1) {
            javaFilePath = javaFilePath.substring(protocolSeparatorIndex + 1);
        }

        return Paths.get(FormaSystemPath(javaFilePath)).toFile();
    }

    private static String FormaSystemPath(String path) {
        // decoding percent-encoded characters
        // for example spaces encoded as %20 to proper spaces
        // that is important for paths with spaces and special characters

        return URLDecoder.decode(path, StandardCharsets.UTF_8);
    }

    private static Path getDependencyFromStorage(String name) {
        var storageDirectory = Path.of("lib", name);

        if (!Files.isDirectory(storageDirectory)) return null;

        return storageDirectory.toAbsolutePath();
    }

    private static Path getDependencyFromEnvironment(String name) {
        var environmentName = "OSULUX_" + name.toUpperCase() + "_DIR";

        var environmentValue = System.getenv(environmentName);
        if (environmentValue == null || environmentValue.isBlank()) return null;

        // we do check there for not overload code upper
        // all in one simple function, same to getTemporalDirectory or getResourceDirectory
        if (!Files.isDirectory(Path.of(environmentValue))) return null;

        return Path.of(FormaSystemPath(environmentValue));
    }
}
