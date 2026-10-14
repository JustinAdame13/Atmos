package com.justin.atmos.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta de OpenWeatherMap → /data/2.5/air_pollution
 * aqi va de 1 (bueno) a 5 (muy malo).
 */
public class CalidadAireOWM {

    @SerializedName("list")
    public List<Item> list;

    public static class Item {
        @SerializedName("main")
        public Indice main;

        @SerializedName("components")
        public Componentes components;
    }

    public static class Indice {
        @SerializedName("aqi")
        public int aqi;
    }

    /** Concentraciones en μg/m³ */
    public static class Componentes {
        @SerializedName("pm2_5")
        public double pm25;

        @SerializedName("pm10")
        public double pm10;

        @SerializedName("o3")
        public double o3;

        @SerializedName("no2")
        public double no2;

        @SerializedName("co")
        public double co;

        @SerializedName("so2")
        public double so2;
    }
}
