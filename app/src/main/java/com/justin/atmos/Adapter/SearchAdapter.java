package com.justin.atmos.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justin.atmos.R;
import com.justin.atmos.model.Ubicacion;
import com.justin.atmos.storage.FavoritesManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultados del buscador (Open-Meteo Geocoding).
 * Tocar una fila abre la ciudad en Inicio; la estrella la agrega/quita de favoritos.
 */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    public interface Listener {
        void alSeleccionar(Ubicacion ubicacion);
        void alCambiarFavorito(Ubicacion ubicacion, boolean ahoraEsFavorito);
    }

    private final List<Ubicacion> lista = new ArrayList<>();
    private final FavoritesManager favoritos;
    private final Listener listener;

    public SearchAdapter(FavoritesManager favoritos, Listener listener) {
        this.favoritos = favoritos;
        this.listener = listener;
    }

    public void setLista(List<Ubicacion> nueva) {
        lista.clear();
        if (nueva != null) lista.addAll(nueva);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Ubicacion u = lista.get(position);
        String bandera = u.bandera();
        h.tvBandera.setText(bandera.isEmpty() ? "🌎" : bandera);
        h.tvCiudad.setText(u.nombre);
        h.tvRegion.setText(u.subtitulo());

        boolean esFav = favoritos.esFavorito(u);
        h.btnFavorito.setImageResource(esFav ? R.drawable.ic_star : R.drawable.ic_star_outline);

        h.itemView.setOnClickListener(v -> listener.alSeleccionar(u));
        h.btnFavorito.setOnClickListener(v -> {
            boolean ahora;
            if (favoritos.esFavorito(u)) {
                favoritos.eliminarFavorito(u);
                ahora = false;
            } else {
                favoritos.guardarFavorito(u);
                ahora = true;
            }
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos);
            listener.alCambiarFavorito(u, ahora);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvBandera, tvCiudad, tvRegion;
        final ImageButton btnFavorito;

        ViewHolder(@NonNull View v) {
            super(v);
            tvBandera = v.findViewById(R.id.tv_bandera);
            tvCiudad = v.findViewById(R.id.tv_search_city);
            tvRegion = v.findViewById(R.id.tv_search_region);
            btnFavorito = v.findViewById(R.id.btn_search_favorito);
        }
    }
}
