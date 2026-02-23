package com.osuplayer.shortcuts;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.osuplayer.dependencies.NativeHookDependencyLoader;
import com.osuplayer.discord.DiscordRichPresence;
import javafx.application.Platform;


public final class GlobalMediaKeyService implements NativeKeyListener {
    private static final Logger LOGGER = Logger.getLogger(DiscordRichPresence.class.getName());

    private final java.util.function.Consumer<ShortcutAction> actionHandler;
    private boolean started;

    public GlobalMediaKeyService(java.util.function.Consumer<ShortcutAction> actionHandler) {
        this.actionHandler = Objects.requireNonNull(actionHandler, "actionHandler");
    }

    public synchronized void start() {
        if (started) {
            return;
        }

        if (!NativeHookDependencyLoader.GLOBAL.loadWithResult()) {
            LOGGER.warning("No se pudieron cargar las dependencias de JNativeHook");
            return;
        }

        silenceNativeHookLogs();

        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            started = true;
        } catch (NativeHookException | UnsatisfiedLinkError | RuntimeException ex) {
            LOGGER.warning("No se pudo registrar el hook nativo: " + ex.getMessage());
        }
    }

    public synchronized void close() {
        if (!started) {
            return;
        }
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("No se pudo desregistrar el hook nativo: " + ex.getMessage());
        } finally {
            started = false;
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
        if (nativeEvent == null) {
            return;
        }
        ShortcutAction action = mapToAction(nativeEvent);
        if (action != null) {
            
            Platform.runLater(() -> actionHandler.accept(action));
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
        
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeEvent) {
        
    }

    private ShortcutAction mapToAction(NativeKeyEvent event) {
        
        return switch (event.getKeyCode()) {
            case NativeKeyEvent.VC_MEDIA_PREVIOUS, NativeKeyEvent.VC_F5 -> ShortcutAction.PREVIOUS_TRACK;
            case NativeKeyEvent.VC_MEDIA_NEXT, NativeKeyEvent.VC_F6 -> ShortcutAction.NEXT_TRACK;
            case NativeKeyEvent.VC_MEDIA_PLAY, NativeKeyEvent.VC_F7 -> ShortcutAction.PLAY_PAUSE;
            case NativeKeyEvent.VC_MEDIA_STOP, NativeKeyEvent.VC_F8 -> ShortcutAction.STOP;
            case NativeKeyEvent.VC_VOLUME_MUTE, NativeKeyEvent.VC_F9 -> ShortcutAction.VOLUME_MUTE;
            case NativeKeyEvent.VC_VOLUME_DOWN, NativeKeyEvent.VC_F10 -> ShortcutAction.VOLUME_DOWN;
            case NativeKeyEvent.VC_VOLUME_UP, NativeKeyEvent.VC_F11 -> ShortcutAction.VOLUME_UP;
            default -> null;
        };
    }

    private void silenceNativeHookLogs() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.SEVERE);
    }
}
