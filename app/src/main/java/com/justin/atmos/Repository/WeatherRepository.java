package com.justin.atmos.Repository;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import com.justin.atmos.API.OpenMeteoService;
import com.justin.atmos.API.RetrofitClient;
import com.justin.atmos.API.WeatherService;
import com.justin.atmos.BuildConfig;
import com.justin.atmos.model.CalidadAireOWM;
import com.justin.atmos.model.CalidadAireOpenMeteo;
import com.justin.atmos.model.ClimaActual;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.model.CondicionClima;
import com.justin.atmos.model.Pronostico;
import com.justin.atmos.model.PronosticoOpenMeteo;
import com.justin.atmos.model.ResultadoGeocoding;
import com.justin.atmos.model.Ubicacion;
import com.justin.atmos.storage.CacheClima;
import com.justin.atmos.storage.Preferencias;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Repositorio: el único lugar que habla con las APIs.
 *
 * Cambio importante respecto a la v1:
 *  En la v1 cada petición usaba enqueue() con su propio Callback. Para combinar
 *  5 peticiones de 2 APIs eso se vuelve un "callback hell". En la v2 hacemos las
 *  peticiones en hilos de fondo (ExecutorService) con execute(), que es síncrono,
 *  esperamos los resultados con Future.get() y luego combinamos todo.
 *
 *  - obtenerClima(...)      → síncrono. Lo usan los widgets y las notificaciones
 *                             (que ya corren en segundo plano).
 *  - obtenerClimaAsync(...) → asíncrono. Lo usa el ViewModel y entrega el
 *                             resultado en el hilo principal.
 */
public class WeatherRepository {

    /** Respuesta asíncrona entregada en el hilo principal. */
    public interface Resultado<T> {
        void exito(T datos);
        void error(String mensaje);
    }

    private static final String UNIDADES = "metric";
    private static final String IDIOMA = "es";
    private static final int DIAS_PRONOSTICO = 16;
    private static final int HORAS_A_MOSTRAR = 48;

    // Dos grupos de hilos compartidos por toda la app:
    //  - hilos:    tareas "grandes" (obtener clima, buscar...).
    //  - hilosRed: cada petición HTTP individual. Van separados para que una tarea
    //              grande que espera a sus peticiones nunca bloquee a otra (deadlock).
    private static final ExecutorService hilos = Executors.newFixedThreadPool(4);
    private static final ExecutorService hilosRed = Executors.newFixedThreadPool(8);

