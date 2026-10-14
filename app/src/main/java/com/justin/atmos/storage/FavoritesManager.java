package com.justin.atmos.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.justin.atmos.model.Ubicacion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda la lista de ciudades favoritas en SharedPreferences (como JSON con Gson).
 *
 * Cambio respecto a la v1: antes se guardaba el clima completo (ClimaActual),
 * que se quedaba viejo. Ahora se guarda solo la Ubicacion (nombre + coordenadas)
 * y el clima se descarga fresco cada vez.
 */
public class FavoritesManager {

    private static final String PREFS_NAME = "atmos_favorites";
    private static final String KEY_FAVORITES = "favorites_v2";

    private final SharedPreferences prefs;
    private final Gson gson;

    public FavoritesManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<Ubicacion> obtenerFavoritos() {
        String json = prefs.getString(KEY_FAVORITES, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Ubicacion>>() {}.getType();
        try {
            List<Ubicacion> lista = gson.fromJson(json, type);
            return lista != null ? lista : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void guardarFavorito(Ubicacion ubicacion) {
        List<Ubicacion> lista = obtenerFavoritos();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).llave().equals(ubicacion.llave())) {
                lista.set(i, ubicacion); // ya existía: se actualiza
                guardarLista(lista);
                return;
            }
        }
        lista.add(ubicacion);
        guardarLista(lista);
    }

    /** Inserta en una posición concreta (se usa para "Deshacer" al borrar). */
    public void insertarFavorito(int posicion, Ubicacion ubicacion) {
        List<Ubicacion> lista = obtenerFavoritos();
        posicion = Math.max(0, Math.min(posicion, lista.size()));
        lista.add(posicion, ubicacion);
        guardarLista(lista);
    }

    public void eliminarFavorito(Ubicacion ubicacion) {
        List<Ubicacion> lista = obtenerFavoritos();
        lista.removeIf(u -> u.llave().equals(ubicacion.llave()));
        guardarLista(lista);
    }

    public boolean esFavorito(Ubicacion ubicacion) {
        if (ubicacion == null) return false;
        for (Ubicacion u : obtenerFavoritos()) {
            if (u.llave().equals(ubicacion.llave())) return true;
        }
        return false;
    }

    private void guardarLista(List<Ubicacion> lista) {
        prefs.edit().putString(KEY_FAVORITES, gson.toJson(lista)).apply();
    }
}
