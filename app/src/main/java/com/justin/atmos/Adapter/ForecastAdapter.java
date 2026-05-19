package com.justin.atmos.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.justin.atmos.R;
import com.justin.atmos.model.Pronostico;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private List<DiaResumen> dias = new ArrayList<>();
    private long sunrise;
    private long sunset;

    // ── modelo interno ───────────────────────────────────
    public static class DiaResumen {
        public String dia;
        public double tempMax;
        public double tempMin;
        public int weatherId;
        public long dt;
    }

    // ── actualizar datos ─────────────────────────────────
    public void setDatos(Pronostico pronostico) {
        this.sunrise = pronostico.city.sunrise;
        this.sunset = pronostico.city.sunset;
        this.dias = agruparPorDia(pronostico.list);
        notifyDataSetChanged();
    }

    // ── agrupar los 40 registros por día ─────────────────
    private List<DiaResumen> agruparPorDia(List<Pronostico.ForecastItem> items) {
        LinkedHashMap<String, List<Pronostico.ForecastItem>> mapa = new LinkedHashMap<>();

        SimpleDateFormat sdfKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (Pronostico.ForecastItem item : items) {
            String key = sdfKey.format(new Date(item.dt * 1000L));
            if (!mapa.containsKey(key)) {
                mapa.put(key, new ArrayList<>());
            }
            mapa.get(key).add(item);
        }

        List<DiaResumen> resultado = new ArrayList<>();

        for (Map.Entry<String, List<Pronostico.ForecastItem>> entry : mapa.entrySet()) {
            List<Pronostico.ForecastItem> grupo = entry.getValue();

            DiaResumen resumen = new DiaResumen();

            // nombre del día
            SimpleDateFormat sdfDia = new SimpleDateFormat("EEEE", new Locale("es"));
            resumen.dia = sdfDia.format(new Date(grupo.get(0).dt * 1000L));
            resumen.dia = resumen.dia.substring(0, 1).toUpperCase() + resumen.dia.substring(1);

            // max y min
            resumen.tempMax = Double.MIN_VALUE;
            resumen.tempMin = Double.MAX_VALUE;
            for (Pronostico.ForecastItem item : grupo) {
                if (item.main.temp > resumen.tempMax) resumen.tempMax = item.main.temp;
                if (item.main.temp < resumen.tempMin) resumen.tempMin = item.main.temp;
            }

            // weather id y dt del mediodía si existe, si no el primero
            Pronostico.ForecastItem representativo = grupo.get(0);
            for (Pronostico.ForecastItem item : grupo) {
                if (item.dtTxt.contains("12:00:00")) {
                    representativo = item;
                    break;
                }
            }
            resumen.weatherId = representativo.weather.get(0).id;
            resumen.dt = representativo.dt;

            resultado.add(resumen);
        }

        return resultado;
    }

    // ── RecyclerView obligatorios ────────────────────────
    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forecast, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        DiaResumen dia = dias.get(position);

        holder.tvDay.setText(dia.dia);
        holder.tvTemps.setText(String.format(Locale.getDefault(),
                "%.0f° / %.0f°", dia.tempMax, dia.tempMin));

        int animRes = elegirAnimacion(dia.weatherId, dia.dt, sunrise, sunset);
        holder.lottie.setAnimation(animRes);
        holder.lottie.playAnimation();
    }

    @Override
    public int getItemCount() {
        return dias.size();
    }

    // ── ViewHolder ───────────────────────────────────────
    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvTemps;
        LottieAnimationView lottie;

        ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvTemps = itemView.findViewById(R.id.tv_temps);
            lottie = itemView.findViewById(R.id.lottie_item);
        }
    }

    // ── misma lógica de animación que HomeFragment ───────
    private int elegirAnimacion(int weatherId, long ahora, long sunrise, long sunset) {
        boolean esDeNoche = ahora < sunrise || ahora > sunset;

        if (weatherId >= 200 && weatherId <= 232) return R.raw.anim_storm;
        if (weatherId >= 781 && weatherId <= 799) return R.raw.anim_thunder;
        if ((weatherId >= 300 && weatherId <= 321) ||
                (weatherId >= 500 && weatherId <= 531)) {
            return esDeNoche ? R.raw.anim_rainy_night : R.raw.anim_rainy_day;
        }
        if (weatherId >= 600 && weatherId <= 780) {
            return esDeNoche ? R.raw.anim_cloudy_night : R.raw.anim_cloudy_day;
        }
        return esDeNoche ? R.raw.anim_night : R.raw.anim_sunny;
    }
}