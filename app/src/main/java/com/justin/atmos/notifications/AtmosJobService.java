package com.justin.atmos.notifications;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;

import com.justin.atmos.Repository.WeatherRepository;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.model.Ubicacion;
import com.justin.atmos.storage.Preferencias;
import com.justin.atmos.widget.WidgetUpdater;

/**
 * Servicio que Android ejecuta en segundo plano cuando toca un trabajo programado.
 * onStartJob corre en el hilo principal, así que el trabajo real va en otro hilo
 * y al terminar avisamos con jobFinished().
 */
public class AtmosJobService extends JobService {

    private static final long SEIS_HORAS = 6 * 60 * 60 * 1000L;

    @Override
    public boolean onStartJob(JobParameters params) {
        final Context ctx = getApplicationContext();
        new Thread(() -> {
            try {
                switch (params.getJobId()) {
                    case ProgramadorTareas.JOB_WIDGETS:
                    case ProgramadorTareas.JOB_WIDGETS_AHORA:
                        WidgetUpdater.descargarYDibujarTodos(ctx);
                        break;
                    case ProgramadorTareas.JOB_LLUVIA:
                        revisarLluvia(ctx);
                        break;
                    case ProgramadorTareas.JOB_RESUMEN:
                        enviarResumen(ctx);
                        ProgramadorTareas.programarResumen(ctx, true); // el de mañana
                        break;
                }
            } catch (Exception ignorado) {
                // Sin internet u otro error: se intentará en la próxima ejecución
            }
            jobFinished(params, false);
        }).start();
        return true; // true = el trabajo sigue en otro hilo
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true; // si Android lo interrumpe, que lo reintente
    }

    private static Ubicacion ubicacionPrincipal(Context ctx) {
        Ubicacion u = Preferencias.ultimaUbicacion(ctx);
        return u != null ? u : Preferencias.ultimoGps(ctx);
    }

    private static void enviarResumen(Context ctx) throws Exception {
        if (!Preferencias.notifDiaria(ctx)) return;
        Ubicacion u = ubicacionPrincipal(ctx);
        if (u == null) return;
        ClimaCombinado clima = new WeatherRepository(ctx).obtenerClima(u, true);
        Notificaciones.mostrarResumen(ctx, clima);
        WidgetUpdater.actualizarTodos(ctx);
    }

    private static void revisarLluvia(Context ctx) throws Exception {
        if (!Preferencias.notifLluvia(ctx)) return;
        if (System.currentTimeMillis() - Preferencias.ultimaAlertaLluvia(ctx) < SEIS_HORAS) return;
        Ubicacion u = ubicacionPrincipal(ctx);
        if (u == null) return;

        ClimaCombinado clima = new WeatherRepository(ctx).obtenerClima(u, false);
        if (clima.actual.condicion.esLluvia()) return; // ya está lloviendo: no avisamos
        ClimaCombinado.Hora hora = clima.proximaLluvia(3, 60);
        if (hora != null) {
            Notificaciones.mostrarAlertaLluvia(ctx, clima, hora);
            Preferencias.setUltimaAlertaLluvia(ctx, System.currentTimeMillis());
        }
        WidgetUpdater.actualizarTodos(ctx);
    }
}
