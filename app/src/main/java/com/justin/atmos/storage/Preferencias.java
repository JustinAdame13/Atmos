package com.justin.atmos.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.justin.atmos.model.Ubicacion;

/**
 * Ajustes del usuario guardados en SharedPreferences.
 * Se usa desde la app, los widgets y las notificaciones, por eso todo
 * es estático y recibe un Context.
 */
public class Preferencias {

    private static final String PREFS = "atmos_ajustes";

    private static final String K_UNIDAD_TEMP = "unidad_temp";        // "C" o "F"
    private static final String K_UNIDAD_VIENTO = "unidad_viento";    // "kmh", "mph", "ms"
    private static final String K_FORMATO_24H = "formato_24h";
    private static final String K_USAR_OWM = "usar_openweather";
    private static final String K_NOTIF_DIARIA = "notif_diaria";
    private static final String K_NOTIF_HORA = "notif_hora";
    private static final String K_NOTIF_MINUTO = "notif_minuto";
    private static final String K_NOTIF_LLUVIA = "notif_lluvia";
    private static final String K_ULTIMA_UBICACION = "ultima_ubicacion";
    private static final String K_ULTIMO_GPS = "ultimo_gps";
    private static final String K_ULTIMA_ALERTA = "ultima_alerta_lluvia";

    private static final Gson gson = new Gson();

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ── unidades ─────────────────────────────────────────────────────────────

    public static boolean usaFahrenheit(Context c) {
        return "F".equals(prefs(c).getString(K_UNIDAD_TEMP, "C"));
    }

    public static void setFahrenheit(Context c, boolean f) {
        prefs(c).edit().putString(K_UNIDAD_TEMP, f ? "F" : "C").apply();
    }

    public static String unidadViento(Context c) {
        return prefs(c).getString(K_UNIDAD_VIENTO, "kmh");
    }

    public static void setUnidadViento(Context c, String unidad) {
        prefs(c).edit().putString(K_UNIDAD_VIENTO, unidad).apply();
    }

    public static boolean formato24h(Context c) {
        return prefs(c).getBoolean(K_FORMATO_24H, true);
    }

    public static void setFormato24h(Context c, boolean valor) {
        prefs(c).edit().putBoolean(K_FORMATO_24H, valor).apply();
    }

    // ── fuentes ──────────────────────────────────────────────────────────────

    public static boolean usarOpenWeather(Context c) {
        return prefs(c).getBoolean(K_USAR_OWM, true);
    }

    public static void setUsarOpenWeather(Context c, boolean valor) {
        prefs(c).edit().putBoolean(K_USAR_OWM, valor).apply();
    }

    // ── notificaciones ───────────────────────────────────────────────────────

    public static boolean notifDiaria(Context c) {
        return prefs(c).getBoolean(K_NOTIF_DIARIA, false);
    }

    public static void setNotifDiaria(Context c, boolean valor) {
        prefs(c).edit().putBoolean(K_NOTIF_DIARIA, valor).apply();
    }

    public static int horaNotif(Context c) {
        return prefs(c).getInt(K_NOTIF_HORA, 7);
    }

    public static void setHoraNotif(Context c, int hora) {
        prefs(c).edit().putInt(K_NOTIF_HORA, hora).apply();
    }

    public static int minutoNotif(Context c) {
        return prefs(c).getInt(K_NOTIF_MINUTO, 0);
    }

    public static void setMinutoNotif(Context c, int minuto) {
        prefs(c).edit().putInt(K_NOTIF_MINUTO, minuto).apply();
    }

    public static boolean notifLluvia(Context c) {
        return prefs(c).getBoolean(K_NOTIF_LLUVIA, false);
    }

    public static void setNotifLluvia(Context c, boolean valor) {
        prefs(c).edit().putBoolean(K_NOTIF_LLUVIA, valor).apply();
    }

    public static long ultimaAlertaLluvia(Context c) {
        return prefs(c).getLong(K_ULTIMA_ALERTA, 0);
    }

    public static void setUltimaAlertaLluvia(Context c, long millis) {
        prefs(c).edit().putLong(K_ULTIMA_ALERTA, millis).apply();
    }

    // ── ubicaciones ──────────────────────────────────────────────────────────

    /** Ciudad que se está viendo en Inicio (GPS o elegida por el usuario). */
    public static Ubicacion ultimaUbicacion(Context c) {
        return leerUbicacion(c, K_ULTIMA_UBICACION);
    }

    public static void setUltimaUbicacion(Context c, Ubicacion u) {
        prefs(c).edit().putString(K_ULTIMA_UBICACION, gson.toJson(u)).apply();
    }

    /** Última posición conocida del GPS (la usan los widgets configurados con "Mi ubicación"). */
    public static Ubicacion ultimoGps(Context c) {
        return leerUbicacion(c, K_ULTIMO_GPS);
    }

    public static void setUltimoGps(Context c, Ubicacion u) {
        prefs(c).edit().putString(K_ULTIMO_GPS, gson.toJson(u)).apply();
    }

    private static Ubicacion leerUbicacion(Context c, String llave) {
        String json = prefs(c).getString(llave, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, Ubicacion.class);
        } catch (Exception e) {
            return null;
        }
    }
}
