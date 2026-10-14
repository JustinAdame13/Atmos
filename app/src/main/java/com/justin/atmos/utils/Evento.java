package com.justin.atmos.utils;

/**
 * Envoltorio para mensajes que solo deben mostrarse UNA vez (por ejemplo un Toast de error).
 * Sin esto, al girar la pantalla o cambiar de pestaña el LiveData volvería a
 * entregar el último error y el Toast aparecería de nuevo.
 */
public class Evento<T> {

    private final T contenido;
    private boolean manejado = false;

    public Evento(T contenido) {
        this.contenido = contenido;
    }

    /** Devuelve el contenido la primera vez y null las siguientes. */
    public T obtenerSiNoManejado() {
        if (manejado) return null;
        manejado = true;
        return contenido;
    }
}
