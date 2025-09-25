package com.osuplayer;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityType;

public class DiscordRichPresence {

    private Core core;
    private final Thread callbackThread;
    private volatile boolean running = false;
    private final Random random = new Random();
    
    private static final List<String> COVER_ASSETS = Arrays.asList(
        "cover1",
        "cover2",
        "cover3",
        "cover4"
    );

    public DiscordRichPresence() {
        this.callbackThread = new Thread(() -> {
            while (running) {
                try {
                    if (core != null) {
                        core.runCallbacks();
                    }
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Discord-Callback-Thread");
    }

    public void start(long clientId) {
        try {
            Core.init(new File("lib/discord_game_sdk.dll"));
            try (CreateParams params = new CreateParams()) {
                params.setClientID(clientId);
                params.setFlags(CreateParams.getDefaultFlags());
                this.core = new Core(params);
                this.running = true;
                this.callbackThread.start();
                System.out.println("Discord SDK inicializado correctamente.");
                setIdleStatus();
            }
        } catch (Exception e) {
            System.err.println("Error fatal al inicializar el Discord SDK. ¿Está Discord abierto y los DLLs en la carpeta 'lib'?");
            e.printStackTrace();
        }
    }

    public void stop() {
        if (!running) return;
        System.out.println("Deteniendo el servicio de Discord...");
        running = false;
        try {
            callbackThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrumpido mientras se esperaba al hilo de Discord.");
        }
        
        if (core != null) {
            try {
                core.close();
                System.out.println("Discord SDK cerrado limpiamente.");
            } catch (Exception e) {
                // Ignored
            }
        }
    }

    public void updateStatus(String songTitle, String artist, long currentTimeMillis, long totalDurationMillis) {
        if (core == null || !running) return;
        try (Activity activity = new Activity()) {
            activity.setType(ActivityType.LISTENING);
            activity.setDetails(songTitle);
            activity.setState("por " + artist);
            activity.assets().setLargeText("Escuchando en Osulux");

            if (!COVER_ASSETS.isEmpty()) {
                int randomIndex = random.nextInt(COVER_ASSETS.size());
                String randomAssetKey = COVER_ASSETS.get(randomIndex);
                activity.assets().setLargeImage(randomAssetKey);
            } else {
                activity.assets().setLargeImage("osulux-logo");
            }
            
            if (totalDurationMillis > 0) {
                Instant now = Instant.now();
                activity.timestamps().setStart(now.minusMillis(currentTimeMillis));
                activity.timestamps().setEnd(now.plusMillis(totalDurationMillis - currentTimeMillis));
            }
            
            core.activityManager().updateActivity(activity);
        } catch (Exception e) {
            System.err.println("Error al actualizar la actividad de Discord: " + e.getMessage());
        }
    }

    public void setIdleStatus() {
        if (core == null || !running) return;
        try (Activity activity = new Activity()) {
            activity.setType(ActivityType.LISTENING);
            activity.setDetails("Navegando por la música");
            activity.setState("en Osulux");
            activity.assets().setLargeImage("osulux-logo");
            activity.assets().setLargeText("Osulux Music Player");
            core.activityManager().updateActivity(activity);
        } catch (Exception e) {
            System.err.println("Error al actualizar el estado inactivo de Discord: " + e.getMessage());
        }
    }
}