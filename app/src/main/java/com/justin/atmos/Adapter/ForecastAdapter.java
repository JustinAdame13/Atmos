package com.justin.atmos.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justin.atmos.R;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.model.CondicionClima;
import com.justin.atmos.ui.views.BarraRangoView;
import com.justin.atmos.utils.Formato;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lista del pronóstico de hasta 16 días. Cada fila se puede tocar para
 * desplegar el detalle con lo que dijo cada fuente.
 *
 * El método estático llenarFila() también lo usa HomeFragment para su
 * lista de 7 días, así no repetimos código.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private final List<ClimaCombinado.Dia> dias = new ArrayList<>();
    private final Set<Integer> expandidos = new HashSet<>();
    private int desfase;
    private double minGlobal, maxGlobal;
    private Double tempActual;

    public void setDatos(ClimaCombinado clima) {
        dias.clear();
        dias.addAll(clima.dias);
        desfase = clima.desfaseUtc;
        tempActual = clima.actual.temperatura;
        double[] r = rangoGlobal(dias);
        minGlobal = r[0];
        maxGlobal = r[1];
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forecast, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ClimaCombinado.Dia d = dias.get(position);
        Context ctx = h.itemView.getContext();
        llenarFila(h.itemView, d, desfase, minGlobal, maxGlobal, position == 0 ? tempActual : null);

        boolean abierto = expandidos.contains(position);
        h.ivExpandir.setVisibility(View.VISIBLE);
        h.ivExpandir.setRotation(abierto ? 180 : 0);
        h.layoutDetalle.setVisibility(abierto ? View.VISIBLE : View.GONE);
        if (abierto) llenarDetalle(ctx, h, d);

        h.itemView.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (!expandidos.remove(pos)) expandidos.add(pos);
            notifyItemChanged(pos);
        });
    }

    private void llenarDetalle(Context ctx, ViewHolder h, ClimaCombinado.Dia d) {
        h.tvDescripcion.setText(CondicionClima.descripcionWmo(d.codigoWmo));

        h.tvFuenteOm.setText(String.format(Locale.getDefault(), "● Open-Meteo:  %s / %s  ·  lluvia %s",
                Formato.temp(ctx, d.maxOpenMeteo), Formato.temp(ctx, d.minOpenMeteo),
                d.probLluviaOpenMeteo != null ? d.probLluviaOpenMeteo + "%" : "—"));
        h.tvFuenteOm.setTextColor(ctx.getColor(R.color.fuente_openmeteo));

        if (d.tieneDosFuentes()) {
            h.tvFuenteOw.setVisibility(View.VISIBLE);
            h.tvFuenteOw.setText(String.format(Locale.getDefault(), "● OpenWeather:  %s / %s  ·  lluvia %s",
                    Formato.temp(ctx, d.maxOpenWeather), Formato.temp(ctx, d.minOpenWeather),
                    d.probLluviaOpenWeather != null ? d.probLluviaOpenWeather + "%" : "—"));
            h.tvFuenteOw.setTextColor(ctx.getColor(R.color.fuente_openweather));
        } else {
            h.tvFuenteOw.setVisibility(View.VISIBLE);
            h.tvFuenteOw.setText("● OpenWeather: sin datos para este día (solo pronostica 5 días)");
            h.tvFuenteOw.setTextColor(ctx.getColor(R.color.text_on_glass_tertiary));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Rango probable: máx ").append(Formato.rango(ctx, d.maxBaja(), d.maxAlta()))
                .append(" · mín ").append(Formato.rango(ctx, d.minBaja(), d.minAlta()));
        if (d.uvMax != null) sb.append("\nUV máx ").append(Math.round(d.uvMax)).append(" (").append(Formato.nivelUv(d.uvMax)).append(")");
        if (d.vientoMaxKmh != null) sb.append("  ·  Viento máx ").append(Formato.viento(ctx, d.vientoMaxKmh));
        if (d.lluviaMm != null && d.lluviaMm > 0) sb.append("\nLluvia acumulada ").append(String.format(Locale.US, "%.1f mm", d.lluviaMm));
        if (d.amanecer > 0) {
            sb.append("\nAmanecer ").append(Formato.hora(ctx, d.amanecer, desfase))
                    .append("  ·  Atardecer ").append(Formato.hora(ctx, d.atardecer, desfase));
        }
        h.tvExtra.setText(sb.toString());
    }

    @Override
    public int getItemCount() {
        return dias.size();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Ayudantes compartidos con HomeFragment
    // ═════════════════════════════════════════════════════════════════════════

    /** Llena una vista inflada de item_forecast.xml con los datos de un día. */
    public static void llenarFila(View fila, ClimaCombinado.Dia d, int desfase,
                                  double minGlobal, double maxGlobal, Double tempActual) {
        Context ctx = fila.getContext();
        ((TextView) fila.findViewById(R.id.tv_dia)).setText(Formato.nombreDia(d.fecha, desfase));
        ((TextView) fila.findViewById(R.id.tv_fecha)).setText(Formato.fechaCorta(d.fecha, desfase));
        ((ImageView) fila.findViewById(R.id.iv_icono)).setImageResource(d.condicion.icono(true));

        TextView tvProb = fila.findViewById(R.id.tv_prob_lluvia);
        int prob = d.probLluvia();
        tvProb.setText(prob >= 10 ? prob + "%" : "");

        ((TextView) fila.findViewById(R.id.tv_min)).setText(Formato.temp(ctx, d.min()));
        ((TextView) fila.findViewById(R.id.tv_max)).setText(Formato.temp(ctx, d.max()));

        BarraRangoView barra = fila.findViewById(R.id.barra_rango);
        barra.setRango(minGlobal, maxGlobal, d.min(), d.max(), d.minBaja(), d.maxAlta(), tempActual);
    }

    /** {mínimo, máximo} de todos los días (incluyendo la diferencia entre fuentes). */
    public static double[] rangoGlobal(List<ClimaCombinado.Dia> dias) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (ClimaCombinado.Dia d : dias) {
            min = Math.min(min, d.minBaja());
            max = Math.max(max, d.maxAlta());
        }
        if (dias.isEmpty()) { min = 0; max = 1; }
        return new double[]{min, max};
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivExpandir;
        final View layoutDetalle;
        final TextView tvDescripcion, tvFuenteOm, tvFuenteOw, tvExtra;

        ViewHolder(@NonNull View v) {
            super(v);
            ivExpandir = v.findViewById(R.id.iv_expandir);
            layoutDetalle = v.findViewById(R.id.layout_detalle);
            tvDescripcion = v.findViewById(R.id.tv_descripcion_dia);
            tvFuenteOm = v.findViewById(R.id.tv_fuente_om);
            tvFuenteOw = v.findViewById(R.id.tv_fuente_ow);
            tvExtra = v.findViewById(R.id.tv_extra_dia);
        }
    }
}
