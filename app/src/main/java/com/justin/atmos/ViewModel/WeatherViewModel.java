package com.justin.atmos.ViewModel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.justin.atmos.BuildConfig;
import com.justin.atmos.model.ClimaActual;
import com.justin.atmos.model.Pronostico;
import com.justin.atmos.Repository.WeatherRepository;

public class WeatherViewModel extends ViewModel {

    private final WeatherRepository repository;

    public final MutableLiveData<ClimaActual> climaActual = new MutableLiveData<>();
    public final MutableLiveData<Pronostico> pronostico = new MutableLiveData<>();
    public final MutableLiveData<String> error = new MutableLiveData<>();
    public final MutableLiveData<Boolean> cargando = new MutableLiveData<>();

    public WeatherViewModel() {
        repository = new WeatherRepository();
    }

    public void buscarClima(String ciudad) {
        cargando.setValue(true);
        repository.getClimaActual(ciudad, BuildConfig.OPENWEATHER_API_KEY,
                climaActual, error);
        repository.getPronostico(ciudad, BuildConfig.OPENWEATHER_API_KEY,
                pronostico, error);
    }

    public void buscarClimaConCoordenadas(double lat, double lon) {
        cargando.setValue(true);
        repository.getClimaActualPorCoordenadas(lat, lon, BuildConfig.OPENWEATHER_API_KEY,
                climaActual, error);
        repository.getPronosticoPorCoordenadas(lat, lon, BuildConfig.OPENWEATHER_API_KEY,
                pronostico, error);
    }
}