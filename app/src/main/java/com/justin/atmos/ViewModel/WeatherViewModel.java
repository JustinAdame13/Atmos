package com.justin.atmos.ViewModel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.justin.atmos.Repository.WeatherRepository;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.model.Ubicacion;
import com.justin.atmos.storage.CacheClima;
import com.justin.atmos.storage.Preferencias;
import com.justin.atmos.utils.Evento;
import com.justin.atmos.widget.WidgetUpdater;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel compartido por todas las pantallas (se obtiene con requireActivity()).
 *
 * En la v2 es AndroidViewModel porque necesita un Context para la caché,
 * los ajustes y el Geocoder.
 */
public class WeatherViewModel extends AndroidViewModel {

    private final WeatherRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ── Inicio / Pronóstico ──────────────────────────────────────────────────
    public final MutableLiveData<ClimaCombinado> clima = new MutableLiveData<>();
    public final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    public final MutableLiveData<Evento<String>> error = new MutableLiveData<>();

    // ── Buscar ───────────────────────────────────────────────────────────────
    public final MutableLiveData<List<Ubicacion>> resultadosBusqueda = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean> buscando = new MutableLiveData<>(false);
    private Runnable busquedaPendiente;
    private int numeroBusqueda = 0;

    // ── Favoritos: llave de la ubicación → resumen del clima ─────────────────
    public final MutableLiveData<Map<String, ClimaCombinado>> resumenes = new MutableLiveData<>(new HashMap<>());

    public WeatherViewModel(@NonNull Application app) {
        super(app);
        repository = new WeatherRepository(app);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Clima de la ubicación principal
    // ═════════════════════════════════════════════════════════════════════════

    /** Muestra una ubicación en Inicio (desde GPS, búsqueda o favoritos). */
    public void cargarUbicacion(Ubicacion ubicacion, boolean forzar) {
        Preferencias.setUltimaUbicacion(getApplication(), ubicacion);

        // 1) Mostrar al instante lo que haya en caché
        ClimaCombinado enCache = CacheClima.leer(getApplication(), ubicacion);
        if (enCache != null) clima.setValue(enCache);

        // 2) Descargar datos nuevos
        cargando.setValue(true);
        repository.obtenerClimaAsync(ubicacion, forzar, new WeatherRepository.Resultado<ClimaCombinado>() {
            @Override
            public void exito(ClimaCombinado datos) {
                cargando.setValue(false);
                clima.setValue(datos);
                Preferencias.setUltimaUbicacion(getApplication(), datos.ubicacion);
                // Los widgets también se benefician de los datos nuevos
                WidgetUpdater.actualizarTodos(getApplication());
            }

            @Override
            public void error(String mensaje) {
                cargando.setValue(false);
                error.setValue(new Evento<>(mensaje));
            }
        });
    }

    /** Recarga la ubicación actual (pull-to-refresh). */
    public void refrescar() {
        ClimaCombinado actual = clima.getValue();
        Ubicacion u = actual != null ? actual.ubicacion : Preferencias.ultimaUbicacion(getApplication());
        if (u != null) cargarUbicacion(u, true);
    }

    /** El GPS nos dio coordenadas: les ponemos nombre y cargamos el clima. */
    public void cargarDesdeGps(double lat, double lon) {
        cargando.setValue(true);
        repository.nombrarUbicacionAsync(lat, lon, new WeatherRepository.Resultado<Ubicacion>() {
            @Override
            public void exito(Ubicacion u) {
                Preferencias.setUltimoGps(getApplication(), u);
                cargarUbicacion(u, false);
            }

            @Override
            public void error(String mensaje) {
                cargando.setValue(false);
                error.setValue(new Evento<>(mensaje));
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Búsqueda con autocompletado
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Se llama en cada tecla. Esperamos 350 ms sin escribir antes de pedir a la API
     * ("debounce") para no hacer una petición por cada letra.
     */
    public void buscar(String texto) {
        if (busquedaPendiente != null) handler.removeCallbacks(busquedaPendiente);
        final String consulta = texto == null ? "" : texto.trim();
        if (consulta.length() < 2) {
            numeroBusqueda++;
            buscando.setValue(false);
            resultadosBusqueda.setValue(new ArrayList<>());
            return;
        }
        busquedaPendiente = () -> {
            final int miNumero = ++numeroBusqueda;
            buscando.setValue(true);
            repository.buscarCiudadesAsync(consulta, new WeatherRepository.Resultado<List<Ubicacion>>() {
                @Override
                public void exito(List<Ubicacion> datos) {
                    if (miNumero != numeroBusqueda) return; // llegó una respuesta vieja
                    buscando.setValue(false);
                    resultadosBusqueda.setValue(datos);
                }

                @Override
                public void error(String mensaje) {
                    if (miNumero != numeroBusqueda) return;
                    buscando.setValue(false);
                    error.setValue(new Evento<>("No se pudo buscar: revisa tu conexión"));
                }
            });
        };
        handler.postDelayed(busquedaPendiente, 350);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Favoritos
    // ═════════════════════════════════════════════════════════════════════════

    public void cargarResumenes(List<Ubicacion> favoritos) {
        for (Ubicacion u : favoritos) {
            repository.obtenerResumenAsync(u, new WeatherRepository.Resultado<ClimaCombinado>() {
                @Override
                public void exito(ClimaCombinado datos) {
                    Map<String, ClimaCombinado> mapa = new HashMap<>(resumenes.getValue());
                    mapa.put(u.llave(), datos);
                    resumenes.setValue(mapa);
                }

                @Override
                public void error(String mensaje) {
                    // En favoritos ignoramos errores individuales: la tarjeta se queda con "--"
                }
            });
        }
    }

    @Override
    protected void onCleared() {
        if (busquedaPendiente != null) handler.removeCallbacks(busquedaPendiente);
        super.onCleared();
    }
}
