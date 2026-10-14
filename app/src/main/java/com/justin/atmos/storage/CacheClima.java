package com.justin.atmos.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.model.Ubicacion;

/**
 * Caché del último clima descargado de cada ubicación.
 *
 * Sirve para:
 *  - abrir la app y mostrar datos al instante mientras llegan los nuevos;
 *  - que los widgets muestren algo aunque no haya internet;
 *  - no gastar peticiones si los datos tienen menos de 15 minutos.
 */
public class CacheClima {

    private static final String PREFS = "atmos_cache";
    public static final long VIGENCIA_MS = 15 * 60 * 1000L; // 15 minutos

    private static final Gson gson = new Gson();

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void guardar(Context c, ClimaCombinado clima) {
        if (clima == null || clima.ubicacion == null) return;
        prefs(c).edit().putString(clima.ubicacion.llave(), gson.toJson(clima)).apply();
    }

    public static ClimaCombinado leer(Context c, Ubicacion u) {
        if (u == null) return null;
        String json = prefs(c).getString(u.llave(), null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, ClimaCombinado.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean esReciente(ClimaCombinado clima) {
        return clima != null && System.currentTimeMillis() - clima.actualizado < VIGENCIA_MS;
    }
}
