package com.justin.atmos.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta de Open-Meteo → https://api.open-meteo.com/v1/forecast
 *
 * Open-Meteo devuelve los datos "en columnas": en lugar de una lista de objetos,
 * cada variable es una lista y el índice i de todas las listas corresponde
 * a la misma hora (o al mismo día). Por eso usamos List<Double>, List<Integer>, etc.
 *
 * Se piden las fechas como Unix (timeformat=unixtime) y la zona horaria
 * del lugar (timezone=auto) → utc_offset_seconds.
 */
public class PronosticoOpenMeteo {

    @SerializedName("latitude")
    public double latitude;

    @SerializedName("longitude")
    public double longitude;

    @SerializedName("utc_offset_seconds")
    public int utcOffsetSeconds;

    @SerializedName("timezone")
    public String timezone;

    @SerializedName("current")
    public Actual current;

    @SerializedName("hourly")
    public PorHora hourly;

    @SerializedName("daily")
    public PorDia daily;

    // ─── clases internas ───────────────────────────────

    public static class Actual {
        @SerializedName("time")
        public long time;

        @SerializedName("temperature_2m")
        public Double temperatura;

        @SerializedName("relative_humidity_2m")
        public Integer humedad;

        @SerializedName("apparent_temperature")
        public Double sensacion;

        @SerializedName("is_day")
        public Integer esDeDia; // 1 = día, 0 = noche

        @SerializedName("precipitation")
        public Double precipitacion; // mm

        @SerializedName("weather_code")
        public Integer codigoClima; // código WMO

        @SerializedName("cloud_cover")
        public Integer nubosidad; // %

        @SerializedName("pressure_msl")
        public Double presion; // hPa

        @SerializedName("wind_speed_10m")
        public Double viento; // km/h

        @SerializedName("wind_direction_10m")
        public Integer direccionViento; // grados

        @SerializedName("wind_gusts_10m")
        public Double rafagas; // km/h

        @SerializedName("uv_index")
        public Double uv;

        @SerializedName("dew_point_2m")
        public Double puntoRocio;

        @SerializedName("visibility")
        public Double visibilidad; // metros
    }

    public static class PorHora {
        @SerializedName("time")
        public List<Long> time;

        @SerializedName("temperature_2m")
        public List<Double> temperatura;

        @SerializedName("precipitation_probability")
        public List<Integer> probLluvia;

        @SerializedName("weather_code")
        public List<Integer> codigoClima;

        @SerializedName("is_day")
        public List<Integer> esDeDia;
    }

    public static class PorDia {
        @SerializedName("time")
        public List<Long> time;

        @SerializedName("weather_code")
        public List<Integer> codigoClima;

        @SerializedName("temperature_2m_max")
        public List<Double> maxima;

        @SerializedName("temperature_2m_min")
        public List<Double> minima;

        @SerializedName("sunrise")
        public List<Long> amanecer;

        @SerializedName("sunset")
        public List<Long> atardecer;

        @SerializedName("uv_index_max")
        public List<Double> uvMax;

        @SerializedName("precipitation_probability_max")
        public List<Integer> probLluviaMax;

        @SerializedName("precipitation_sum")
        public List<Double> lluviaTotal; // mm

        @SerializedName("wind_speed_10m_max")
        public List<Double> vientoMax; // km/h
    }
}
