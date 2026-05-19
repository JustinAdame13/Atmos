package com.justin.atmos.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Pronostico {

    @SerializedName("city")
    public City city;

    @SerializedName("list")
    public List<ForecastItem> list;

    // ─── clases internas ───────────────────────────────

    public static class City {
        @SerializedName("name")
        public String name;

        @SerializedName("sunrise")
        public long sunrise;

        @SerializedName("sunset")
        public long sunset;
    }

    public static class ForecastItem {
        @SerializedName("dt")
        public long dt;

        @SerializedName("main")
        public ClimaActual.Main main;

        @SerializedName("weather")
        public List<ClimaActual.Weather> weather;

        @SerializedName("wind")
        public ClimaActual.Wind wind;

        @SerializedName("dt_txt")
        public String dtTxt; // fecha en texto, ej: "2026-05-15 12:00:00"
    }

}
