package com.justin.atmos.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.justin.atmos.Adapter.ForecastAdapter;
import com.justin.atmos.R;
import com.justin.atmos.Repository.WeatherRepository;
import com.justin.atmos.ViewModel.WeatherViewModel;
import com.justin.atmos.model.ClimaCombinado;
import com.justin.atmos.model.Ubicacion;
import com.justin.atmos.storage.FavoritesManager;
import com.justin.atmos.storage.Preferencias;
import com.justin.atmos.ui.views.ArcoSolView;
import com.justin.atmos.ui.views.GraficaTemperaturaView;
import com.justin.atmos.utils.Formato;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla principal. Muestra el clima combinado de la ubicación actual:
 * temperatura, comparativa de fuentes, gráfica por hora, 7 días, detalles,
 * sol y calidad del aire.
 */
public class HomeFragment extends Fragment {

    private WeatherViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private FavoritesManager favoritos;

    // ── vistas ───────────────────────────────────────────────────────────────
    private SwipeRefreshLayout swipe;
    private View estadoVacio, scroll;
    private ImageView ivTipoUbicacion;
    private ImageButton btnFavorito;
    private TextView tvCityName, tvRegion, tvDate, tvTemperature, tvDescription,
            tvMaxMin, tvFeelsLike, tvActualizado, tvAlerta,
            tvTempOm, tvTempOw, tvConfianza, tvFuentesDetalle, tvLeyenda,
            tvAmanecer, tvAtardecer, tvDuracionDia,
            tvAqiValor, tvAqiNivel, tvContaminantes, tvAqiOwm;
    private View cardAlerta, cardAire, puntoAqi, escalaAqi;
    private LottieAnimationView lottieWeather;
    private GraficaTemperaturaView graficaHoras;
    private LinearLayout containerDias;
    private ArcoSolView arcoSol;
    private Mosaico tileUv, tileViento, tileHumedad, tileLluvia, tilePresion, tileVisibilidad;

    private int animacionActual = 0;

