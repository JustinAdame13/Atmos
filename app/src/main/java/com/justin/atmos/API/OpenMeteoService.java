package com.justin.atmos.API;

import com.justin.atmos.model.CalidadAireOpenMeteo;
import com.justin.atmos.model.PronosticoOpenMeteo;
import com.justin.atmos.model.ResultadoGeocoding;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Endpoints de Open-Meteo (gratis y sin API key).
 * Base: https://api.open-meteo.com/
 * Calidad del aire y geocoding viven en otros subdominios, por eso usan URL completa
 * (Retrofit permite poner una URL absoluta en @GET).
 */
public interface OpenMeteoService {

    String VARIABLES_ACTUALES = "temperature_2m,relative_humidity_2m,apparent_temperature,"
            + "is_day,precipitation,weather_code,cloud_cover,pressure_msl,"
            + "wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,dew_point_2m,visibility";

    /** Plan B por si el servidor rechaza alguna variable de la lista completa. */
    String VARIABLES_ACTUALES_BASICAS = "temperature_2m,relative_humidity_2m,apparent_temperature,"
            + "is_day,precipitation,weather_code,cloud_cover,pressure_msl,"
            + "wind_speed_10m,wind_direction_10m,wind_gusts_10m";

    String VARIABLES_HORARIAS = "temperature_2m,precipitation_probability,weather_code,is_day";

    String VARIABLES_DIARIAS = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,"
            + "uv_index_max,precipitation_probability_max,precipitation_sum,wind_speed_10m_max";

    /** Pronóstico completo: actual + por hora + por día (hasta 16 días). */
    @GET("v1/forecast")
    Call<PronosticoOpenMeteo> getPronostico(
            @Query("latitude") double lat,
            @Query("longitude") double lon,
            @Query("current") String actuales,
            @Query("hourly") String horarias,
            @Query("daily") String diarias,
            @Query("forecast_days") int dias,
            @Query("timezone") String zona,          // "auto"
            @Query("timeformat") String formatoTiempo // "unixtime"
    );

    /** Versión ligera para la lista de favoritos (solo lo actual y el día de hoy). */
    @GET("v1/forecast")
    Call<PronosticoOpenMeteo> getResumen(
            @Query("latitude") double lat,
            @Query("longitude") double lon,
            @Query("current") String actuales,
            @Query("daily") String diarias,
            @Query("forecast_days") int dias,
            @Query("timezone") String zona,
            @Query("timeformat") String formatoTiempo
    );

    @GET("https://air-quality-api.open-meteo.com/v1/air-quality")
    Call<CalidadAireOpenMeteo> getCalidadAire(
            @Query("latitude") double lat,
            @Query("longitude") double lon,
            @Query("current") String actuales  // "us_aqi,pm2_5,pm10,ozone,nitrogen_dioxide"
    );

    @GET("https://geocoding-api.open-meteo.com/v1/search")
    Call<ResultadoGeocoding> buscarCiudad(
            @Query("name") String nombre,
            @Query("count") int cantidad,
            @Query("language") String idioma,
            @Query("format") String formato // "json"
    );
}
