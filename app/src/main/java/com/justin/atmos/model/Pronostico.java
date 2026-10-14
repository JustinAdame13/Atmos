package com.justin.atmos.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta de OpenWeatherMap → /data/2.5/forecast
 * Pronóstico de 5 días en bloques de 3 horas (40 elementos).
 */
public class Pronostico {

    @SerializedName("city")
    public City city;

    @SerializedName("list")
    public List<ForecastItem> list;

    // ─── clases internas ───────────────────────────────

    public static class City {
        @SerializedName("name")
        public String name;

        @SerializedName("country")
        public String country;

        @SerializedName("timezone")
        public int timezone;

        @SerializedName("sunrise")
        public long sunrise;

        @SerializedName("sunset")
        public long sunset;
    }

    public static class ForecastItem {
        @SerializedName("dt")
        public long dt;

        @SerializedName("main")
        public ClimaActual.Main main;

        @SerializedName("weather")
        public List<ClimaActual.Weather> weather;

        @SerializedName("wind")
        public ClimaActual.Wind wind;

        @SerializedName("pop")
        public double pop; // probabilidad de precipitación 0..1

        @SerializedName("dt_txt")
        public String dtTxt; // ej: "2026-05-15 12:00:00" (UTC)
    }
}
