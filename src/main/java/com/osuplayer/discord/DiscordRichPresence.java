package com.osuplayer.discord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.osuplayer.dependencies.DiscordDependencyLoader;
import com.osuplayer.lang.I18n;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityType;

public class DiscordRichPresence {

    private static final Logger LOGGER = Logger.getLogger(DiscordRichPresence.class.getName());

    private Core core;
    private final Thread callbackThread;
    private volatile boolean running = false;
    private final Random random = new Random();
    private static final double RARE_COVER_CHANCE = 0.01d;
    private static final Set<String> RARE_COVER_KEYS = Set.of("coverrare1", "coverrare2");
    
    private static final List<String> COVER_ASSETS = Arrays.asList(
        "cover1",
        "cover2",
        "cover3",
        "cover4",
        "cover5",
        "cover6",
        "cover7",
        "cover8",
        "cover9",
        "coverrare1",
        "coverrare2"
    );

    public DiscordRichPresence() {
        this.callbackThread = new Thread(() -> {
            while (running) {
                try {
                    if (core != null) {
                        core.runCallbacks();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Error al procesar callbacks de Discord", e);
                }
                if (!running) {
                    break;
                }
                LockSupport.parkNanos(16_000_000L);
            }
        }, "Discord-Callback-Thread");
    }

    public void start(long clientId) {
        if (!DiscordDependencyLoader.GLOBAL.loadWithResult()) {
            LOGGER.severe("No se pudieron cargar las dependencias de Discord SDK");
            return;
        }

        try (CreateParams params = new CreateParams()) {
            params.setClientID(clientId);
            params.setFlags(CreateParams.getDefaultFlags());
            this.core = new Core(params);
            this.running = true;
            this.callbackThread.start();
            setIdleStatus();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "No se pudo inicializar Discord SDK", e);
        }
    }

    public void stop() {
        if (!running) return;
        running = false;
        LockSupport.unpark(callbackThread);
        try {
            callbackThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrumpido mientras se esperaba al hilo de Discord", e);
        }
        
        if (core != null) {
            try {
                core.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "No se pudo cerrar Discord SDK limpiamente", e);
            }
        }
    }

    public void updateStatus(String songTitle, String artist, long currentTimeMillis, long totalDurationMillis) {
        if (core == null || !running) return;
        try (Activity activity = new Activity()) {
            activity.setType(ActivityType.LISTENING);
            activity.setDetails(songTitle);
            activity.setState(I18n.trf("por %s", artist));
            activity.assets().setLargeText(I18n.tr("Escuchando en Osulux"));

            activity.assets().setLargeImage(selectCoverAsset());
            
            if (totalDurationMillis > 0) {
                Instant now = Instant.now();
                activity.timestamps().setStart(now.minusMillis(currentTimeMillis));
                activity.timestamps().setEnd(now.plusMillis(totalDurationMillis - currentTimeMillis));
            }
            
            core.activityManager().updateActivity(activity);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error al actualizar la actividad de Discord", e);
        }
    }

    public void setIdleStatus() {
        if (core == null || !running) return;
        try (Activity activity = new Activity()) {
            activity.setType(ActivityType.LISTENING);
            activity.setDetails(I18n.tr("Navegando por la música"));
            activity.setState(I18n.tr("en Osulux"));
            activity.assets().setLargeImage("osulux-logo");
            activity.assets().setLargeText("Osulux Music Player");
            core.activityManager().updateActivity(activity);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error al actualizar el estado inactivo de Discord", e);
        }
    }

    private String selectCoverAsset() {
        if (COVER_ASSETS.isEmpty()) {
            return "osulux-logo";
        }

        List<String> rareCovers = new ArrayList<>();
        List<String> commonCovers = new ArrayList<>();
        for (String asset : COVER_ASSETS) {
            if (RARE_COVER_KEYS.contains(asset)) {
                rareCovers.add(asset);
            } else {
                commonCovers.add(asset);
            }
        }

        if (!rareCovers.isEmpty()) {
            double totalRareChance = Math.min(RARE_COVER_CHANCE * rareCovers.size(), 0.99d);
            if (random.nextDouble() < totalRareChance) {
                return rareCovers.get(random.nextInt(rareCovers.size()));
            }
        }

        if (!commonCovers.isEmpty()) {
            return commonCovers.get(random.nextInt(commonCovers.size()));
        }

        return rareCovers.isEmpty() ? "osulux-logo" : rareCovers.get(random.nextInt(rareCovers.size()));
    }
}
