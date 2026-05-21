package com.justin.atmos.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justin.atmos.R;
import com.justin.atmos.model.ClimaActual;

import java.util.Locale;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    // ── interfaces de callback ──────────────────────────────────────────────
    public interface OnFavoriteClickListener {
        void onFavoriteClick(ClimaActual clima);
    }

    // ── datos ───────────────────────────────────────────────────────────────
    private ClimaActual climaActual = null;
    private OnFavoriteClickListener favoriteListener;

    // ── setters ─────────────────────────────────────────────────────────────
    public void setClima(ClimaActual clima) {
        this.climaActual = clima;
        notifyDataSetChanged();
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteListener = listener;
    }

    // ── RecyclerView obligatorios ────────────────────────────────────────────
    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        if (climaActual == null) return;

        // Ciudad y país
        holder.tvCityName.setText(climaActual.cityName);

        String country = "";
        if (climaActual.sys != null && climaActual.sys.country != null) {
            country = climaActual.sys.country;
        }
        holder.tvCountry.setText(country);

        // Descripción
        if (climaActual.weather != null && !climaActual.weather.isEmpty()) {
            String desc = climaActual.weather.get(0).description;
            String descCap = desc.substring(0, 1).toUpperCase() + desc.substring(1);
            holder.tvWeatherDesc.setText(descCap);
        }

        // Temperatura
        holder.tvTemperature.setText(
                String.format(Locale.getDefault(), "%.0f°C", climaActual.main.temp));

        // Ícono del clima (drawable estático)
        if (climaActual.weather != null && !climaActual.weather.isEmpty()) {
            int weatherId = climaActual.weather.get(0).id;
            int iconRes = elegirIcono(weatherId,
                    climaActual.dt,
                    climaActual.sys != null ? climaActual.sys.sunrise : 0,
                    climaActual.sys != null ? climaActual.sys.sunset : 0);
            holder.ivWeatherIcon.setImageResource(iconRes);
        }

        // Botón favorito
        holder.btnFavorite.setOnClickListener(v -> {
            if (favoriteListener != null) {
                favoriteListener.onFavoriteClick(climaActual);
            }
        });
    }

    @Override
    public int getItemCount() {
        return climaActual != null ? 1 : 0;
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────
    static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView tvCityName, tvCountry, tvWeatherDesc, tvTemperature;
        ImageView ivWeatherIcon;
        ImageButton btnFavorite;

        SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCityName    = itemView.findViewById(R.id.tv_city_name);
            tvCountry     = itemView.findViewById(R.id.tv_country);
            tvWeatherDesc = itemView.findViewById(R.id.tv_weather_desc);
            tvTemperature = itemView.findViewById(R.id.tv_temperature);
            ivWeatherIcon = itemView.findViewById(R.id.iv_weather_icon);
            btnFavorite   = itemView.findViewById(R.id.btn_favorite);
        }
    }

    // ── lógica de ícono (drawable, no Lottie) ────────────────────────────────
    private int elegirIcono(int weatherId, long ahora, long sunrise, long sunset) {
        boolean esDeNoche = ahora < sunrise || ahora > sunset;

        if (weatherId >= 200 && weatherId <= 232) return R.drawable.ic_weather_storm;
        if (weatherId >= 781 && weatherId <= 799) return R.drawable.ic_weather_storm;
        if ((weatherId >= 300 && weatherId <= 321) ||
                (weatherId >= 500 && weatherId <= 531)) {
            return R.drawable.ic_weather_rainy;
        }
        if (weatherId >= 600 && weatherId <= 780) {
            return esDeNoche ? R.drawable.ic_weather_cloudy_night : R.drawable.ic_weather_cloudy;
        }
        return esDeNoche ? R.drawable.ic_weather_night : R.drawable.ic_weather_sunny;
    }
}

