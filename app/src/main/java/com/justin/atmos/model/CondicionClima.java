package com.justin.atmos.model;

import com.justin.atmos.R;

/**
 * Condición del clima "unificada".
 *
 * Cada API usa sus propios códigos:
 *  - OpenWeather usa ids como 800 (despejado), 500 (lluvia ligera), 211 (tormenta)...
 *  - Open-Meteo usa los códigos WMO: 0 (despejado), 61 (lluvia ligera), 95 (tormenta)...
 *
 * Este enum traduce ambos a un mismo idioma para que la UI (fondo, animación,
 * ícono y widgets) no tenga que saber de qué API vino el dato.
 */
public enum CondicionClima {
    DESPEJADO,
    PARCIAL,     // parcialmente nublado
    NUBLADO,
    NIEBLA,
    LLOVIZNA,
    LLUVIA,
    NIEVE,
    TORMENTA;

    // ── conversión desde cada API ────────────────────────────────────────────

    public static CondicionClima desdeWmo(int codigo) {
        if (codigo == 0 || codigo == 1) return DESPEJADO;
        if (codigo == 2) return PARCIAL;
        if (codigo == 3) return NUBLADO;
        if (codigo == 45 || codigo == 48) return NIEBLA;
        if (codigo >= 51 && codigo <= 57) return LLOVIZNA;
        if ((codigo >= 61 && codigo <= 67) || (codigo >= 80 && codigo <= 82)) return LLUVIA;
        if ((codigo >= 71 && codigo <= 77) || codigo == 85 || codigo == 86) return NIEVE;
        if (codigo >= 95) return TORMENTA;
        return NUBLADO;
    }

    public static CondicionClima desdeOpenWeather(int id) {
        if (id >= 200 && id < 300) return TORMENTA;
        if (id >= 300 && id < 400) return LLOVIZNA;
        if (id >= 500 && id < 600) return LLUVIA;
        if (id >= 600 && id < 700) return NIEVE;
        if (id >= 700 && id < 800) return NIEBLA;
        if (id == 800) return DESPEJADO;
        if (id == 801 || id == 802) return PARCIAL;
        return NUBLADO;
    }

    /** Descripción en español de un código WMO (Open-Meteo no manda texto). */
    public static String descripcionWmo(int codigo) {
        switch (codigo) {
            case 0: return "Despejado";
            case 1: return "Mayormente despejado";
            case 2: return "Parcialmente nublado";
            case 3: return "Nublado";
            case 45: return "Niebla";
            case 48: return "Niebla con escarcha";
            case 51: return "Llovizna ligera";
            case 53: return "Llovizna";
            case 55: return "Llovizna intensa";
            case 56: case 57: return "Llovizna helada";
            case 61: return "Lluvia ligera";
            case 63: return "Lluvia moderada";
            case 65: return "Lluvia intensa";
            case 66: case 67: return "Lluvia helada";
            case 71: return "Nevada ligera";
            case 73: return "Nevada moderada";
            case 75: return "Nevada intensa";
            case 77: return "Granizo fino";
            case 80: return "Chubascos ligeros";
            case 81: return "Chubascos";
            case 82: return "Chubascos fuertes";
            case 85: case 86: return "Chubascos de nieve";
            case 95: return "Tormenta eléctrica";
            case 96: case 99: return "Tormenta con granizo";
            default: return "Sin datos";
        }
    }

    // ── recursos visuales ────────────────────────────────────────────────────

    /** Animación Lottie de la carpeta res/raw. */
    public int animacion(boolean esDeDia) {
        switch (this) {
            case DESPEJADO: return esDeDia ? R.raw.anim_sunny : R.raw.anim_night;
            case PARCIAL:
            case NUBLADO:
            case NIEBLA: return esDeDia ? R.raw.anim_cloudy_day : R.raw.anim_cloudy_night;
            case LLOVIZNA:
            case LLUVIA: return esDeDia ? R.raw.anim_rainy_day : R.raw.anim_rainy_night;
            case NIEVE: return R.raw.anim_snow;
            case TORMENTA: return esDeDia ? R.raw.anim_storm : R.raw.anim_thunder;
            default: return R.raw.anim_sunny;
        }
    }

    /** Ícono vectorial (se usa en listas, gráfica y widgets). */
    public int icono(boolean esDeDia) {
        switch (this) {
            case DESPEJADO: return esDeDia ? R.drawable.ic_weather_sunny : R.drawable.ic_weather_night;
            case PARCIAL: return esDeDia ? R.drawable.ic_weather_partly_cloudy : R.drawable.ic_weather_cloudy_night;
            case NUBLADO:
            case NIEBLA: return esDeDia ? R.drawable.ic_weather_cloudy : R.drawable.ic_weather_cloudy_night;
            case LLOVIZNA:
            case LLUVIA: return R.drawable.ic_weather_rainy;
            case NIEVE: return R.drawable.ic_weather_snow;
            case TORMENTA: return R.drawable.ic_weather_storm;
            default: return R.drawable.ic_weather_sunny;
        }
    }

    /** Colores del degradado de fondo: {arriba, abajo}. */
    public int[] colores(boolean esDeDia) {
        switch (this) {
            case DESPEJADO:
                return esDeDia ? new int[]{0xFF2F80ED, 0xFF56B4E9} : new int[]{0xFF0B1A2E, 0xFF23395B};
            case PARCIAL:
                return esDeDia ? new int[]{0xFF3D6F9E, 0xFF77A0C8} : new int[]{0xFF141E33, 0xFF33415E};
            case NUBLADO:
                return esDeDia ? new int[]{0xFF4E5D70, 0xFF8494A8} : new int[]{0xFF161C26, 0xFF353F4F};
            case NIEBLA:
                return esDeDia ? new int[]{0xFF5F6B82, 0xFF919CB0} : new int[]{0xFF262D3A, 0xFF4A5468};
            case LLOVIZNA:
            case LLUVIA:
                return esDeDia ? new int[]{0xFF2C4B5E, 0xFF52768C} : new int[]{0xFF0D1821, 0xFF22374A};
            case NIEVE:
                return esDeDia ? new int[]{0xFF5878A6, 0xFF8FAFD3} : new int[]{0xFF1F2E44, 0xFF41587A};
            case TORMENTA:
                return esDeDia ? new int[]{0xFF2B2840, 0xFF4F4A73} : new int[]{0xFF100E1C, 0xFF2C2846};
            default:
                return new int[]{0xFF0F1923, 0xFF1E2D3D};
        }
    }

    /** Fondo con esquinas redondeadas para los widgets (RemoteViews no admite degradados creados en código). */
    public int fondoWidget(boolean esDeDia) {
        switch (this) {
            case DESPEJADO: return esDeDia ? R.drawable.bg_widget_clear_day : R.drawable.bg_widget_clear_night;
            case PARCIAL:
            case NUBLADO: return esDeDia ? R.drawable.bg_widget_cloudy_day : R.drawable.bg_widget_cloudy_night;
            case NIEBLA: return R.drawable.bg_widget_fog;
            case LLOVIZNA:
            case LLUVIA: return esDeDia ? R.drawable.bg_widget_rain_day : R.drawable.bg_widget_rain_night;
            case NIEVE: return R.drawable.bg_widget_snow;
            case TORMENTA: return R.drawable.bg_widget_storm;
            default: return R.drawable.bg_widget_clear_night;
        }
    }

    public boolean esLluvia() {
        return this == LLOVIZNA || this == LLUVIA || this == TORMENTA;
    }
}
