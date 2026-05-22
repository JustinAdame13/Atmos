package com.justin.atmos.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.justin.atmos.R;
import com.justin.atmos.model.ClimaActual;

import java.util.Locale;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    public interface OnFavoriteClickListener {
        void onFavoriteClick(ClimaActual clima);
    }

    private ClimaActual climaActual = null;
    private OnFavoriteClickListener favoriteListener;

    public void setClima(ClimaActual clima) {
        this.climaActual = clima;
        notifyDataSetChanged();
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteListener = listener;
    }

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

        holder.tvCityName.setText(climaActual.cityName);

        String country = "";
        if (climaActual.sys != null && climaActual.sys.country != null) {
            country = climaActual.sys.country;
        }
        holder.tvCountry.setText(country);

        if (climaActual.weather != null && !climaActual.weather.isEmpty()) {
            String desc = climaActual.weather.get(0).description;
            holder.tvWeatherDesc.setText(
                    desc.substring(0, 1).toUpperCase() + desc.substring(1));

            // Elegir animación Lottie correcta
            int weatherId = climaActual.weather.get(0).id;
            int animRes = elegirAnimacion(
                    weatherId,
                    climaActual.dt,
                    climaActual.sys != null ? climaActual.sys.sunrise : 0,
                    climaActual.sys != null ? climaActual.sys.sunset : 0
            );
            holder.lottieIcon.setAnimation(animRes);
            holder.lottieIcon.playAnimation();
        }

        holder.tvTemperature.setText(
                String.format(Locale.getDefault(), "%.0f°C", climaActual.main.temp));

        holder.btnFavorite.setOnClickListener(v -> {
            if (favoriteListener != null) {
                favoriteListener.onFavoriteClick(climaActual);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(climaActual);
            }
        });

    }

    @Override
    public int getItemCount() {
        return climaActual != null ? 1 : 0;
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView tvCityName, tvCountry, tvWeatherDesc, tvTemperature;
        LottieAnimationView lottieIcon;
        ImageButton btnFavorite;

        SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCityName    = itemView.findViewById(R.id.tv_city_name);
            tvCountry     = itemView.findViewById(R.id.tv_country);
            tvWeatherDesc = itemView.findViewById(R.id.tv_weather_desc);
            tvTemperature = itemView.findViewById(R.id.tv_temperature);
            lottieIcon    = itemView.findViewById(R.id.iv_weather_icon);
            btnFavorite   = itemView.findViewById(R.id.btn_favorite);
        }
    }

    // Misma lógica que HomeFragment — devuelve raw Lottie
    private int elegirAnimacion(int weatherId, long ahora, long sunrise, long sunset) {
        boolean esDeNoche = ahora < sunrise || ahora > sunset;

        // Tormenta eléctrica: 200–232
        if (weatherId >= 200 && weatherId <= 232) return R.raw.anim_storm;

        // Llovizna: 300–321
        if (weatherId >= 300 && weatherId <= 321) {
            return esDeNoche ? R.raw.anim_rainy_night : R.raw.anim_rainy_day;
        }

        // Lluvia: 500–531
        if (weatherId >= 500 && weatherId <= 531) {
            return esDeNoche ? R.raw.anim_rainy_night : R.raw.anim_rainy_day;
        }

        // Nieve: 600–622
        if (weatherId >= 600 && weatherId <= 622) {
            return esDeNoche ? R.raw.anim_cloudy_night : R.raw.anim_cloudy_day;
        }

        // Atmósfera (niebla, neblina, humo, etc.): 701–781
        if (weatherId >= 701 && weatherId <= 781) {
            return esDeNoche ? R.raw.anim_cloudy_night : R.raw.anim_cloudy_day;
        }

        // Tornado: 781–799
        if (weatherId >= 781 && weatherId <= 799) return R.raw.anim_thunder;

        // Cielo despejado: 800
        if (weatherId == 800) {
            return esDeNoche ? R.raw.anim_night : R.raw.anim_sunny;
        }

        // Algo nublado (pocas nubes): 801
        if (weatherId == 801) {
            return esDeNoche ? R.raw.anim_cloudy_night : R.raw.anim_cloudy_day;
        }

        // Nublado: 802–804
        if (weatherId >= 802 && weatherId <= 804) {
            return esDeNoche ? R.raw.anim_cloudy_night : R.raw.anim_cloudy_day;
        }

        // Fallback
        return esDeNoche ? R.raw.anim_night : R.raw.anim_sunny;
    }
    public interface OnItemClickListener {
        void onItemClick(ClimaActual clima);
    }

    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
}