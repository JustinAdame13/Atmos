package com.justin.atmos.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.justin.atmos.model.ClimaActual;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoritesManager {

    private static final String PREFS_NAME = "atmos_favorites";
    private static final String KEY_FAVORITES = "favorites_list";

    private final SharedPreferences prefs;
    private final Gson gson;

    public FavoritesManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<ClimaActual> obtenerFavoritos() {
        String json = prefs.getString(KEY_FAVORITES, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<ClimaActual>>() {}.getType();
        List<ClimaActual> lista = gson.fromJson(json, type);
        return lista != null ? lista : new ArrayList<>();
    }

    public void guardarFavorito(ClimaActual clima) {
        List<ClimaActual> lista = obtenerFavoritos();
        // Evitar duplicados por nombre de ciudad
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).cityName.equalsIgnoreCase(clima.cityName)) {
                lista.set(i, clima); // actualiza si ya existe
                guardarLista(lista);
                return;
            }
        }
        lista.add(clima);
        guardarLista(lista);
    }

    public void eliminarFavorito(String cityName) {
        List<ClimaActual> lista = obtenerFavoritos();
        lista.removeIf(c -> c.cityName.equalsIgnoreCase(cityName));
        guardarLista(lista);
    }

    public boolean esFavorito(String cityName) {
        for (ClimaActual c : obtenerFavoritos()) {
            if (c.cityName.equalsIgnoreCase(cityName)) return true;
        }
        return false;
    }

    private void guardarLista(List<ClimaActual> lista) {
        prefs.edit().putString(KEY_FAVORITES, gson.toJson(lista)).apply();
    }
}