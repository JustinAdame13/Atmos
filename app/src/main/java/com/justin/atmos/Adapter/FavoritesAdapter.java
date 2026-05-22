package com.justin.atmos.Adapter;

import android.content.Context;
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
import com.justin.atmos.storage.FavoritesManager;

import java.util.List;
import java.util.Locale;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    public interface OnFavoriteListener {
        void onDelete(ClimaActual clima);
        void onCardClick(ClimaActual clima);
    }

    private final List<ClimaActual> lista;
    private final OnFavoriteListener listener;

    public FavoritesAdapter(List<ClimaActual> lista, OnFavoriteListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        ClimaActual clima = lista.get(position);

        holder.tvCityName.setText(clima.cityName);

        // País desde sys.country, si está disponible
        if (clima.sys != null && clima.sys.country != null) {
            holder.tvCountry.setText(clima.sys.country);
        } else {
            holder.tvCountry.setText("--");
        }

        // Descripción
        if (clima.weather != null && !clima.weather.isEmpty()) {
            String desc = clima.weather.get(0).description;
            holder.tvWeatherDesc.setText(
                    desc.substring(0, 1).toUpperCase() + desc.substring(1));
            holder.ivWeatherIcon.setImageResource(
                    elegirIcono(clima.weather.get(0).id));
        }

        // Temperatura
        if (clima.main != null) {
            holder.tvTemperature.setText(
                    String.format(Locale.getDefault(), "%.0f°C", clima.main.temp));
            holder.tvHumidity.setText(
                    String.format(Locale.getDefault(), "%d%%", clima.main.humidity));
            holder.tvFeelsLike.setText(
                    String.format(Locale.getDefault(), "%.0f°C", clima.main.feelsLike));
        }

        // Viento
        if (clima.wind != null) {
            holder.tvWind.setText(
                    String.format(Locale.getDefault(), "%.1f km/h", clima.wind.speed * 3.6));
        }

        // Botón eliminar
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                listener.onDelete(clima);
            }
        });

        // Tap en tarjeta
        holder.itemView.setOnClickListener(v -> listener.onCardClick(clima));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivWeatherIcon;
        TextView tvCityName, tvCountry, tvWeatherDesc,
                tvTemperature, tvHumidity, tvWind, tvFeelsLike;
        ImageButton btnDelete;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivWeatherIcon   = itemView.findViewById(R.id.iv_weather_icon);
            tvCityName      = itemView.findViewById(R.id.tv_city_name);
            tvCountry       = itemView.findViewById(R.id.tv_country);
            tvWeatherDesc   = itemView.findViewById(R.id.tv_weather_desc);
            tvTemperature   = itemView.findViewById(R.id.tv_temperature);
            tvHumidity      = itemView.findViewById(R.id.tv_humidity);
            tvWind          = itemView.findViewById(R.id.tv_wind);
            tvFeelsLike     = itemView.findViewById(R.id.tv_feels_like);
            btnDelete       = itemView.findViewById(R.id.btn_delete);
        }
    }

    private int elegirIcono(int weatherId) {
        if (weatherId >= 200 && weatherId <= 232) return R.drawable.ic_weather_storm;
        if (weatherId >= 300 && weatherId <= 321) return R.drawable.ic_weather_rainy;
        if (weatherId >= 500 && weatherId <= 531) return R.drawable.ic_weather_rainy;
        if (weatherId >= 600 && weatherId <= 622) return R.drawable.ic_weather_snow;
        if (weatherId >= 700 && weatherId <= 781) return R.drawable.ic_weather_cloudy;
        if (weatherId == 800)                      return R.drawable.ic_weather_sunny;
        if (weatherId >= 801 && weatherId <= 804) return R.drawable.ic_weather_partly_cloudy;
        return R.drawable.ic_weather_sunny;
    }
}