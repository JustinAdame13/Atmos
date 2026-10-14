package com.justin.atmos.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import com.justin.atmos.Repository.WeatherRepository;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.model.Ubicacion;
import com.justin.atmos.storage.CacheClima;
import com.justin.atmos.storage.Preferencias;
import com.justin.atmos.storage.WidgetPrefs;

/**
 * Punto central para redibujar todos los widgets de Atmos.
 *
 *  - actualizarTodos():          rápido, usa solo la caché (sin internet).
 *  - descargarYDibujarTodos():   descarga datos nuevos. SÍNCRONO: llamarlo en segundo plano.
 */
public class WidgetUpdater {

    private static final AtmosWidgetBase[] TIPOS = {
            new WidgetChico(), new WidgetMediano(), new WidgetGrande()
    };

    /** ¿Qué ubicación muestra este widget? */
    public static Ubicacion ubicacionDe(Context ctx, int widgetId) {
        Ubicacion u = WidgetPrefs.leer(ctx, widgetId);
        if (u != null && !u.esGps) return u; // una ciudad fija

        // "Mi ubicación" (o widget sin configurar): última posición que detectó la app
        Ubicacion gps = Preferencias.ultimoGps(ctx);
        if (gps != null) return gps;
        return Preferencias.ultimaUbicacion(ctx);
    }

    public static ClimaCombinado climaEnCache(Context ctx, int widgetId) {
        return CacheClima.leer(ctx, ubicacionDe(ctx, widgetId));
    }

    /** Redibuja con lo que haya en caché. */
    public static void actualizarTodos(Context ctx) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        for (AtmosWidgetBase tipo : TIPOS) {
            for (int id : m.getAppWidgetIds(new ComponentName(ctx, tipo.getClass()))) {
                m.updateAppWidget(id, tipo.construir(ctx, id, climaEnCache(ctx, id)));
            }
        }
    }

    /** Descarga el clima de cada widget y lo redibuja. */
    public static void descargarYDibujarTodos(Context ctx) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        WeatherRepository repo = new WeatherRepository(ctx);
        for (AtmosWidgetBase tipo : TIPOS) {
            for (int id : m.getAppWidgetIds(new ComponentName(ctx, tipo.getClass()))) {
                Ubicacion u = ubicacionDe(ctx, id);
                ClimaCombinado clima = null;
                if (u != null) {
                    try {
                        clima = repo.obtenerClima(u, false); // usa caché si tiene < 15 min
                    } catch (Exception e) {
                        clima = CacheClima.leer(ctx, u);
                    }
                }
                m.updateAppWidget(id, tipo.construir(ctx, id, clima));
            }
        }
    }

    /** Dibuja un widget concreto (lo usa la pantalla de configuración). */
    public static void dibujar(Context ctx, int widgetId) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        android.appwidget.AppWidgetProviderInfo info = m.getAppWidgetInfo(widgetId);
        if (info == null) return;
        for (AtmosWidgetBase tipo : TIPOS) {
            if (info.provider.getClassName().equals(tipo.getClass().getName())) {
                m.updateAppWidget(widgetId, tipo.construir(ctx, widgetId, climaEnCache(ctx, widgetId)));
            }
        }
    }
}
