package com.osuplayer.dependencies;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DependencyLoader {
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    public void loadOrThrow() {
        // that's atomic check-and-set
        // that using compareAndExchange to avoid double loading in multithreaded scenarios
        // for example, if two threads call loadOrThrow simultaneously,
        // only one of them will proceed to loadCore, while the other will return immediately
        // if you potentially used there boolean instead of AtomicBoolean,
        // there could be a race condition where both threads see loaded as false
        // and both proceed to loadCore, leading to double loading
        // AtomicBoolean also faster than synchronized block in this case, much more lightweight and efficient

        if (loaded.compareAndExchange(false, true)) return;

        try {
            loadCore();
        } catch (Exception ex) {
            loaded.set(false);
            throw ex;
        }
    }

    public Boolean loadWithResult() {
        try {
            loadOrThrow();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected abstract void loadCore();
}
