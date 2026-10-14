package com.justin.atmos.notifications;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import com.justin.atmos.storage.Preferencias;
import com.justin.atmos.widget.WidgetChico;
import com.justin.atmos.widget.WidgetGrande;
import com.justin.atmos.widget.WidgetMediano;

import java.util.Calendar;

/**
 * Programa el trabajo en segundo plano usando JobScheduler (parte de Android,
 * no necesita librerías). Android decide el momento exacto para ahorrar batería,
 * pero respeta los intervalos mínimos que pedimos.
 *
 *  - Widgets:  cada 30 min (solo si hay algún widget en pantalla).
 *  - Lluvia:   cada 2 h (si el usuario activó las alertas).
 *  - Resumen:  una vez al día a la hora elegida (se reprograma después de cada aviso).
 */
public class ProgramadorTareas {

    public static final int JOB_WIDGETS = 100;
    public static final int JOB_LLUVIA = 101;
    public static final int JOB_RESUMEN = 102;
    public static final int JOB_WIDGETS_AHORA = 103;

    private static JobScheduler scheduler(Context c) {
        return (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    private static ComponentName servicio(Context c) {
        return new ComponentName(c, AtmosJobService.class);
    }

    /** Revisa los ajustes y deja programado (o cancelado) cada trabajo. Se puede llamar muchas veces. */
    public static void programarTodo(Context c) {
        JobScheduler js = scheduler(c);
        if (js == null) return;

        // ── widgets ──────────────────────────────────────────────────────────
        if (hayWidgets(c)) {
            if (js.getPendingJob(JOB_WIDGETS) == null) {
                js.schedule(new JobInfo.Builder(JOB_WIDGETS, servicio(c))
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPeriodic(30 * 60 * 1000L)
                        .setPersisted(true)
                        .build());
            }
        } else {
            js.cancel(JOB_WIDGETS);
        }

        // ── alertas de lluvia ────────────────────────────────────────────────
        if (Preferencias.notifLluvia(c)) {
            if (js.getPendingJob(JOB_LLUVIA) == null) {
                js.schedule(new JobInfo.Builder(JOB_LLUVIA, servicio(c))
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPeriodic(2 * 60 * 60 * 1000L)
                        .setPersisted(true)
                        .build());
            }
        } else {
            js.cancel(JOB_LLUVIA);
        }

        // ── resumen diario ───────────────────────────────────────────────────
        if (Preferencias.notifDiaria(c)) {
            programarResumen(c, false);
        } else {
            js.cancel(JOB_RESUMEN);
        }
    }

    /**
     * Programa el próximo resumen diario.
     * @param forzar true para reemplazar el que ya estaba (al cambiar la hora o tras mostrar uno).
     */
    public static void programarResumen(Context c, boolean forzar) {
        JobScheduler js = scheduler(c);
        if (js == null) return;
        if (!forzar && js.getPendingJob(JOB_RESUMEN) != null) return;

        Calendar proximo = Calendar.getInstance();
        proximo.set(Calendar.HOUR_OF_DAY, Preferencias.horaNotif(c));
        proximo.set(Calendar.MINUTE, Preferencias.minutoNotif(c));
        proximo.set(Calendar.SECOND, 0);
        if (proximo.getTimeInMillis() <= System.currentTimeMillis() + 60_000) {
            proximo.add(Calendar.DAY_OF_YEAR, 1);
        }
        long espera = proximo.getTimeInMillis() - System.currentTimeMillis();

        js.schedule(new JobInfo.Builder(JOB_RESUMEN, servicio(c))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(espera)
                .setOverrideDeadline(espera + 20 * 60 * 1000L) // margen de 20 min
                .setPersisted(true)
                .build());
    }

    /** Pide actualizar los widgets lo antes posible (cuando haya internet). */
    public static void actualizarWidgetsAhora(Context c) {
        JobScheduler js = scheduler(c);
        if (js == null) return;
        js.schedule(new JobInfo.Builder(JOB_WIDGETS_AHORA, servicio(c))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(5_000)
                .build());
        programarTodo(c);
    }

    public static boolean hayWidgets(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        return m.getAppWidgetIds(new ComponentName(c, WidgetChico.class)).length
                + m.getAppWidgetIds(new ComponentName(c, WidgetMediano.class)).length
                + m.getAppWidgetIds(new ComponentName(c, WidgetGrande.class)).length > 0;
    }
}
