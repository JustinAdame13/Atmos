package com.justin.atmos.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.justin.atmos.model.Ubicacion;

/**
 * Cada widget que el usuario pone en su pantalla tiene un id (appWidgetId).
 * Aquí guardamos qué ubicación eligió para cada uno.
 * Si el widget usa "Mi ubicación" guardamos una Ubicacion con esGps = true.
 */
public class WidgetPrefs {

    private static final String PREFS = "atmos_widgets";
    private static final Gson gson = new Gson();

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void guardar(Context c, int widgetId, Ubicacion u) {
        prefs(c).edit().putString("w_" + widgetId, gson.toJson(u)).apply();
    }

    public static Ubicacion leer(Context c, int widgetId) {
        String json = prefs(c).getString("w_" + widgetId, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, Ubicacion.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static void borrar(Context c, int widgetId) {
        prefs(c).edit().remove("w_" + widgetId).apply();
    }
}
