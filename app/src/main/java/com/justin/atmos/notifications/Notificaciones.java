package com.justin.atmos.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.justin.atmos.R;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.ui.MainActivity;
import com.justin.atmos.utils.Formato;

import java.util.Locale;

/**
 * Crea los canales y muestra las notificaciones de Atmos.
 * Desde Android 8 cada notificación pertenece a un "canal" que el usuario
 * puede silenciar por separado en los ajustes del sistema.
 */
public class Notificaciones {

    public static final String CANAL_DIARIO = "resumen_diario";
    public static final String CANAL_LLUVIA = "alertas_lluvia";

    private static final int ID_RESUMEN = 1;
    private static final int ID_LLUVIA = 2;

    public static void crearCanales(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel diario = new NotificationChannel(CANAL_DIARIO,
                c.getString(R.string.canal_diario), NotificationManager.IMPORTANCE_DEFAULT);
        diario.setDescription(c.getString(R.string.canal_diario_desc));

        NotificationChannel lluvia = new NotificationChannel(CANAL_LLUVIA,
                c.getString(R.string.canal_lluvia), NotificationManager.IMPORTANCE_HIGH);
        lluvia.setDescription(c.getString(R.string.canal_lluvia_desc));

        nm.createNotificationChannel(diario);
        nm.createNotificationChannel(lluvia);
    }

    public static boolean tienePermiso(Context c) {
        if (Build.VERSION.SDK_INT < 33) return true;
        return ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void mostrarResumen(Context c, ClimaCombinado clima) {
        ClimaCombinado.Actual a = clima.actual;
        String titulo = clima.ubicacion.nombre + ": " + Formato.temp(c, a.temperatura) + " · " + a.descripcion;

        StringBuilder texto = new StringBuilder();
        if (!clima.dias.isEmpty()) {
            ClimaCombinado.Dia hoy = clima.dias.get(0);
            texto.append("Hoy: máx ").append(Formato.rango(c, hoy.maxBaja(), hoy.maxAlta()))
                    .append(", mín ").append(Formato.rango(c, hoy.minBaja(), hoy.minAlta()))
                    .append(". Lluvia ").append(hoy.probLluvia()).append("%.");
            if (hoy.uvMax != null && hoy.uvMax >= 6) {
                texto.append(" UV ").append(Formato.nivelUv(hoy.uvMax).toLowerCase(Locale.getDefault()))
                        .append(": usa bloqueador.");
            }
        }
        mostrar(c, CANAL_DIARIO, ID_RESUMEN, a.condicion.icono(a.esDeDia), titulo, texto.toString());
    }

    public static void mostrarAlertaLluvia(Context c, ClimaCombinado clima, ClimaCombinado.Hora hora) {
        String titulo = "☔ Lluvia probable en " + clima.ubicacion.nombre;
        String texto = String.format(Locale.getDefault(),
                "%d%% de probabilidad a las %s. ¡No olvides el paraguas!",
                hora.probLluvia, Formato.hora(c, hora.tiempo, clima.desfaseUtc));
        mostrar(c, CANAL_LLUVIA, ID_LLUVIA, R.drawable.ic_weather_rainy, titulo, texto);
    }

    private static void mostrar(Context c, String canal, int id, int iconoGrande, String titulo, String texto) {
        if (!tienePermiso(c)) return;
        crearCanales(c);

        Intent abrir = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, id, abrir,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, canal)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setColor(0xFF4FC3F7)
                .setContentIntent(pi)
                .setAutoCancel(true);

        // Ícono grande: el vector del clima convertido a Bitmap
        android.graphics.drawable.Drawable d = ContextCompat.getDrawable(c, iconoGrande);
        if (d != null) {
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(128, 128, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
            d.setBounds(0, 0, 128, 128);
            d.draw(canvas);
            b.setLargeIcon(bmp);
        }

        try {
            NotificationManagerCompat.from(c).notify(id, b.build());
        } catch (SecurityException ignorado) {
            // El usuario quitó el permiso de notificaciones
        }
    }
}
