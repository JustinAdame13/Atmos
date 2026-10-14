package com.justin.atmos.widget;

import android.content.Context;
import android.widget.RemoteViews;

import com.justin.atmos.R;
import com.justin.atmos.model.ClimaCombinado;

/** Widget 2x2: ciudad, ícono, temperatura, condición y máx/mín. */
public class WidgetChico extends AtmosWidgetBase {

    @Override
    protected int layout() {
        return R.layout.widget_chico;
    }

    @Override
    protected void llenar(Context ctx, RemoteViews rv, ClimaCombinado clima) {
        // Todo lo que muestra el widget chico ya lo llena la clase base.
    }
}
