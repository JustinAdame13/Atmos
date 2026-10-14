package com.justin.atmos.model;

import java.util.Locale;

/**
 * Un lugar del mundo: lo que guardamos en favoritos, en los widgets
 * y como "última ubicación". Ya no guardamos solo el nombre (como en la v1)
 * sino las coordenadas, porque Open-Meteo trabaja con lat/lon.
 */
public class Ubicacion {

    public String nombre;
    public String estado;      // puede ser null
    public String pais;        // puede ser null
    public String codigoPais;  // "MX", "US"... puede ser null
    public double lat;
    public double lon;
    public boolean esGps;      // true si viene del GPS del teléfono

    public Ubicacion() { }

    public Ubicacion(String nombre, String estado, String pais, String codigoPais,
                     double lat, double lon) {
        this.nombre = nombre;
        this.estado = estado;
        this.pais = pais;
        this.codigoPais = codigoPais;
        this.lat = lat;
        this.lon = lon;
    }

    public static Ubicacion desdeLugar(ResultadoGeocoding.Lugar l) {
        return new Ubicacion(l.nombre, l.estado, l.pais, l.codigoPais, l.lat, l.lon);
    }

    /** Llave única (coordenadas redondeadas) para caché y para evitar duplicados. */
    public String llave() {
        return String.format(Locale.US, "%.2f,%.2f", lat, lon);
    }

    /** "Coahuila, México" */
    public String subtitulo() {
        StringBuilder sb = new StringBuilder();
        if (estado != null && !estado.isEmpty() && !estado.equalsIgnoreCase(nombre)) {
            sb.append(estado);
        }
        if (pais != null && !pais.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(pais);
        }
        return sb.toString();
    }

    /** Convierte "MX" en la bandera 🇲🇽 usando los "regional indicator symbols" de Unicode. */
    public String bandera() {
        if (codigoPais == null || codigoPais.length() != 2) return "";
        String cc = codigoPais.toUpperCase(Locale.US);
        int primero = Character.codePointAt(cc, 0) - 'A' + 0x1F1E6;
        int segundo = Character.codePointAt(cc, 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(primero)) + new String(Character.toChars(segundo));
    }
}
