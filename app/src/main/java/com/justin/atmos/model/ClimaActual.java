package com.justin.atmos.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta de OpenWeatherMap → /data/2.5/weather
 * (clima actual de un punto). En la v2 se leen muchos más campos que en la v1
 * para aprovechar al máximo la API: presión, visibilidad, nubes, ráfagas, lluvia, etc.
 */
public class ClimaActual {

    @SerializedName("name")
    public String cityName;

    @SerializedName("coord")
    public Coord coord;

    @SerializedName("weather")
    public List<Weather> weather;

    @SerializedName("main")
    public Main main;

    @SerializedName("visibility")
    public Integer visibility; // metros (máx. 10 000)

    @SerializedName("wind")
    public Wind wind;

    @SerializedName("clouds")
    public Clouds clouds;

    @SerializedName("rain")
    public Precipitacion rain;

    @SerializedName("sys")
    public Sys sys;

    @SerializedName("timezone")
    public int timezone; // desfase con UTC en segundos

    @SerializedName("dt")
    public long dt; // fecha de la medición (Unix, segundos)

    // ─── clases internas ───────────────────────────────

    public static class Coord {
        @SerializedName("lat")
        public double lat;

        @SerializedName("lon")
        public double lon;
    }

    public static class Weather {
        @SerializedName("id")
        public int id;

        @SerializedName("main")
        public String main;

        @SerializedName("description")
        public String description;

        @SerializedName("icon")
        public String icon;
    }

    public static class Main {
        @SerializedName("temp")
        public double temp;

        @SerializedName("feels_like")
        public double feelsLike;

        @SerializedName("temp_min")
        public double tempMin;

        @SerializedName("temp_max")
        public double tempMax;

        @SerializedName("pressure")
        public int pressure;

        @SerializedName("humidity")
        public int humidity;
    }

    public static class Wind {
        @SerializedName("speed")
        public double speed; // m/s

        @SerializedName("deg")
        public int deg;

        @SerializedName("gust")
        public Double gust; // m/s (puede no venir)
    }

    public static class Clouds {
        @SerializedName("all")
        public int all; // % de nubosidad
    }

    public static class Precipitacion {
        @SerializedName("1h")
        public Double unaHora; // mm en la última hora

        @SerializedName("3h")
        public Double tresHoras;
    }

    public static class Sys {
        @SerializedName("sunrise")
        public long sunrise;

        @SerializedName("sunset")
        public long sunset;

        @SerializedName("country")
        public String country;
    }
}
