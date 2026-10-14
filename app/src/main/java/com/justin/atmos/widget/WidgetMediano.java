package com.justin.atmos.widget;

import android.content.Context;
import android.widget.RemoteViews;

import com.justin.atmos.R;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.utils.Formato;

/** Widget 4x2: clima actual + las próximas 5 horas. */
public class WidgetMediano extends AtmosWidgetBase {

    @Override
    protected int layout() {
        return R.layout.widget_mediano;
    }

    @Override
    protected void botones(Context ctx, RemoteViews rv) {
        rv.setOnClickPendingIntent(R.id.iv_w_refrescar, intentRefrescar(ctx));
    }

    @Override
    protected void llenar(Context ctx, RemoteViews rv, ClimaCombinado clima) {
        llenarHoras(ctx, rv, clima, 5);
    }

    /** Agrega N celdas de hora al contenedor (lo reutiliza el widget grande). */
    static void llenarHoras(Context ctx, RemoteViews rv, ClimaCombinado clima, int cantidad) {
        rv.removeAllViews(R.id.contenedor_w_horas);
        // Empezamos en la siguiente hora (la actual ya se muestra grande)
        for (int i = 1; i <= cantidad && i < clima.horas.size(); i++) {
            ClimaCombinado.Hora h = clima.horas.get(i);
            RemoteViews celda = new RemoteViews(ctx.getPackageName(), R.layout.widget_item_hora);
            celda.setTextViewText(R.id.tv_wh_hora, Formato.horaCorta(ctx, h.tiempo, clima.desfaseUtc));
            celda.setImageViewResource(R.id.iv_wh_icono, h.condicion.icono(h.esDeDia));
            celda.setTextViewText(R.id.tv_wh_temp, Formato.temp(ctx, h.temperatura));
            rv.addView(R.id.contenedor_w_horas, celda);
        }
    }
}
