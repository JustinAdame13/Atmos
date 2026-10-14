package com.justin.atmos.API;

import com.justin.atmos.model.CalidadAireOWM;
import com.justin.atmos.model.ClimaActual;
import com.justin.atmos.model.Pronostico;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Endpoints de OpenWeatherMap (plan gratuito).
 * Base: https://api.openweathermap.org/
 */
public interface WeatherService {

    /** Clima actual por coordenadas. */
    @GET("data/2.5/weather")
    Call<ClimaActual> getClimaActualPorCoordenadas(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String unidades,
            @Query("lang") String idioma
    );

    /** Pronóstico 5 días / cada 3 horas por coordenadas. */
    @GET("data/2.5/forecast")
    Call<Pronostico> getPronosticoPorCoordenadas(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String unidades,
            @Query("lang") String idioma
    );

    /** Calidad del aire actual (índice 1-5 y contaminantes). */
    @GET("data/2.5/air_pollution")
    Call<CalidadAireOWM> getCalidadAire(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey
    );
}
