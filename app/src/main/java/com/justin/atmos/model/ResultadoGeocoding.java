package com.justin.atmos.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta de Open-Meteo Geocoding → https://geocoding-api.open-meteo.com/v1/search
 * Sirve para el buscador con autocompletado: devuelve ciudades con su estado,
 * país y coordenadas.
 */
public class ResultadoGeocoding {

    @SerializedName("results")
    public List<Lugar> results;

    public static class Lugar {
        @SerializedName("name")
        public String nombre;

        @SerializedName("admin1")
        public String estado;

        @SerializedName("country")
        public String pais;

        @SerializedName("country_code")
        public String codigoPais;

        @SerializedName("latitude")
        public double lat;

        @SerializedName("longitude")
        public double lon;

        @SerializedName("population")
        public Long poblacion;
    }
}
