package com.justin.atmos.API;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Crea (una sola vez) los clientes Retrofit de las dos APIs.
 * Patrón Singleton: igual que en la v1, pero ahora con dos "Retrofit",
 * uno por cada servidor, compartiendo el mismo OkHttpClient.
 */
public class RetrofitClient {

    private static final String URL_OPENWEATHER = "https://api.openweathermap.org/";
    private static final String URL_OPENMETEO = "https://api.open-meteo.com/";

    private static RetrofitClient instance;

    private final WeatherService openWeather;
    private final OpenMeteoService openMeteo;

    private RetrofitClient() {
        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        openWeather = new Retrofit.Builder()
                .baseUrl(URL_OPENWEATHER)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherService.class);

        openMeteo = new Retrofit.Builder()
                .baseUrl(URL_OPENMETEO)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenMeteoService.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public WeatherService openWeather() {
        return openWeather;
    }

    public OpenMeteoService openMeteo() {
        return openMeteo;
    }
}
