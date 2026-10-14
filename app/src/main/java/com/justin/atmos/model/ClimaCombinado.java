package com.justin.atmos.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo "de la app" (dominio). Es el resultado de combinar las dos fuentes:
 * OpenWeather + Open-Meteo. La UI, los widgets y las notificaciones solo
 * trabajan con esta clase; no saben nada de JSON ni de Retrofit.
 *
 * Todas las temperaturas se guardan en °C y el viento en km/h.
 * La conversión a °F / mph se hace al mostrar (ver utils/Formato).
 * Los campos Double/Integer pueden ser null cuando una fuente no respondió.
 */
public class ClimaCombinado {

    public Ubicacion ubicacion;
    public long actualizado;        // System.currentTimeMillis() de la descarga
    public int desfaseUtc;          // segundos respecto a UTC del lugar consultado

    public Actual actual = new Actual();
    public List<Hora> horas = new ArrayList<>();   // próximas 24-48 h
    public List<Dia> dias = new ArrayList<>();     // hasta 16 días
    public Aire aire;                              // puede ser null

    /** true si OpenWeather respondió (si no, solo mostramos Open-Meteo). */
    public boolean tieneOpenWeather;

    // ─── clases internas ───────────────────────────────

    public static class Actual {
        public double temperatura;          // promedio de las fuentes disponibles
        public Double tempOpenMeteo;
        public Double tempOpenWeather;
        public double sensacion;
        public String descripcion;
        public CondicionClima condicion = CondicionClima.DESPEJADO;
        public boolean esDeDia = true;

        public int humedad;
        public Double puntoRocio;
        public Double presion;               // hPa
        public Double visibilidadKm;
        public double vientoKmh;
        public int direccionViento;          // grados
        public Double rafagasKmh;
        public Double uv;
        public Integer nubosidad;            // %
        public Double precipitacionMm;

        public long amanecer;                // Unix s
        public long atardecer;

        public Double maxHoy;
        public Double minHoy;
    }

    public static class Hora {
        public long tiempo;                  // Unix s
        public double temperatura;           // Open-Meteo
        public Double tempOpenWeather;       // solo en las horas que coinciden con el bloque de 3h
        public int probLluvia;               // %
        public CondicionClima condicion;
        public boolean esDeDia;
    }

    public static class Dia {
        public long fecha;                   // Unix s (medianoche local)
        public CondicionClima condicion;
        public int codigoWmo;

        public double maxOpenMeteo;
        public double minOpenMeteo;
        public Double maxOpenWeather;        // solo para los primeros 5 días
        public Double minOpenWeather;

        public Integer probLluviaOpenMeteo;
        public Integer probLluviaOpenWeather;

        public Double uvMax;
        public Double lluviaMm;
        public Double vientoMaxKmh;
        public long amanecer;
        public long atardecer;

        /** Rango combinado: la máxima más baja y la más alta entre las dos fuentes. */
        public double maxBaja() { return maxOpenWeather == null ? maxOpenMeteo : Math.min(maxOpenMeteo, maxOpenWeather); }
        public double maxAlta() { return maxOpenWeather == null ? maxOpenMeteo : Math.max(maxOpenMeteo, maxOpenWeather); }
        public double minBaja() { return minOpenWeather == null ? minOpenMeteo : Math.min(minOpenMeteo, minOpenWeather); }
        public double minAlta() { return minOpenWeather == null ? minOpenMeteo : Math.max(minOpenMeteo, minOpenWeather); }

        /** Promedio de máximas/mínimas (lo que se muestra como valor principal). */
        public double max() { return maxOpenWeather == null ? maxOpenMeteo : (maxOpenMeteo + maxOpenWeather) / 2.0; }
        public double min() { return minOpenWeather == null ? minOpenMeteo : (minOpenMeteo + minOpenWeather) / 2.0; }

        /** Probabilidad de lluvia combinada (promedio de lo disponible). */
        public int probLluvia() {
            if (probLluviaOpenMeteo != null && probLluviaOpenWeather != null) {
                return Math.round((probLluviaOpenMeteo + probLluviaOpenWeather) / 2f);
            }
            if (probLluviaOpenMeteo != null) return probLluviaOpenMeteo;
            if (probLluviaOpenWeather != null) return probLluviaOpenWeather;
            return 0;
        }

        public boolean tieneDosFuentes() { return maxOpenWeather != null; }
    }

    public static class Aire {
        public Integer usAqi;        // Open-Meteo (0-500)
        public Integer indiceOwm;    // OpenWeather (1-5)
        public Double pm25;
        public Double pm10;
        public Double ozono;
        public Double no2;
    }

    // ─── utilidades ─────────────────────────────────────

    /** Diferencia de temperatura actual entre las dos fuentes (null si solo hay una). */
    public Double diferenciaFuentes() {
        if (actual.tempOpenMeteo == null || actual.tempOpenWeather == null) return null;
        return Math.abs(actual.tempOpenMeteo - actual.tempOpenWeather);
    }

    /** Primera hora de las próximas `horasAdelante` con probabilidad de lluvia >= umbral. */
    public Hora proximaLluvia(int horasAdelante, int umbral) {
        long ahora = System.currentTimeMillis() / 1000L;
        for (Hora h : horas) {
            if (h.tiempo + 3600 < ahora) continue;
            if (h.tiempo > ahora + horasAdelante * 3600L) break;
            if (h.probLluvia >= umbral) return h;
        }
        return null;
    }
}
