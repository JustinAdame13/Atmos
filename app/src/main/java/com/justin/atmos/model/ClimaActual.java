package com.justin.atmos.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
public class ClimaActual {

    @SerializedName("name")
    public String cityName;

    @SerializedName("weather")
    public List<Weather> weather;

    @SerializedName("main")
    public Main main;

    @SerializedName("wind")
    public Wind wind;

    @SerializedName("sys")
    public Sys sys;

    @SerializedName("dt")
    public long dt; // fecha actual en Unix timestamp

    // ─── clases internas ───────────────────────────────

    public static class Weather {
        @SerializedName("id")
        public int id;

        @SerializedName("main")
        public String main;

        @SerializedName("description")
        public String description;

        @SerializedName("icon")
        public String icon;
    }

    public static class Main {
        @SerializedName("temp")
        public double temp;

        @SerializedName("feels_like")
        public double feelsLike;

        @SerializedName("humidity")
        public int humidity;
    }

    public static class Wind {
        @SerializedName("speed")
        public double speed;
    }

    public static class Sys {
        @SerializedName("sunrise")
        public long sunrise; // Unix timestamp

        @SerializedName("sunset")
        public long sunset;  // Unix timestamp
    }
}
