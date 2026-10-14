package com.justin.atmos.widget;

import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

import com.justin.atmos.R;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.utils.Formato;

/** Widget 4x4: clima actual, próximas horas y próximos 5 días. */
public class WidgetGrande extends AtmosWidgetBase {

    @Override
    protected int layout() {
        return R.layout.widget_grande;
    }

    @Override
    protected void botones(Context ctx, RemoteViews rv) {
        rv.setOnClickPendingIntent(R.id.iv_w_refrescar, intentRefrescar(ctx));
    }

    @Override
    protected void llenar(Context ctx, RemoteViews rv, ClimaCombinado clima) {
        rv.setTextViewText(R.id.tv_w_sensacion, "Sensación " + Formato.temp(ctx, clima.actual.sensacion)
                + "  ·  Humedad " + clima.actual.humedad + "%");
        rv.setTextViewText(R.id.tv_w_actualizado, Formato.hora(ctx, clima.actualizado / 1000L, clima.desfaseUtc));

        WidgetMediano.llenarHoras(ctx, rv, clima, 5);

        rv.removeAllViews(R.id.contenedor_w_dias);
        for (int i = 1; i <= 5 && i < clima.dias.size(); i++) { // desde mañana
            ClimaCombinado.Dia d = clima.dias.get(i);
            RemoteViews fila = new RemoteViews(ctx.getPackageName(), R.layout.widget_item_dia);
            fila.setTextViewText(R.id.tv_wd_dia, Formato.nombreDia(d.fecha, clima.desfaseUtc));
            fila.setImageViewResource(R.id.iv_wd_icono, d.condicion.icono(true));
            int prob = d.probLluvia();
            fila.setTextViewText(R.id.tv_wd_lluvia, prob >= 10 ? prob + "%" : "");
            fila.setViewVisibility(R.id.tv_wd_lluvia, prob >= 10 ? View.VISIBLE : View.INVISIBLE);
            fila.setTextViewText(R.id.tv_wd_min, Formato.temp(ctx, d.min()));
            fila.setTextViewText(R.id.tv_wd_max, Formato.temp(ctx, d.max()));
            rv.addView(R.id.contenedor_w_dias, fila);
        }
    }
}
