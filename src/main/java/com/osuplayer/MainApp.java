package com.osuplayer;

import javafx.application.Application;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class MainApp extends Application {

    // --- CAMBIO 1: Los objetos de VLCJ ahora son ESTÁTICOS ---
    // Esto es necesario para que el método estático 'main' pueda crearlos
    // y el método de instancia 'start' pueda acceder a ellos.
    private static MediaPlayerFactory factory;
    private static EmbeddedMediaPlayer audioPlayer;
    private static EmbeddedMediaPlayer videoPlayer;
    
    // Estos pueden seguir siendo campos de instancia, no hay problema.
    private ConfigManager configManager;
    private MusicManager musicManager;

    @Override
    public void start(Stage primaryStage) {
        // --- CAMBIO 3: La inicialización de VLCJ se ha ELIMINADO de aquí ---
        // La fábrica y los reproductores ya existen en este punto.
        
        // La creación de estos objetos está bien aquí.
        configManager = new ConfigManager();
        musicManager = new MusicManager();

        // El resto de tu código funciona igual, pero ahora usa los objetos estáticos.
        UIController ui = new UIController(audioPlayer, videoPlayer, configManager, musicManager);
        ui.start(primaryStage);

        // El manejador de cierre ahora accederá a los campos estáticos para liberar los recursos.
        primaryStage.setOnCloseRequest(event -> {
            if (audioPlayer != null) {
                audioPlayer.controls().stop();
                audioPlayer.release();
            }
            if (videoPlayer != null) {
                videoPlayer.controls().stop();
                videoPlayer.release();
            }
            if (factory != null) {
                factory.release();
            }
        });
    }

    public static void main(String[] args) {
        // --- CAMBIO 2: Toda la inicialización de VLCJ se mueve AQUÍ ---
        // Esto se ejecuta en el hilo principal, ANTES de que el hilo de la UI de JavaFX se inicie.
        factory = new MediaPlayerFactory("--input-title-format=Osulux");
        audioPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
        videoPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
        
        // Una vez que VLCJ está listo, lanzamos la aplicación JavaFX.
        launch(args);
    }
}