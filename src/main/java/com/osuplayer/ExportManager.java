package com.osuplayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * Encargado de exportar canciones individuales o listas de reproducción completas.
 */
public class ExportManager {

    private final MusicManager musicManager;

    public ExportManager(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    /**
     * Exporta una canción individual a la carpeta "Exportado".
     * @param songName nombre de la canción a exportar
     */
    public void exportSong(String songName) {
        String songPath = musicManager.getSongPath(songName);
        if (songPath == null) return;

        try {
            File exportDir = new File("Exportado");
            if (!exportDir.exists()) exportDir.mkdirs();

            String extension = "";
            int dotIndex = songPath.lastIndexOf('.');
            if (dotIndex != -1) {
                extension = songPath.substring(dotIndex);
            }
            String safeName = songName.replaceAll("[\\\\/:*?\"<>|]", "_");
            File exportFile = new File(exportDir, safeName + extension);

            Files.copy(new File(songPath).toPath(), exportFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Canción exportada a: " + exportFile.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Exporta todas las canciones de una playlist.
     * @param playlistName nombre de la playlist
     * @param playlists mapa con el contenido de las playlists
     */
    public void exportPlaylist(String playlistName, Map<String, List<String>> playlists) {
        if (playlistName == null) return;
        List<String> songs = playlists.get(playlistName);
        if (songs == null || songs.isEmpty()) return;

        try {
            File exportDir = new File("Exportado");
            if (!exportDir.exists()) exportDir.mkdirs();

            for (String song : songs) {
                String songPath = musicManager.getSongPath(song);
                if (songPath != null) {
                    File sourceFile = new File(songPath);
                    String extension = "";
                    int dotIndex = songPath.lastIndexOf('.');
                    if (dotIndex != -1) {
                        extension = songPath.substring(dotIndex);
                    }
                    String safeName = song.replaceAll("[\\\\/:*?\"<>|]", "_");
                    File destFile = new File(exportDir, safeName + extension);
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.out.println("Playlist exportada a carpeta: " + exportDir.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
