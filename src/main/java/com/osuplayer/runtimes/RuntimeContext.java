package com.osuplayer.runtimes;

public record RuntimeContext(RuntimePlatform platform, RuntimeArchitecture architecture) {
    public static RuntimeContext CURRENT = current();

    private static RuntimeContext current() {
        return new RuntimeContext(RuntimePlatform.CURRENT, RuntimeArchitecture.CURRENT);
    }
}
