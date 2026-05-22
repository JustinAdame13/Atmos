package com.justin.atmos.Repository;

import androidx.lifecycle.MutableLiveData;

import com.justin.atmos.API.RetrofitClient;
import com.justin.atmos.API.WeatherService;
import com.justin.atmos.model.ClimaActual;
import com.justin.atmos.model.Pronostico;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherRepository {

    private final WeatherService service;

    public WeatherRepository() {
        service = RetrofitClient.getInstance().createService(WeatherService.class);
    }
    public void getClimaActual(String ciudad, String apiKey,
                               MutableLiveData<ClimaActual> climaData,
                               MutableLiveData<String> errorData) {

        service.getClimaActual(ciudad, apiKey, "metric", "es")
                .enqueue(new Callback<ClimaActual>() {

                    @Override
                    public void onResponse(Call<ClimaActual> call, Response<ClimaActual> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            climaData.postValue(response.body());
                        } else {
                            errorData.postValue("Ciudad no encontrada");
                        }
                    }

                    @Override
                    public void onFailure(Call<ClimaActual> call, Throwable t) {
                        errorData.postValue("Sin conexión a internet");
                    }
                });
    }

    public void getClimaActualPorCoordenadas(double lat, double lon, String apiKey,
                                             MutableLiveData<ClimaActual> climaData,
                                             MutableLiveData<String> errorData,
                                             MutableLiveData<Boolean> cargando) {
        service.getClimaActualPorCoordenadas(lat, lon, apiKey, "metric", "es")
                .enqueue(new Callback<ClimaActual>() {
                    @Override
                    public void onResponse(Call<ClimaActual> call, Response<ClimaActual> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            climaData.postValue(response.body());
                        } else {
                            errorData.postValue("No se pudo obtener el clima");
                        }
                        cargando.postValue(false);
                    }

                    @Override
                    public void onFailure(Call<ClimaActual> call, Throwable t) {
                        errorData.postValue("Sin conexión a internet");
                        cargando.postValue(false);
                    }
                });
    }

    public void getPronostico(String ciudad, String apiKey,
                              MutableLiveData<Pronostico> pronosticoData,
                              MutableLiveData<String> errorData) {

        service.getPronostico(ciudad, apiKey, "metric", "es")
                .enqueue(new Callback<Pronostico>() {

                    @Override
                    public void onResponse(Call<Pronostico> call, Response<Pronostico> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            pronosticoData.postValue(response.body());
                        } else {
                            errorData.postValue("Ciudad no encontrada");
                        }
                    }

                    @Override
                    public void onFailure(Call<Pronostico> call, Throwable t) {
                        errorData.postValue("Sin conexión a internet");
                    }
                });
    }

    public void getClimaActualPorCoordenadas(double lat, double lon, String apiKey,
                                             MutableLiveData<ClimaActual> climaData,
                                             MutableLiveData<String> errorData) {
        service.getClimaActualPorCoordenadas(lat, lon, apiKey, "metric", "es")
                .enqueue(new Callback<ClimaActual>() {
                    @Override
                    public void onResponse(Call<ClimaActual> call, Response<ClimaActual> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            climaData.postValue(response.body());
                        } else {
                            errorData.postValue("No se pudo obtener el clima");
                        }
                    }

                    @Override
                    public void onFailure(Call<ClimaActual> call, Throwable t) {
                        errorData.postValue("Sin conexión a internet");
                    }
                });
    }

    public void getPronosticoPorCoordenadas(double lat, double lon, String apiKey,
                                            MutableLiveData<Pronostico> pronosticoData,
                                            MutableLiveData<String> errorData) {
        service.getPronosticoPorCoordenadas(lat, lon, apiKey, "metric", "es")
                .enqueue(new Callback<Pronostico>() {
                    @Override
                    public void onResponse(Call<Pronostico> call, Response<Pronostico> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            pronosticoData.postValue(response.body());
                        } else {
                            errorData.postValue("No se pudo obtener el pronóstico");
                        }
                    }

                    @Override
                    public void onFailure(Call<Pronostico> call, Throwable t) {
                        errorData.postValue("Sin conexión a internet");
                    }
                });
    }
}