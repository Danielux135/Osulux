package com.osuplayer.runtimes;

import com.osuplayer.common.KeyValuePair;

import java.util.List;

public enum RuntimeArchitecture {
    X86,
    X64,
    ARM64;

    public static final RuntimeArchitecture CURRENT = current();

    private static RuntimeArchitecture current() {
        var architecture = System.getProperty("os.arch").toLowerCase();

        for (var context : getArchitecturesPatterns()) {
            if (architecture.contains(context.key())) {
                return context.value();
            }
        }

        throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
    }

    private static List<KeyValuePair<String, RuntimeArchitecture>> getArchitecturesPatterns() {
        return List.of(
            new KeyValuePair<>("amd64", X64),
            new KeyValuePair<>("x86_64", X64),
            new KeyValuePair<>("aarch64", ARM64),
            new KeyValuePair<>("arm64", ARM64),
            new KeyValuePair<>("x86", X86),
            new KeyValuePair<>("i386", X86),
            new KeyValuePair<>("i486", X86),
            new KeyValuePair<>("i586", X86),
            new KeyValuePair<>("i686", X86)
        );
    }
}
