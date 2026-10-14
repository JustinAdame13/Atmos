package com.justin.atmos.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.justin.atmos.R;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.notifications.ProgramadorTareas;
import com.justin.atmos.storage.WidgetPrefs;
import com.justin.atmos.ui.MainActivity;
import com.justin.atmos.utils.Formato;

/**
 * Clase base de los tres widgets (2x2, 4x2 y 4x4).
 *
 * Un widget NO es una vista normal: vive dentro del launcher (otra app), por eso
 * se dibuja con RemoteViews, que solo admite ciertos componentes (TextView,
 * ImageView, LinearLayout...) y se actualiza con AppWidgetManager.
 *
 * Patrón "Template Method": la base hace todo lo común (clics, encabezado,
 * fondo) y cada subclase solo llena su parte con llenar().
 */
public abstract class AtmosWidgetBase extends AppWidgetProvider {

    public static final String ACCION_REFRESCAR = "com.justin.atmos.widget.REFRESCAR";

    /** Layout XML del widget. */
    protected abstract int layout();

    /** Llena las vistas propias del tamaño de widget. */
    protected abstract void llenar(Context ctx, RemoteViews rv, ClimaCombinado clima);

    /** Conecta botones extra (refrescar). Se llama siempre, haya datos o no. */
    protected void botones(Context ctx, RemoteViews rv) { }

    // ── ciclo de vida del widget ─────────────────────────────────────────────

    @Override
    public void onUpdate(Context ctx, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            manager.updateAppWidget(id, construir(ctx, id, WidgetUpdater.climaEnCache(ctx, id)));
        }
        ProgramadorTareas.actualizarWidgetsAhora(ctx);
    }

    @Override
    public void onDeleted(Context ctx, int[] ids) {
        for (int id : ids) WidgetPrefs.borrar(ctx, id);
    }

    @Override
    public void onEnabled(Context ctx) {
        ProgramadorTareas.programarTodo(ctx);
    }

    @Override
    public void onDisabled(Context ctx) {
        ProgramadorTareas.programarTodo(ctx); // cancela el trabajo si ya no hay widgets
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACCION_REFRESCAR.equals(intent.getAction())) {
            ProgramadorTareas.actualizarWidgetsAhora(ctx);
        }
    }

    // ── construcción de la vista ─────────────────────────────────────────────

    public RemoteViews construir(Context ctx, int widgetId, ClimaCombinado clima) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), layout());

        // Tocar el widget abre la app
        Intent abrir = new Intent(ctx, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        rv.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(ctx, widgetId, abrir,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        botones(ctx, rv);

        if (clima == null) {
            rv.setTextViewText(R.id.tv_w_ciudad, "Atmos");
            rv.setTextViewText(R.id.tv_w_temp, "--°");
            rv.setTextViewText(R.id.tv_w_desc, "Toca para abrir");
            rv.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_clear_night);
            return rv;
        }

        ClimaCombinado.Actual a = clima.actual;
        rv.setInt(R.id.widget_root, "setBackgroundResource", a.condicion.fondoWidget(a.esDeDia));
        rv.setTextViewText(R.id.tv_w_ciudad, clima.ubicacion.nombre);
        rv.setTextViewText(R.id.tv_w_temp, Formato.temp(ctx, a.temperatura));
        rv.setTextViewText(R.id.tv_w_desc, a.descripcion);
        rv.setImageViewResource(R.id.iv_w_icono, a.condicion.icono(a.esDeDia));
        if (a.maxHoy != null && a.minHoy != null) {
            rv.setTextViewText(R.id.tv_w_maxmin, "↑" + Formato.temp(ctx, a.maxHoy) + "  ↓" + Formato.temp(ctx, a.minHoy));
        }

        llenar(ctx, rv, clima);
        return rv;
    }

    /** PendingIntent para el botón de refrescar (lo usan el mediano y el grande). */
    protected PendingIntent intentRefrescar(Context ctx) {
        Intent i = new Intent(ctx, getClass()).setAction(ACCION_REFRESCAR);
        return PendingIntent.getBroadcast(ctx, getClass().hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    protected ComponentName componente(Context ctx) {
        return new ComponentName(ctx, getClass());
    }
}
