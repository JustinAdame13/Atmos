package com.justin.atmos.utils;

import android.content.Context;

import com.justin.atmos.storage.Preferencias;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Convierte números a texto bonito respetando los ajustes del usuario
 * (°C/°F, km/h/mph/m/s, 12/24 h) y la zona horaria DEL LUGAR consultado
 * (si ves el clima de Tokio, las horas salen en hora de Tokio).
 */
public class Formato {

    private static final Locale ES = new Locale("es", "MX");

    // ── temperatura ──────────────────────────────────────────────────────────

    public static double convertirTemp(Context c, double celsius) {
        return Preferencias.usaFahrenheit(c) ? celsius * 9 / 5 + 32 : celsius;
    }

    /** "28°" */
    public static String temp(Context c, double celsius) {
        return Math.round(convertirTemp(c, celsius)) + "°";
    }

    /** "28°C" */
    public static String tempConUnidad(Context c, double celsius) {
        return temp(c, celsius) + (Preferencias.usaFahrenheit(c) ? "F" : "C");
    }

    /** Diferencia de temperatura (sin sumar 32 al convertir). */
    public static String diferenciaTemp(Context c, double difCelsius) {
        double v = Preferencias.usaFahrenheit(c) ? difCelsius * 9 / 5 : difCelsius;
        return String.format(Locale.US, "%.1f°", v);
    }

    /** Rango: "31°" si ambos redondean igual, si no "31–33°" */
    public static String rango(Context c, double bajo, double alto) {
        long b = Math.round(convertirTemp(c, bajo));
        long a = Math.round(convertirTemp(c, alto));
        return b == a ? a + "°" : b + "–" + a + "°";
    }

    // ── viento ───────────────────────────────────────────────────────────────

    public static String viento(Context c, double kmh) {
        switch (Preferencias.unidadViento(c)) {
            case "mph": return Math.round(kmh / 1.609) + " mph";
            case "ms": return String.format(Locale.US, "%.1f m/s", kmh / 3.6);
            default: return Math.round(kmh) + " km/h";
        }
    }

    /** 0° → "N", 90° → "E"... */
    public static String puntoCardinal(int grados) {
        String[] puntos = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        return puntos[(int) Math.round(((grados % 360) / 45.0)) % 8];
    }

    // ── fechas y horas (en la zona horaria del lugar) ────────────────────────

    private static ZonedDateTime fecha(long unix, int desfase) {
        return Instant.ofEpochSecond(unix).atZone(ZoneOffset.ofTotalSeconds(desfase));
    }

    /** "14:00" o "2 p. m." según ajustes */
    public static String hora(Context c, long unix, int desfase) {
        String patron = Preferencias.formato24h(c) ? "HH:mm" : "h:mm a";
        return fecha(unix, desfase).format(DateTimeFormatter.ofPattern(patron, ES));
    }

    /** Versión corta para la gráfica: "14" o "2pm" */
    public static String horaCorta(Context c, long unix, int desfase) {
        ZonedDateTime z = fecha(unix, desfase);
        if (Preferencias.formato24h(c)) return z.format(DateTimeFormatter.ofPattern("HH", ES)) + ":00";
        int h = z.getHour();
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + (h < 12 ? "am" : "pm");
    }

    /** "miércoles, 14 de octubre" */
    public static String fechaLarga(long unix, int desfase) {
        String s = fecha(unix, desfase).format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ES));
        return capitalizar(s);
    }

    /** "Hoy", "Mañana" o "Vie" */
    public static String nombreDia(long unix, int desfase) {
        LocalDate dia = fecha(unix, desfase).toLocalDate();
        LocalDate hoy = Instant.now().atZone(ZoneOffset.ofTotalSeconds(desfase)).toLocalDate();
        if (dia.equals(hoy)) return "Hoy";
        if (dia.equals(hoy.plusDays(1))) return "Mañana";
        String s = dia.format(DateTimeFormatter.ofPattern("EEE", ES)).replace(".", "");
        return capitalizar(s);
    }

    /** "14 oct" */
    public static String fechaCorta(long unix, int desfase) {
        return fecha(unix, desfase).format(DateTimeFormatter.ofPattern("d MMM", ES)).replace(".", "");
    }

    public static boolean esHoy(long unix, int desfase) {
        LocalDate dia = fecha(unix, desfase).toLocalDate();
        return dia.equals(Instant.now().atZone(ZoneOffset.ofTotalSeconds(desfase)).toLocalDate());
    }

    /** "Actualizado hace 5 min" */
    public static String haceCuanto(long millis) {
        long min = (System.currentTimeMillis() - millis) / 60000;
        if (min < 1) return "Actualizado justo ahora";
        if (min < 60) return "Actualizado hace " + min + " min";
        long h = min / 60;
        if (h < 24) return "Actualizado hace " + h + " h";
        return "Actualizado hace " + (h / 24) + " d";
    }

    // ── índices ──────────────────────────────────────────────────────────────

    public static String nivelUv(double uv) {
        if (uv < 3) return "Bajo";
        if (uv < 6) return "Moderado";
        if (uv < 8) return "Alto";
        if (uv < 11) return "Muy alto";
        return "Extremo";
    }

    public static int colorUv(double uv) {
        if (uv < 3) return 0xFF81C784;
        if (uv < 6) return 0xFFFFD54F;
        if (uv < 8) return 0xFFFFA726;
        if (uv < 11) return 0xFFEF5350;
        return 0xFFAB47BC;
    }

    /** Escala US AQI (0-500). */
    public static String nivelAqi(int aqi) {
        if (aqi <= 50) return "Buena";
        if (aqi <= 100) return "Moderada";
        if (aqi <= 150) return "Dañina para sensibles";
        if (aqi <= 200) return "Dañina";
        if (aqi <= 300) return "Muy dañina";
        return "Peligrosa";
    }

    public static int colorAqi(int aqi) {
        if (aqi <= 50) return 0xFF66BB6A;
        if (aqi <= 100) return 0xFFFFEE58;
        if (aqi <= 150) return 0xFFFFA726;
        if (aqi <= 200) return 0xFFEF5350;
        if (aqi <= 300) return 0xFFAB47BC;
        return 0xFF8D6E63;
    }

    /** Escala OpenWeather (1-5). */
    public static String nivelAqiOwm(int indice) {
        switch (indice) {
            case 1: return "Buena";
            case 2: return "Aceptable";
            case 3: return "Moderada";
            case 4: return "Mala";
            case 5: return "Muy mala";
            default: return "—";
        }
    }

    /** Color según la temperatura (para la barra de rango y la gráfica). */
    public static int colorTemperatura(double celsius) {
        if (celsius <= 0) return 0xFF90CAF9;
        if (celsius <= 10) return 0xFF4FC3F7;
        if (celsius <= 18) return 0xFF4DD0E1;
        if (celsius <= 24) return 0xFF81C784;
        if (celsius <= 29) return 0xFFFFD54F;
        if (celsius <= 34) return 0xFFFFA726;
        return 0xFFEF5350;
    }

    public static String capitalizar(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(ES) + s.substring(1);
    }
}
