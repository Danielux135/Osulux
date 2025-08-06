package com.osuplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryManager {

    private final List<String> history = new ArrayList<>();
    private int index = -1;

    /**
     * Añade una canción al historial.
     * Si hay canciones adelante en el historial (índice < última posición),
     * se eliminan para mantener linealidad.
     * Luego la nueva canción se añade y se actualiza índice.
     */
    public void addSong(String songName) {
        // Si índice no está al final, eliminar "adelante"
        if (index < history.size() - 1) {
            history.subList(index + 1, history.size()).clear();
        }
        history.add(songName);
        index = history.size() - 1;
    }

    /**
     * Obtiene la canción anterior en el historial y mueve el índice hacia atrás.
     * Si no hay anterior, devuelve null y no cambia índice.
     */
    public String getPrevious() {
        if (hasPrevious()) {
            index--;
            return history.get(index);
        }
        return null;
    }

    /**
     * Obtiene la canción siguiente en el historial y mueve el índice hacia adelante.
     * Si no hay siguiente, devuelve null y no cambia índice.
     */
    public String getNext() {
        if (hasNext()) {
            index++;
            return history.get(index);
        }
        return null;
    }

    /**
     * Consulta si hay canción anterior en el historial.
     */
    public boolean hasPrevious() {
        return index > 0;
    }

    /**
     * Consulta si hay canción siguiente en el historial.
     */
    public boolean hasNext() {
        return index < history.size() - 1;
    }

    /**
     * Obtiene la canción actual en el historial.
     * Si el índice es inválido, devuelve null.
     */
    public String getCurrent() {
        if (index >= 0 && index < history.size()) {
            return history.get(index);
        }
        return null;
    }

    /**
     * Limpia todo el historial y resetea índice.
     */
    public void clear() {
        history.clear();
        index = -1;
    }

    /**
     * Devuelve copia inmutable de la lista historial.
     */
    public List<String> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    /**
     * Devuelve el índice actual en el historial.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Establece el índice actual en el historial.
     * Si el índice es inválido, se ignora.
     */
    public void setIndex(int idx) {
        if (idx >= 0 && idx < history.size()) {
            index = idx;
        }
    }

    /**
     * Establece lista completa e índice actual del historial.
     * Se copia la lista para evitar modificaciones externas.
     * Si el índice es inválido, se pone a -1.
     */
    public void setHistory(List<String> newHistory, int newIndex) {
        history.clear();
        if (newHistory != null) {
            history.addAll(newHistory);
        }
        if (newIndex >= 0 && newIndex < history.size()) {
            index = newIndex;
        } else {
            index = -1;
        }
    }
}