    // ── permiso de ubicación ─────────────────────────────────────────────────
    private final ActivityResultLauncher<String[]> permisosLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    permisos -> {
                        boolean aceptado = Boolean.TRUE.equals(permisos.get(Manifest.permission.ACCESS_FINE_LOCATION))
                                || Boolean.TRUE.equals(permisos.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                        if (aceptado) {
                            obtenerUbicacion();
                        } else {
                            mostrarEstadoVacioSiNoHayDatos();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        conectarVistas(view);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        favoritos = new FavoritesManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // ── acciones ─────────────────────────────────────────────────────────
        swipe.setColorSchemeColors(0xFF0F1923);
        swipe.setProgressBackgroundColorSchemeColor(0xFFFFFFFF);
        swipe.setOnRefreshListener(() -> viewModel.refrescar());

        view.findViewById(R.id.btn_ajustes).setOnClickListener(v -> ((MainActivity) requireActivity()).abrirAjustes());
        view.findViewById(R.id.btn_ver_pronostico).setOnClickListener(v -> ((MainActivity) requireActivity()).irAPestana(R.id.nav_forecast));
        view.findViewById(R.id.btn_ir_buscar).setOnClickListener(v -> ((MainActivity) requireActivity()).irAPestana(R.id.nav_search));
        view.findViewById(R.id.btn_usar_ubicacion).setOnClickListener(v -> verificarPermisos());
        ivTipoUbicacion.setOnClickListener(v -> verificarPermisos()); // tocar el ícono = volver a "mi ubicación"
        btnFavorito.setOnClickListener(v -> alternarFavorito());

        // ── observar datos ───────────────────────────────────────────────────
        viewModel.clima.observe(getViewLifecycleOwner(), this::mostrar);
        viewModel.cargando.observe(getViewLifecycleOwner(), c -> swipe.setRefreshing(Boolean.TRUE.equals(c)));

        // ── primera carga ────────────────────────────────────────────────────
        if (viewModel.clima.getValue() == null) {
            Ubicacion ultima = Preferencias.ultimaUbicacion(requireContext());
            if (ultima != null && !ultima.esGps) {
                viewModel.cargarUbicacion(ultima, false);   // el usuario había elegido una ciudad
            } else if (tienePermiso()) {
                if (ultima != null) viewModel.cargarUbicacion(ultima, false); // datos rápidos mientras llega el GPS
                obtenerUbicacion();
            } else if (ultima != null) {
                viewModel.cargarUbicacion(ultima, false);
            } else {
                verificarPermisos();
            }
        }
    }

    private void conectarVistas(View v) {
        swipe = v.findViewById(R.id.swipe_refresh);
        scroll = v.findViewById(R.id.scroll_home);
        estadoVacio = v.findViewById(R.id.estado_vacio);
        ivTipoUbicacion = v.findViewById(R.id.iv_tipo_ubicacion);
        btnFavorito = v.findViewById(R.id.btn_favorito);
        tvCityName = v.findViewById(R.id.tv_city_name);
        tvRegion = v.findViewById(R.id.tv_region);
        tvDate = v.findViewById(R.id.tv_date);
        lottieWeather = v.findViewById(R.id.lottie_weather);
        tvTemperature = v.findViewById(R.id.tv_temperature);
        tvDescription = v.findViewById(R.id.tv_description);
        tvMaxMin = v.findViewById(R.id.tv_max_min);
        tvFeelsLike = v.findViewById(R.id.tv_feels_like);
        tvActualizado = v.findViewById(R.id.tv_actualizado);
        cardAlerta = v.findViewById(R.id.card_alerta);
        tvAlerta = v.findViewById(R.id.tv_alerta);
        tvTempOm = v.findViewById(R.id.tv_temp_om);
        tvTempOw = v.findViewById(R.id.tv_temp_ow);
        tvConfianza = v.findViewById(R.id.tv_confianza);
        tvFuentesDetalle = v.findViewById(R.id.tv_fuentes_detalle);
        graficaHoras = v.findViewById(R.id.grafica_horas);
        tvLeyenda = v.findViewById(R.id.tv_leyenda);
        containerDias = v.findViewById(R.id.container_dias);
        arcoSol = v.findViewById(R.id.arco_sol);
        tvAmanecer = v.findViewById(R.id.tv_amanecer);
        tvAtardecer = v.findViewById(R.id.tv_atardecer);
        tvDuracionDia = v.findViewById(R.id.tv_duracion_dia);
        cardAire = v.findViewById(R.id.card_aire);
        tvAqiValor = v.findViewById(R.id.tv_aqi_valor);
        tvAqiNivel = v.findViewById(R.id.tv_aqi_nivel);
        escalaAqi = v.findViewById(R.id.escala_aqi);
        puntoAqi = v.findViewById(R.id.punto_aqi);
        tvContaminantes = v.findViewById(R.id.tv_contaminantes);
        tvAqiOwm = v.findViewById(R.id.tv_aqi_owm);

        tileUv = new Mosaico(v.findViewById(R.id.tile_uv), R.drawable.ic_uv, "Índice UV");
        tileViento = new Mosaico(v.findViewById(R.id.tile_viento), R.drawable.ic_air, "Viento");
        tileHumedad = new Mosaico(v.findViewById(R.id.tile_humedad), R.drawable.ic_drop, "Humedad");
        tileLluvia = new Mosaico(v.findViewById(R.id.tile_lluvia), R.drawable.ic_cloud, "Precipitación");
        tilePresion = new Mosaico(v.findViewById(R.id.tile_presion), R.drawable.ic_speed, "Presión");
        tileVisibilidad = new Mosaico(v.findViewById(R.id.tile_visibilidad), R.drawable.ic_visibility, "Visibilidad");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Permisos y GPS
    // ═════════════════════════════════════════════════════════════════════════

    private boolean tienePermiso() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void verificarPermisos() {
        if (tienePermiso()) {
            obtenerUbicacion();
        } else {
            permisosLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressWarnings("MissingPermission") // ya verificamos con tienePermiso()
    private void obtenerUbicacion() {
        if (!tienePermiso()) return;
        estadoVacio.setVisibility(View.GONE);
        scroll.setVisibility(View.VISIBLE);
        swipe.setRefreshing(true);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (!isAdded()) return;
                    if (location != null) {
                        viewModel.cargarDesdeGps(location.getLatitude(), location.getLongitude());
                    } else {
                        swipe.setRefreshing(false);
                        Toast.makeText(requireContext(), "No se pudo obtener tu ubicación. ¿Está activado el GPS?", Toast.LENGTH_LONG).show();
                        mostrarEstadoVacioSiNoHayDatos();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    swipe.setRefreshing(false);
                    mostrarEstadoVacioSiNoHayDatos();
                });
    }

    private void mostrarEstadoVacioSiNoHayDatos() {
        if (viewModel.clima.getValue() == null) {
            estadoVacio.setVisibility(View.VISIBLE);
            scroll.setVisibility(View.INVISIBLE);
        }
    }

    private void alternarFavorito() {
        ClimaCombinado c = viewModel.clima.getValue();
        if (c == null) return;
        if (favoritos.esFavorito(c.ubicacion)) {
            favoritos.eliminarFavorito(c.ubicacion);
            Toast.makeText(requireContext(), "Quitado de favoritos", Toast.LENGTH_SHORT).show();
        } else {
            favoritos.guardarFavorito(c.ubicacion);
            Toast.makeText(requireContext(), "Agregado a favoritos ⭐", Toast.LENGTH_SHORT).show();
        }
        actualizarIconoFavorito(c.ubicacion);
    }

    private void actualizarIconoFavorito(Ubicacion u) {
        btnFavorito.setImageResource(favoritos.esFavorito(u) ? R.drawable.ic_star : R.drawable.ic_star_outline);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Pintar los datos
    // ═════════════════════════════════════════════════════════════════════════

    private void mostrar(ClimaCombinado c) {
        if (c == null) return;
        estadoVacio.setVisibility(View.GONE);
        scroll.setVisibility(View.VISIBLE);

        mostrarEncabezado(c);
        mostrarHeroe(c);
        mostrarAlerta(c);
        mostrarFuentes(c);
        mostrarHoras(c);
        mostrarDias(c);
        mostrarDetalles(c);
        mostrarSol(c);
        mostrarAire(c);
    }

    private void mostrarEncabezado(ClimaCombinado c) {
        Ubicacion u = c.ubicacion;
        tvCityName.setText(u.nombre);
        String sub = u.subtitulo();
        String bandera = u.bandera();
        tvRegion.setText(bandera.isEmpty() ? sub : bandera + "  " + sub);
        tvRegion.setVisibility(sub.isEmpty() && bandera.isEmpty() ? View.GONE : View.VISIBLE);
        ivTipoUbicacion.setImageResource(u.esGps ? R.drawable.ic_my_location : R.drawable.ic_location);
        long ahora = System.currentTimeMillis() / 1000L;
        tvDate.setText(Formato.fechaLarga(ahora, c.desfaseUtc) + " · " + Formato.hora(requireContext(), ahora, c.desfaseUtc));
        actualizarIconoFavorito(u);
    }

    private void mostrarHeroe(ClimaCombinado c) {
        ClimaCombinado.Actual a = c.actual;
        int anim = a.condicion.animacion(a.esDeDia);
        if (anim != animacionActual) {
            animacionActual = anim;
            lottieWeather.setAnimation(anim);
            lottieWeather.playAnimation();
        }
        tvTemperature.setText(Formato.temp(requireContext(), a.temperatura));
        tvDescription.setText(a.descripcion);
        if (a.maxHoy != null && a.minHoy != null) {
            tvMaxMin.setText("Máx " + Formato.temp(requireContext(), a.maxHoy) + "  ·  Mín " + Formato.temp(requireContext(), a.minHoy));
        } else {
            tvMaxMin.setText("");
        }
        tvFeelsLike.setText("Sensación térmica " + Formato.temp(requireContext(), a.sensacion));
        tvActualizado.setText(Formato.haceCuanto(c.actualizado));
    }

    private void mostrarAlerta(ClimaCombinado c) {
        ClimaCombinado.Hora lluvia = c.proximaLluvia(12, 50);
        if (c.actual.condicion.esLluvia()) {
            cardAlerta.setVisibility(View.VISIBLE);
            tvAlerta.setText("Está lloviendo ahora. Lleva paraguas ☔");
        } else if (lluvia != null) {
            cardAlerta.setVisibility(View.VISIBLE);
            tvAlerta.setText(String.format(Locale.getDefault(), "Probabilidad de lluvia de %d%% a las %s",
                    lluvia.probLluvia, Formato.hora(requireContext(), lluvia.tiempo, c.desfaseUtc)));
        } else {
            cardAlerta.setVisibility(View.GONE);
        }
    }

    private void mostrarFuentes(ClimaCombinado c) {
        ClimaCombinado.Actual a = c.actual;
        tvTempOm.setText(a.tempOpenMeteo != null ? Formato.temp(requireContext(), a.tempOpenMeteo) : "—");
        tvTempOw.setText(a.tempOpenWeather != null ? Formato.temp(requireContext(), a.tempOpenWeather) : "—");

        Double dif = c.diferenciaFuentes();
        if (dif == null) {
            tvConfianza.setText("Una fuente");
            tvConfianza.setTextColor(requireContext().getColor(R.color.text_on_glass_secondary));
            String motivo;
            if (!WeatherRepository.tieneApiKey()) {
                motivo = "Falta la API key de OpenWeather en local.properties. Se muestran solo datos de Open-Meteo.";
            } else if (!Preferencias.usarOpenWeather(requireContext())) {
                motivo = "OpenWeather está desactivado en Ajustes. Se muestran solo datos de Open-Meteo.";
            } else {
                motivo = "OpenWeather no respondió esta vez. Se muestran solo datos de Open-Meteo.";
            }
            tvFuentesDetalle.setText(motivo);
            tvLeyenda.setText("━ Open-Meteo (cada hora)");
            return;
        }

        int color;
        String nivel;
        if (dif <= 1.5) { nivel = "Coincidencia alta"; color = R.color.confianza_alta; }
        else if (dif <= 3) { nivel = "Coincidencia media"; color = R.color.confianza_media; }
        else { nivel = "Coincidencia baja"; color = R.color.confianza_baja; }
        tvConfianza.setText(nivel);
        tvConfianza.setTextColor(requireContext().getColor(color));

        StringBuilder sb = new StringBuilder();
        sb.append("Las fuentes difieren ").append(Formato.diferenciaTemp(requireContext(), dif))
                .append(". La temperatura principal es el promedio de ambas.");
        if (!c.dias.isEmpty() && c.dias.get(0).tieneDosFuentes()) {
            ClimaCombinado.Dia hoy = c.dias.get(0);
            sb.append("\nRango probable hoy: máx ").append(Formato.rango(requireContext(), hoy.maxBaja(), hoy.maxAlta()))
                    .append(" · mín ").append(Formato.rango(requireContext(), hoy.minBaja(), hoy.minAlta()));
        }
        tvFuentesDetalle.setText(sb.toString());
        tvLeyenda.setText("━ Open-Meteo (cada hora)   ┅ OpenWeather (cada 3 h)");
    }

    private void mostrarHoras(ClimaCombinado c) {
        List<GraficaTemperaturaView.Punto> puntos = new ArrayList<>();
        int total = Math.min(24, c.horas.size());
        for (int i = 0; i < total; i++) {
            ClimaCombinado.Hora h = c.horas.get(i);
            GraficaTemperaturaView.Punto p = new GraficaTemperaturaView.Punto();
            p.hora = i == 0 ? "Ahora" : Formato.horaCorta(requireContext(), h.tiempo, c.desfaseUtc);
            p.temp = Formato.convertirTemp(requireContext(), h.temperatura);
            p.tempSecundaria = h.tempOpenWeather != null ? Formato.convertirTemp(requireContext(), h.tempOpenWeather) : null;
            p.tempCelsius = h.temperatura;
            p.probLluvia = h.probLluvia;
            p.icono = h.condicion.icono(h.esDeDia);
            puntos.add(p);
        }
        graficaHoras.setPuntos(puntos);
    }

    private void mostrarDias(ClimaCombinado c) {
        containerDias.removeAllViews();
        int total = Math.min(7, c.dias.size());
        List<ClimaCombinado.Dia> semana = c.dias.subList(0, total);
        double[] rango = ForecastAdapter.rangoGlobal(semana);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < total; i++) {
            View fila = inflater.inflate(R.layout.item_forecast, containerDias, false);
            ForecastAdapter.llenarFila(fila, semana.get(i), c.desfaseUtc, rango[0], rango[1],
                    i == 0 ? c.actual.temperatura : null);
            fila.setOnClickListener(v -> ((MainActivity) requireActivity()).irAPestana(R.id.nav_forecast));
            containerDias.addView(fila);
        }
    }

    private void mostrarDetalles(ClimaCombinado c) {
        ClimaCombinado.Actual a = c.actual;

        if (a.uv != null) {
            tileUv.set(String.valueOf(Math.round(a.uv)), Formato.nivelUv(a.uv));
            tileUv.valor.setTextColor(Formato.colorUv(a.uv));
        } else {
            tileUv.set("--", "Sin datos");
        }

        String rafagas = a.rafagasKmh != null ? "Ráfagas " + Formato.viento(requireContext(), a.rafagasKmh) : "";
        tileViento.set(Formato.viento(requireContext(), a.vientoKmh),
                "Desde el " + Formato.puntoCardinal(a.direccionViento) + (rafagas.isEmpty() ? "" : " · " + rafagas));
        tileViento.extra.setVisibility(View.VISIBLE);
        // La flecha apunta hacia DONDE VA el viento (la dirección meteorológica indica de dónde viene)
        tileViento.extra.setRotation(a.direccionViento + 180);

        tileHumedad.set(a.humedad + "%", a.puntoRocio != null
                ? "Punto de rocío " + Formato.temp(requireContext(), a.puntoRocio) : "");

        int probHoy = c.dias.isEmpty() ? 0 : c.dias.get(0).probLluvia();
        String mm = a.precipitacionMm != null ? String.format(Locale.US, "%.1f mm", a.precipitacionMm) : "0 mm";
        tileLluvia.set(mm, "Prob. hoy " + probHoy + "%"
                + (a.nubosidad != null ? " · Nubes " + a.nubosidad + "%" : ""));

        if (a.presion != null) {
            String tendencia = a.presion > 1020 ? "Alta (estable)" : a.presion < 1005 ? "Baja (inestable)" : "Normal";
            tilePresion.set(Math.round(a.presion) + " hPa", tendencia);
        } else {
            tilePresion.set("--", "");
        }

        if (a.visibilidadKm != null) {
            String v = a.visibilidadKm >= 10 ? Math.round(a.visibilidadKm) + " km" : String.format(Locale.US, "%.1f km", a.visibilidadKm);
            String desc = a.visibilidadKm >= 10 ? "Despejado" : a.visibilidadKm >= 4 ? "Buena" : a.visibilidadKm >= 1 ? "Reducida" : "Muy baja";
            tileVisibilidad.set(v, desc);
        } else {
            tileVisibilidad.set("--", "");
        }
    }

    private void mostrarSol(ClimaCombinado c) {
        long am = c.actual.amanecer, at = c.actual.atardecer;
        if (am <= 0 || at <= 0) {
            tvAmanecer.setText("--");
            tvAtardecer.setText("--");
            tvDuracionDia.setText("");
            return;
        }
        arcoSol.setHorario(am, at);
        tvAmanecer.setText(Formato.hora(requireContext(), am, c.desfaseUtc));
        tvAtardecer.setText(Formato.hora(requireContext(), at, c.desfaseUtc));
        long minutos = (at - am) / 60;
        tvDuracionDia.setText("Duración del día: " + (minutos / 60) + " h " + (minutos % 60) + " min");
    }

    private void mostrarAire(ClimaCombinado c) {
        ClimaCombinado.Aire aire = c.aire;
        if (aire == null) {
            cardAire.setVisibility(View.GONE);
            return;
        }
        cardAire.setVisibility(View.VISIBLE);

        if (aire.usAqi != null) {
            int aqi = aire.usAqi;
            tvAqiValor.setText(String.valueOf(aqi));
            tvAqiNivel.setText(Formato.nivelAqi(aqi));
            tvAqiNivel.setTextColor(Formato.colorAqi(aqi));
            escalaAqi.setVisibility(View.VISIBLE);
            // Colocar el punto sobre la escala (0 a 300+) cuando ya conocemos el ancho
            escalaAqi.post(() -> {
                float f = Math.min(1f, aqi / 300f);
                puntoAqi.setTranslationX(f * (escalaAqi.getWidth() - puntoAqi.getWidth()));
            });
        } else if (aire.indiceOwm != null) {
            tvAqiValor.setText(aire.indiceOwm + "/5");
            tvAqiNivel.setText(Formato.nivelAqiOwm(aire.indiceOwm));
            tvAqiNivel.setTextColor(requireContext().getColor(R.color.text_on_glass));
            escalaAqi.setVisibility(View.GONE);
        }

        StringBuilder sb = new StringBuilder();
        if (aire.pm25 != null) sb.append(String.format(Locale.US, "PM2.5 %.0f", aire.pm25));
        if (aire.pm10 != null) sb.append(String.format(Locale.US, "   PM10 %.0f", aire.pm10));
        if (aire.ozono != null) sb.append(String.format(Locale.US, "   O₃ %.0f", aire.ozono));
        if (aire.no2 != null) sb.append(String.format(Locale.US, "   NO₂ %.0f", aire.no2));
        if (sb.length() > 0) sb.append("  μg/m³");
        tvContaminantes.setText(sb.toString().trim());

        if (aire.indiceOwm != null && aire.usAqi != null) {
            tvAqiOwm.setVisibility(View.VISIBLE);
            tvAqiOwm.setText("Escala US AQI (Open-Meteo) · OpenWeather: " + aire.indiceOwm + "/5 "
                    + Formato.nivelAqiOwm(aire.indiceOwm).toLowerCase(Locale.getDefault()));
        } else {
            tvAqiOwm.setVisibility(View.GONE);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════

    /** Agrupa las vistas de un mosaico de detalle (item_detalle.xml). */
    private static class Mosaico {
        final TextView valor, sub;
        final ImageView extra;

        Mosaico(View raiz, int icono, String titulo) {
            ((ImageView) raiz.findViewById(R.id.iv_detalle_icono)).setImageResource(icono);
            ((TextView) raiz.findViewById(R.id.tv_detalle_titulo)).setText(titulo);
            valor = raiz.findViewById(R.id.tv_detalle_valor);
            sub = raiz.findViewById(R.id.tv_detalle_sub);
            extra = raiz.findViewById(R.id.iv_detalle_extra);
        }

        void set(String v, String s) {
            valor.setText(v);
            sub.setText(s);
        }
    }
}
