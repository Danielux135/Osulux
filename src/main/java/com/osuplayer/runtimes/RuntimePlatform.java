package com.osuplayer.runtimes;

import com.osuplayer.common.KeyValuePair;

import java.util.List;

public enum RuntimePlatform {
    WINDOWS,
    LINUX,
    OSX;

    public static final RuntimePlatform CURRENT = current();

    private static RuntimePlatform current() {
        var platform = System.getProperty("os.name").toLowerCase();

        for (var context : getPlatformsPatterns()) {
            if (platform.contains(context.key())) {
                return context.value();
            }
        }

        throw new UnsupportedOperationException("Unsupported operating system: " + platform);
    }

    private static List<KeyValuePair<String, RuntimePlatform>> getPlatformsPatterns() {
        return List.of(
            new KeyValuePair<>("win", WINDOWS),
            new KeyValuePair<>("mac", OSX),
            new KeyValuePair<>("nix", LINUX),
            new KeyValuePair<>("nux", LINUX),
            new KeyValuePair<>("aix", LINUX)
        );
    }
}