    private final Context context;
    private final WeatherService openWeather;
    private final OpenMeteoService openMeteo;
    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());

    public WeatherRepository(Context context) {
        this.context = context.getApplicationContext();
        RetrofitClient cliente = RetrofitClient.getInstance();
        openWeather = cliente.openWeather();
        openMeteo = cliente.openMeteo();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  API pública
    // ═════════════════════════════════════════════════════════════════════════

    public void obtenerClimaAsync(Ubicacion ubicacion, boolean forzar, Resultado<ClimaCombinado> cb) {
        hilos.execute(() -> {
            try {
                ClimaCombinado clima = obtenerClima(ubicacion, forzar);
                hiloPrincipal.post(() -> cb.exito(clima));
            } catch (IOException e) {
                hiloPrincipal.post(() -> cb.error(e.getMessage()));
            }
        });
    }

    public void obtenerResumenAsync(Ubicacion ubicacion, Resultado<ClimaCombinado> cb) {
        hilos.execute(() -> {
            try {
                ClimaCombinado c = obtenerResumen(ubicacion);
                hiloPrincipal.post(() -> cb.exito(c));
            } catch (IOException e) {
                hiloPrincipal.post(() -> cb.error(e.getMessage()));
            }
        });
    }

    public void buscarCiudadesAsync(String texto, Resultado<List<Ubicacion>> cb) {
        hilos.execute(() -> {
            try {
                List<Ubicacion> lista = buscarCiudades(texto);
                hiloPrincipal.post(() -> cb.exito(lista));
            } catch (IOException e) {
                hiloPrincipal.post(() -> cb.error(e.getMessage()));
            }
        });
    }

    /** Pone nombre a unas coordenadas del GPS (ciudad, estado, país). */
    public void nombrarUbicacionAsync(double lat, double lon, Resultado<Ubicacion> cb) {
        hilos.execute(() -> {
            Ubicacion u = nombrarUbicacion(lat, lon);
            hiloPrincipal.post(() -> cb.exito(u));
        });
    }

    /**
     * Descarga y combina el clima de ambas fuentes. SÍNCRONO: no llamar desde el hilo principal.
     *
     * @param forzar si es false y hay datos en caché de hace menos de 15 min, se devuelven esos.
     */
    public ClimaCombinado obtenerClima(Ubicacion ubicacion, boolean forzar) throws IOException {
        if (!forzar) {
            ClimaCombinado enCache = CacheClima.leer(context, ubicacion);
            if (CacheClima.esReciente(enCache)) return enCache;
        }

        final double lat = ubicacion.lat;
        final double lon = ubicacion.lon;
        final boolean conOwm = tieneApiKey() && Preferencias.usarOpenWeather(context);
        final String key = BuildConfig.OPENWEATHER_API_KEY;

        // 1) Lanzar todas las peticiones EN PARALELO
        Future<PronosticoOpenMeteo> fOm = hilosRed.submit(() -> ejecutar(openMeteo.getPronostico(
                lat, lon,
                OpenMeteoService.VARIABLES_ACTUALES,
                OpenMeteoService.VARIABLES_HORARIAS,
                OpenMeteoService.VARIABLES_DIARIAS,
                DIAS_PRONOSTICO, "auto", "unixtime")));

        Future<CalidadAireOpenMeteo> fAireOm = hilosRed.submit(() -> ejecutar(openMeteo.getCalidadAire(
                lat, lon, "us_aqi,pm2_5,pm10,ozone,nitrogen_dioxide")));

        Future<ClimaActual> fOwActual = null;
        Future<Pronostico> fOwPronostico = null;
        Future<CalidadAireOWM> fOwAire = null;
        if (conOwm) {
            fOwActual = hilosRed.submit(() -> ejecutar(openWeather.getClimaActualPorCoordenadas(lat, lon, key, UNIDADES, IDIOMA)));
            fOwPronostico = hilosRed.submit(() -> ejecutar(openWeather.getPronosticoPorCoordenadas(lat, lon, key, UNIDADES, IDIOMA)));
            fOwAire = hilosRed.submit(() -> ejecutar(openWeather.getCalidadAire(lat, lon, key)));
        }

        // 2) Esperar resultados (los de OpenWeather son opcionales)
        PronosticoOpenMeteo om = esperar(fOm);
        CalidadAireOpenMeteo aireOm = esperar(fAireOm);
        ClimaActual owActual = esperar(fOwActual);
        Pronostico owPronostico = esperar(fOwPronostico);
        CalidadAireOWM owAire = esperar(fOwAire);

        if (om == null) {
            // Plan B: reintentar con menos variables (por si alguna no está disponible)
            try {
                om = ejecutar(openMeteo.getPronostico(lat, lon,
                        OpenMeteoService.VARIABLES_ACTUALES_BASICAS,
                        OpenMeteoService.VARIABLES_HORARIAS,
                        OpenMeteoService.VARIABLES_DIARIAS,
                        DIAS_PRONOSTICO, "auto", "unixtime"));
            } catch (IOException ignorado) { }
        }

        if (om == null || om.current == null) {
            // Open-Meteo es la fuente principal: sin ella no podemos armar el pronóstico.
            ClimaCombinado enCache = CacheClima.leer(context, ubicacion);
            if (enCache != null) return enCache; // mejor datos viejos que nada
            throw new IOException("Sin conexión a internet");
        }

        // 3) Combinar
        ClimaCombinado clima = CombinadorClima.combinar(ubicacion, om, aireOm, owActual, owPronostico, owAire);

        // Si la ubicación no tenía nombre (GPS) usamos el de OpenWeather
        if ((ubicacion.nombre == null || ubicacion.nombre.isEmpty()) && owActual != null) {
            ubicacion.nombre = owActual.cityName;
        }

        CacheClima.guardar(context, clima);
        return clima;
    }

    /** Petición ligera solo de Open-Meteo (para la lista de favoritos). */
    public ClimaCombinado obtenerResumen(Ubicacion u) throws IOException {
        ClimaCombinado enCache = CacheClima.leer(context, u);
        if (CacheClima.esReciente(enCache)) return enCache;

        PronosticoOpenMeteo om = ejecutar(openMeteo.getResumen(u.lat, u.lon,
                "temperature_2m,weather_code,is_day",
                "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
                1, "auto", "unixtime"));
        if (om == null || om.current == null) {
            if (enCache != null) return enCache;
            throw new IOException("Sin conexión a internet");
        }
        return CombinadorClima.combinarResumen(u, om);
    }

    public List<Ubicacion> buscarCiudades(String texto) throws IOException {
        ResultadoGeocoding r = ejecutar(openMeteo.buscarCiudad(texto, 10, IDIOMA, "json"));
        List<Ubicacion> lista = new ArrayList<>();
        if (r != null && r.results != null) {
            for (ResultadoGeocoding.Lugar l : r.results) {
                lista.add(Ubicacion.desdeLugar(l));
            }
        }
        return lista;
    }

    /** Usa el Geocoder de Android (sin internet extra en muchos teléfonos). SÍNCRONO. */
    @SuppressWarnings("deprecation")
    public Ubicacion nombrarUbicacion(double lat, double lon) {
        Ubicacion u = new Ubicacion(null, null, null, null, lat, lon);
        u.esGps = true;
        try {
            if (Geocoder.isPresent()) {
                Geocoder geocoder = new Geocoder(context, new Locale("es"));
                List<Address> dir = geocoder.getFromLocation(lat, lon, 1);
                if (dir != null && !dir.isEmpty()) {
                    Address a = dir.get(0);
                    u.nombre = a.getLocality() != null ? a.getLocality()
                            : (a.getSubAdminArea() != null ? a.getSubAdminArea() : a.getAdminArea());
                    u.estado = a.getAdminArea();
                    u.pais = a.getCountryName();
                    u.codigoPais = a.getCountryCode();
                }
            }
        } catch (Exception ignorado) {
            // Sin Geocoder: el nombre lo pondrá OpenWeather al descargar el clima
        }
        if (u.nombre == null) {
            try {
                ClimaActual ow = tieneApiKey() ? ejecutar(openWeather.getClimaActualPorCoordenadas(
                        lat, lon, BuildConfig.OPENWEATHER_API_KEY, UNIDADES, IDIOMA)) : null;
                if (ow != null) {
                    u.nombre = ow.cityName;
                    if (ow.sys != null) u.codigoPais = ow.sys.country;
                }
            } catch (IOException ignorado) { }
        }
        if (u.nombre == null || u.nombre.isEmpty()) u.nombre = "Mi ubicación";
        return u;
    }

    public static boolean tieneApiKey() {
        String k = BuildConfig.OPENWEATHER_API_KEY;
        return k != null && !k.isEmpty() && !"null".equals(k);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Ayudantes
    // ═════════════════════════════════════════════════════════════════════════

    /** Ejecuta una llamada Retrofit de forma síncrona. Devuelve null si el servidor respondió con error. */
    private static <T> T ejecutar(Call<T> llamada) throws IOException {
        Response<T> r = llamada.execute();
        return r.isSuccessful() ? r.body() : null;
    }

    /** Espera un Future; si falló (sin internet, error, etc.) devuelve null. */
    private static <T> T esperar(Future<T> f) {
        if (f == null) return null;
        try {
            return f.get();
        } catch (Exception e) {
            return null;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Lógica de combinación de las dos fuentes
    // ═════════════════════════════════════════════════════════════════════════

    static class CombinadorClima {

        static ClimaCombinado combinar(Ubicacion ubicacion,
                                       PronosticoOpenMeteo om,
                                       CalidadAireOpenMeteo aireOm,
                                       ClimaActual owActual,
                                       Pronostico owPronostico,
                                       CalidadAireOWM owAire) {
            ClimaCombinado c = new ClimaCombinado();
            c.ubicacion = ubicacion;
            c.actualizado = System.currentTimeMillis();
            c.desfaseUtc = om.utcOffsetSeconds;
            c.tieneOpenWeather = owActual != null;

            llenarActual(c, om.current, owActual);
            llenarDias(c, om.daily, owPronostico);
            llenarHoras(c, om.hourly, owPronostico);
            llenarAire(c, aireOm, owAire);

            // Amanecer / atardecer y máx/mín de hoy salen del primer día
            if (!c.dias.isEmpty()) {
                ClimaCombinado.Dia hoy = c.dias.get(0);
                c.actual.amanecer = hoy.amanecer;
                c.actual.atardecer = hoy.atardecer;
                c.actual.maxHoy = hoy.max();
                c.actual.minHoy = hoy.min();
            } else if (owActual != null && owActual.sys != null) {
                c.actual.amanecer = owActual.sys.sunrise;
                c.actual.atardecer = owActual.sys.sunset;
            }
            return c;
        }

        static ClimaCombinado combinarResumen(Ubicacion u, PronosticoOpenMeteo om) {
            ClimaCombinado c = new ClimaCombinado();
            c.ubicacion = u;
            c.actualizado = System.currentTimeMillis();
            c.desfaseUtc = om.utcOffsetSeconds;
            int codigo = valor(om.current.codigoClima, 0);
            c.actual.temperatura = valor(om.current.temperatura, 0.0);
            c.actual.tempOpenMeteo = om.current.temperatura;
            c.actual.condicion = CondicionClima.desdeWmo(codigo);
            c.actual.descripcion = CondicionClima.descripcionWmo(codigo);
            c.actual.esDeDia = valor(om.current.esDeDia, 1) == 1;
            if (om.daily != null && om.daily.maxima != null && !om.daily.maxima.isEmpty()) {
                c.actual.maxHoy = om.daily.maxima.get(0);
                c.actual.minHoy = om.daily.minima.get(0);
            }
            // NO se guarda en caché: es un resumen incompleto y pisaría el clima completo.
            return c;
        }

        private static void llenarActual(ClimaCombinado c, PronosticoOpenMeteo.Actual a, ClimaActual ow) {
            ClimaCombinado.Actual x = c.actual;
            int codigo = valor(a.codigoClima, 0);

            x.tempOpenMeteo = a.temperatura;
            x.tempOpenWeather = ow != null && ow.main != null ? ow.main.temp : null;
            x.temperatura = promedio(x.tempOpenMeteo, x.tempOpenWeather);

            x.sensacion = a.sensacion != null ? a.sensacion
                    : (ow != null && ow.main != null ? ow.main.feelsLike : x.temperatura);

            x.condicion = a.codigoClima != null ? CondicionClima.desdeWmo(codigo)
                    : (ow != null && ow.weather != null && !ow.weather.isEmpty()
                    ? CondicionClima.desdeOpenWeather(ow.weather.get(0).id) : CondicionClima.DESPEJADO);
            x.esDeDia = valor(a.esDeDia, 1) == 1;

            // OpenWeather da descripciones más ricas en español ("nubes dispersas", "lluvia ligera")
            if (ow != null && ow.weather != null && !ow.weather.isEmpty() && ow.weather.get(0).description != null) {
                x.descripcion = capitalizar(ow.weather.get(0).description);
            } else {
                x.descripcion = CondicionClima.descripcionWmo(codigo);
            }

            x.humedad = a.humedad != null ? a.humedad : (ow != null && ow.main != null ? ow.main.humidity : 0);
            x.puntoRocio = a.puntoRocio;
            x.presion = a.presion != null ? a.presion
                    : (ow != null && ow.main != null ? (Double) (double) ow.main.pressure : null);

            if (a.visibilidad != null) x.visibilidadKm = a.visibilidad / 1000.0;
            else if (ow != null && ow.visibility != null) x.visibilidadKm = ow.visibility / 1000.0;

            if (a.viento != null) {
                x.vientoKmh = a.viento;
                x.direccionViento = valor(a.direccionViento, 0);
                x.rafagasKmh = a.rafagas;
            } else if (ow != null && ow.wind != null) {
                x.vientoKmh = ow.wind.speed * 3.6;
                x.direccionViento = ow.wind.deg;
                x.rafagasKmh = ow.wind.gust != null ? ow.wind.gust * 3.6 : null;
            }

            x.uv = a.uv;
            x.nubosidad = a.nubosidad != null ? a.nubosidad
                    : (ow != null && ow.clouds != null ? (Integer) ow.clouds.all : null);
            x.precipitacionMm = a.precipitacion;
        }

        private static void llenarDias(ClimaCombinado c, PronosticoOpenMeteo.PorDia d, Pronostico ow) {
            if (d == null || d.time == null) return;

            // Agrupar los bloques de 3 h de OpenWeather por día local
            Map<Long, double[]> owPorDia = agruparOpenWeatherPorDia(ow, c.desfaseUtc);

            for (int i = 0; i < d.time.size(); i++) {
                ClimaCombinado.Dia dia = new ClimaCombinado.Dia();
                dia.fecha = d.time.get(i);
                dia.codigoWmo = valor(get(d.codigoClima, i), 0);
                dia.condicion = CondicionClima.desdeWmo(dia.codigoWmo);
                dia.maxOpenMeteo = valor(get(d.maxima, i), 0.0);
                dia.minOpenMeteo = valor(get(d.minima, i), 0.0);
                dia.probLluviaOpenMeteo = get(d.probLluviaMax, i);
                dia.uvMax = get(d.uvMax, i);
                dia.lluviaMm = get(d.lluviaTotal, i);
                dia.vientoMaxKmh = get(d.vientoMax, i);
                Long am = get(d.amanecer, i);
                Long at = get(d.atardecer, i);
                dia.amanecer = am != null ? am : 0;
                dia.atardecer = at != null ? at : 0;

                double[] owDia = owPorDia.get(diaLocal(dia.fecha, c.desfaseUtc));
                // owDia = {max, min, probMax, cantidadDeBloques}
                if (owDia != null && owDia[3] >= 3) { // al menos 9 h de datos para que sea representativo
                    dia.maxOpenWeather = owDia[0];
                    dia.minOpenWeather = owDia[1];
                    dia.probLluviaOpenWeather = (int) Math.round(owDia[2] * 100);
                }
                c.dias.add(dia);
            }
        }

        private static void llenarHoras(ClimaCombinado c, PronosticoOpenMeteo.PorHora h, Pronostico ow) {
            if (h == null || h.time == null) return;

            Map<Long, Double> owPorHora = new HashMap<>();
            if (ow != null && ow.list != null) {
                for (Pronostico.ForecastItem item : ow.list) {
                    if (item.main != null) owPorHora.put(item.dt, item.main.temp);
                }
            }

            long ahora = System.currentTimeMillis() / 1000L;
            for (int i = 0; i < h.time.size() && c.horas.size() < HORAS_A_MOSTRAR; i++) {
                long t = h.time.get(i);
                if (t + 3600 <= ahora) continue; // horas que ya pasaron
                ClimaCombinado.Hora hora = new ClimaCombinado.Hora();
                hora.tiempo = t;
                hora.temperatura = valor(get(h.temperatura, i), 0.0);
                hora.probLluvia = valor(get(h.probLluvia, i), 0);
                hora.condicion = CondicionClima.desdeWmo(valor(get(h.codigoClima, i), 0));
                hora.esDeDia = valor(get(h.esDeDia, i), 1) == 1;
                hora.tempOpenWeather = owPorHora.get(t);
                c.horas.add(hora);
            }
        }

        private static void llenarAire(ClimaCombinado c, CalidadAireOpenMeteo om, CalidadAireOWM ow) {
            CalidadAireOWM.Item owItem = ow != null && ow.list != null && !ow.list.isEmpty() ? ow.list.get(0) : null;
            if ((om == null || om.current == null) && owItem == null) return;

            ClimaCombinado.Aire aire = new ClimaCombinado.Aire();
            if (om != null && om.current != null) {
                aire.usAqi = om.current.usAqi;
                aire.pm25 = om.current.pm25;
                aire.pm10 = om.current.pm10;
                aire.ozono = om.current.ozono;
                aire.no2 = om.current.no2;
            }
            if (owItem != null) {
                if (owItem.main != null) aire.indiceOwm = owItem.main.aqi;
                if (owItem.components != null) {
                    if (aire.pm25 == null) aire.pm25 = owItem.components.pm25;
                    if (aire.pm10 == null) aire.pm10 = owItem.components.pm10;
                    if (aire.ozono == null) aire.ozono = owItem.components.o3;
                    if (aire.no2 == null) aire.no2 = owItem.components.no2;
                }
            }
            c.aire = aire;
        }

        /** {max, min, probMax(0..1), cantidad} por día local. */
        private static Map<Long, double[]> agruparOpenWeatherPorDia(Pronostico ow, int desfase) {
            Map<Long, double[]> mapa = new HashMap<>();
            if (ow == null || ow.list == null) return mapa;
            for (Pronostico.ForecastItem item : ow.list) {
                if (item.main == null) continue;
                long dia = diaLocal(item.dt, desfase);
                double[] v = mapa.get(dia);
                if (v == null) {
                    v = new double[]{-1000, 1000, 0, 0};
                    mapa.put(dia, v);
                }
                v[0] = Math.max(v[0], item.main.temp);
                v[1] = Math.min(v[1], item.main.temp);
                v[2] = Math.max(v[2], item.pop);
                v[3]++;
            }
            return mapa;
        }

        /** Número de día local (días desde 1970 en la zona horaria del lugar). */
        private static long diaLocal(long unix, int desfase) {
            return Math.floorDiv(unix + desfase, 86400L);
        }

        private static double promedio(Double a, Double b) {
            if (a != null && b != null) return (a + b) / 2.0;
            if (a != null) return a;
            if (b != null) return b;
            return 0;
        }

        private static <T> T get(List<T> lista, int i) {
            return lista != null && i < lista.size() ? lista.get(i) : null;
        }

        private static int valor(Integer v, int defecto) { return v != null ? v : defecto; }

        private static double valor(Double v, double defecto) { return v != null ? v : defecto; }

        private static String capitalizar(String s) {
            if (s == null || s.isEmpty()) return s;
            return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
        }
    }
}
