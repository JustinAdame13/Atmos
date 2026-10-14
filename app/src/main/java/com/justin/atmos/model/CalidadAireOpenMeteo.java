package com.justin.atmos.model;

import com.google.gson.annotations.SerializedName;

/**
 * Respuesta de Open-Meteo → https://air-quality-api.open-meteo.com/v1/air-quality
 * us_aqi usa la escala de EE. UU. (0 a 500), más detallada que la de OpenWeather (1 a 5).
 */
public class CalidadAireOpenMeteo {

    @SerializedName("current")
    public Actual current;

    public static class Actual {
        @SerializedName("us_aqi")
        public Integer usAqi;

        @SerializedName("pm2_5")
        public Double pm25;

        @SerializedName("pm10")
        public Double pm10;

        @SerializedName("ozone")
        public Double ozono;

        @SerializedName("nitrogen_dioxide")
        public Double no2;
    }
}
