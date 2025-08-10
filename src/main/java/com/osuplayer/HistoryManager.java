package com.osuplayer;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private final List<String> history = new ArrayList<>();
    private int currentIndex = -1;

    /**
     * Añade una canción al historial. Si ya estaba, la mueve al final.
     */
    public void addSong(String songName) {
        if (songName == null) return;
        history.remove(songName);
        history.add(songName);
        currentIndex = history.size() - 1;
    }

    /**
     * Devuelve true si hay canción anterior en el historial.
     */
    public boolean hasPrevious() {
        return currentIndex > 0;
    }

    /**
     * Devuelve true si hay canción siguiente en el historial.
     */
    public boolean hasNext() {
        return currentIndex >= 0 && currentIndex < history.size() - 1;
    }

    /**
     * Obtiene la canción anterior y actualiza el índice.
     */
    public String getPrevious() {
        if (!hasPrevious()) return null;
        currentIndex--;
        return history.get(currentIndex);
    }

    /**
     * Obtiene la canción siguiente y actualiza el índice.
     */
    public String getNext() {
        if (!hasNext()) return null;
        currentIndex++;
        return history.get(currentIndex);
    }

    /**
     * Obtiene la canción actual sin cambiar índice.
     */
    public String getCurrent() {
        if (currentIndex < 0 || currentIndex >= history.size()) return null;
        return history.get(currentIndex);
    }

    /**
     * Devuelve el índice actual en el historial.
     */
    public int getIndex() {
        return currentIndex;
    }

    /**
     * Cambia el índice actual si es válido.
     */
    public void setIndex(int index) {
        if (index >= 0 && index < history.size()) {
            currentIndex = index;
        }
    }

    /**
     * Reemplaza todo el historial y posiciona el índice.
     */
    public void setHistory(List<String> newHistory, int index) {
        history.clear();
        if (newHistory != null) {
            history.addAll(newHistory);
        }
        if (index >= 0 && index < history.size()) {
            currentIndex = index;
        } else {
            currentIndex = history.isEmpty() ? -1 : 0;
        }
    }

    /**
     * Devuelve una copia del historial.
     */
    public List<String> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Limpia el historial y resetea el índice.
     */
    public void clear() {
        history.clear();
        currentIndex = -1;
    }

    /**
     * Indica si el historial está vacío.
     */
    public boolean isEmpty() {
        return history.isEmpty();
    }
}
