package com.justin.atmos.Adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justin.atmos.R;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.model.Ubicacion;
import com.justin.atmos.utils.Formato;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tarjetas de ciudades favoritas. Cada tarjeta toma el color del clima
 * de esa ciudad (soleado = azul cielo, lluvia = gris azulado, etc.).
 */
public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    public interface OnFavoritoClick {
        void alTocar(Ubicacion ubicacion);
    }

    private final List<Ubicacion> lista = new ArrayList<>();
    private Map<String, ClimaCombinado> resumenes = new HashMap<>();
    private final OnFavoritoClick listener;

    public FavoritesAdapter(OnFavoritoClick listener) {
        this.listener = listener;
    }

    public void setLista(List<Ubicacion> nueva) {
        lista.clear();
        lista.addAll(nueva);
        notifyDataSetChanged();
    }

    public void setResumenes(Map<String, ClimaCombinado> mapa) {
        resumenes = mapa != null ? mapa : new HashMap<>();
        notifyDataSetChanged();
    }

    public Ubicacion getItem(int pos) {
        return lista.get(pos);
    }

    public void eliminar(int pos) {
        lista.remove(pos);
        notifyItemRemoved(pos);
    }

    public void insertar(int pos, Ubicacion u) {
        lista.add(pos, u);
        notifyItemInserted(pos);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Ubicacion u = lista.get(position);
        Context ctx = h.itemView.getContext();
        h.tvCiudad.setText(u.nombre);
        String bandera = u.bandera();
        h.tvRegion.setText(bandera.isEmpty() ? u.subtitulo() : bandera + "  " + u.subtitulo());

        ClimaCombinado c = resumenes.get(u.llave());
        if (c != null) {
            ClimaCombinado.Actual a = c.actual;
            h.tvTemp.setText(Formato.temp(ctx, a.temperatura));
            h.ivIcono.setImageResource(a.condicion.icono(a.esDeDia));
            if (a.maxHoy != null && a.minHoy != null) {
                h.tvMaxMin.setText("Máx " + Formato.temp(ctx, a.maxHoy) + "  Mín " + Formato.temp(ctx, a.minHoy));
            } else {
                h.tvMaxMin.setText("");
            }
            String hora = Formato.hora(ctx, System.currentTimeMillis() / 1000L, c.desfaseUtc);
            h.tvCondicion.setText(a.descripcion + "  ·  " + hora);

            GradientDrawable fondo = new GradientDrawable(GradientDrawable.Orientation.TL_BR, a.condicion.colores(a.esDeDia));
            fondo.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, ctx.getResources().getDisplayMetrics()));
            h.itemView.setBackground(fondo);
        } else {
            h.tvTemp.setText("--°");
            h.tvCondicion.setText("Cargando…");
            h.tvMaxMin.setText("");
            h.ivIcono.setImageResource(R.drawable.ic_weather_cloudy);
            h.itemView.setBackgroundResource(R.drawable.bg_glass);
        }

        h.itemView.setOnClickListener(v -> listener.alTocar(u));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCiudad, tvRegion, tvCondicion, tvTemp, tvMaxMin;
        final ImageView ivIcono;

        ViewHolder(@NonNull View v) {
            super(v);
            tvCiudad = v.findViewById(R.id.tv_fav_city);
            tvRegion = v.findViewById(R.id.tv_fav_region);
            tvCondicion = v.findViewById(R.id.tv_fav_condicion);
            tvTemp = v.findViewById(R.id.tv_fav_temp);
            tvMaxMin = v.findViewById(R.id.tv_fav_maxmin);
            ivIcono = v.findViewById(R.id.iv_fav_icono);
        }
    }
}
