package com.justin.atmos.ViewModel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.justin.atmos.BuildConfig;
import com.justin.atmos.model.ClimaActual;
import com.justin.atmos.model.Pronostico;
import com.justin.atmos.Repository.WeatherRepository;

public class WeatherViewModel extends ViewModel {

    private final WeatherRepository repository;

    // ── datos de la ciudad actual (Home) ─────────────────────────────────
    public final MutableLiveData<ClimaActual> climaActual = new MutableLiveData<>();
    public final MutableLiveData<Pronostico> pronostico = new MutableLiveData<>();

    // ── datos de búsqueda (Search) — no pisan climaActual ────────────────
    public final MutableLiveData<ClimaActual> climaBusqueda = new MutableLiveData<>();

    public final MutableLiveData<String> error = new MutableLiveData<>();
    public final MutableLiveData<Boolean> cargando = new MutableLiveData<>();

    public WeatherViewModel() {
        repository = new WeatherRepository();
    }

    // ── buscar por nombre para Search (no toca climaActual) ──────────────
    public void buscarClima(String ciudad) {
        cargando.setValue(true);
        repository.getClimaActual(ciudad, BuildConfig.OPENWEATHER_API_KEY,
                climaBusqueda, error);
    }

    // ── cargar ciudad desde Search hacia Home (sí toca climaActual) ──────
    public void cargarCiudadEnHome(ClimaActual clima) {
        climaActual.setValue(clima);
        // También cargamos el pronóstico de esa ciudad
        repository.getPronostico(clima.cityName, BuildConfig.OPENWEATHER_API_KEY,
                pronostico, error);
    }

    // ── buscar por coordenadas GPS (Home) ─────────────────────────────────
    public void buscarClimaConCoordenadas(double lat, double lon) {
        cargando.setValue(true);
        repository.getClimaActualPorCoordenadas(lat, lon, BuildConfig.OPENWEATHER_API_KEY,
                climaActual, error, cargando);
        repository.getPronosticoPorCoordenadas(lat, lon, BuildConfig.OPENWEATHER_API_KEY,
                pronostico, error);
    }
}