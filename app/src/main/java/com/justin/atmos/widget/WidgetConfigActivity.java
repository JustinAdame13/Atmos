package com.justin.atmos.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.justin.atmos.R;
import com.justin.atmos.model.CondicionClima;
import com.justin.atmos.model.Ubicacion;
import com.justin.atmos.notifications.ProgramadorTareas;
import com.justin.atmos.storage.FavoritesManager;
import com.justin.atmos.storage.Preferencias;
import com.justin.atmos.storage.WidgetPrefs;
import com.justin.atmos.ui.views.FondoDinamico;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla que aparece al colocar un widget (y al "reconfigurarlo" en Android 12+).
 * El usuario elige: "Mi ubicación", la ciudad que ve en la app o un favorito.
 *
 * Regla de Android: si el usuario sale sin elegir, devolvemos RESULT_CANCELED
 * y el launcher quita el widget.
 */
public class WidgetConfigActivity extends AppCompatActivity {

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED); // por defecto: cancelado
        setContentView(R.layout.activity_widget_config);

        View root = findViewById(R.id.config_root);
        new FondoDinamico(root).cambiarA(CondicionClima.DESPEJADO.colores(false));
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int p = (int) (20 * getResources().getDisplayMetrics().density);
            v.setPadding(p, barras.top + p, p, barras.bottom + p);
            return insets;
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // Opción "Mi ubicación"
        Ubicacion gps = Preferencias.ultimoGps(this);
        TextView tvGps = findViewById(R.id.tv_gps_detalle);
        if (gps != null) tvGps.setText("Ahora: " + gps.nombre + " · se actualiza al abrir la app");
        findViewById(R.id.opcion_gps).setOnClickListener(v -> {
            Ubicacion u = gps != null ? gps : new Ubicacion("Mi ubicación", null, null, null, 0, 0);
            u.esGps = true;
            elegir(u);
        });

        // Ciudad actual de la app + favoritos (sin repetir)
        List<Ubicacion> opciones = new ArrayList<>();
        Ubicacion actual = Preferencias.ultimaUbicacion(this);
        if (actual != null && !actual.esGps) opciones.add(actual);
        for (Ubicacion f : new FavoritesManager(this).obtenerFavoritos()) {
            boolean repetida = false;
            for (Ubicacion o : opciones) if (o.llave().equals(f.llave())) repetida = true;
            if (!repetida) opciones.add(f);
        }

        LinearLayout contenedor = findViewById(R.id.contenedor_opciones);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Ubicacion u : opciones) {
            View fila = inflater.inflate(R.layout.item_search, contenedor, false);
            String bandera = u.bandera();
            ((TextView) fila.findViewById(R.id.tv_bandera)).setText(bandera.isEmpty() ? "🌎" : bandera);
            ((TextView) fila.findViewById(R.id.tv_search_city)).setText(u.nombre);
            ((TextView) fila.findViewById(R.id.tv_search_region)).setText(u.subtitulo());
            fila.findViewById(R.id.btn_search_favorito).setVisibility(View.GONE);
            fila.setOnClickListener(v -> elegir(u));
            contenedor.addView(fila);
        }
        findViewById(R.id.tv_sin_favoritos).setVisibility(opciones.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_titulo_favoritos).setVisibility(opciones.isEmpty() ? View.GONE : View.VISIBLE);

        findViewById(R.id.btn_cancelar_widget).setOnClickListener(v -> finish());
    }

    private void elegir(Ubicacion u) {
        WidgetPrefs.guardar(this, widgetId, u);

        // Dibujar ya con lo que haya en caché y pedir datos frescos
        WidgetUpdater.dibujar(this, widgetId);
        ProgramadorTareas.actualizarWidgetsAhora(this);

        Intent resultado = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, resultado);
        finish();
    }
}
