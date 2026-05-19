package com.justin.atmos.API;

import com.justin.atmos.model.ClimaActual;
import com.justin.atmos.model.Pronostico;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherService {

    @GET("weather")
    Call<ClimaActual> getClimaActual(
            @Query("q") String ciudad,
            @Query("appid") String apiKey,
            @Query("units") String unidades,
            @Query("lang") String idioma
    );

    @GET("forecast")
    Call<Pronostico> getPronostico(
            @Query("q") String ciudad,
            @Query("appid") String apiKey,
            @Query("units") String unidades,
            @Query("lang") String idioma
    );

    @GET("weather")
    Call<ClimaActual> getClimaActualPorCoordenadas(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String unidades,
            @Query("lang") String idioma
    );

    @GET("forecast")
    Call<Pronostico> getPronosticoPorCoordenadas(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String unidades,
            @Query("lang") String idioma
    );
}