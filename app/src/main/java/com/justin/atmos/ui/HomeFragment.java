package com.justin.atmos.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.justin.atmos.R;
import com.justin.atmos.model.ClimaActual;
import com.justin.atmos.ViewModel.WeatherViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private WeatherViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private TextView tvCityName, tvDate, tvTemperature,
            tvDescription, tvFeelsLike, tvHumidity, tvWind;
    private LottieAnimationView lottieWeather;
    private LinearLayout containerHourly;

    // ── launcher para pedir permiso ──────────────────────
    private final ActivityResultLauncher<String[]> permisosLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    permisos -> {
                        boolean aceptado = Boolean.TRUE.equals(
                                permisos.get(Manifest.permission.ACCESS_FINE_LOCATION)) ||
                                Boolean.TRUE.equals(
                                        permisos.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                        if (aceptado) {
                            obtenerUbicacion();
                        } else {
                            navegarASearch();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── conectar vistas ──────────────────────────────────
        tvCityName    = view.findViewById(R.id.tv_city_name);
        tvDate        = view.findViewById(R.id.tv_date);
        tvTemperature = view.findViewById(R.id.tv_temperature);
        tvDescription = view.findViewById(R.id.tv_description);
        tvFeelsLike   = view.findViewById(R.id.tv_feels_like);
        tvHumidity    = view.findViewById(R.id.tv_humidity);
        tvWind        = view.findViewById(R.id.tv_wind);
        lottieWeather = view.findViewById(R.id.lottie_weather);
        containerHourly = view.findViewById(R.id.container_hourly);

        // ── inicializar GPS ──────────────────────────────────
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // ── obtener ViewModel ────────────────────────────────
        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // ── observar datos ───────────────────────────────────
        viewModel.climaActual.observe(getViewLifecycleOwner(), this::actualizarUI);
        viewModel.error.observe(getViewLifecycleOwner(), error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show());

        // ── si ya hay datos no pedimos ubicación de nuevo ────
        if (viewModel.climaActual.getValue() != null) {
            actualizarUI(viewModel.climaActual.getValue());
        } else {
            verificarPermisos();
        }
    }

    private void verificarPermisos() {
        boolean fineLoc = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLoc = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineLoc || coarseLoc) {
            obtenerUbicacion();
        } else {
            permisosLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void obtenerUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        viewModel.buscarClimaConCoordenadas(
                                location.getLatitude(),
                                location.getLongitude()
                        );
                    } else {
                        // GPS no disponible, ir a search
                        navegarASearch();
                    }
                })
                .addOnFailureListener(e -> navegarASearch());
    }

    private void navegarASearch() {
        if (getActivity() != null) {
            com.google.android.material.bottomnavigation.BottomNavigationView nav =
                    requireActivity().findViewById(R.id.bottom_navigation);
            nav.setSelectedItemId(R.id.nav_search);
        }
    }

    private void actualizarUI(ClimaActual clima) {
        tvCityName.setText(clima.cityName);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy", new Locale("es"));
        tvDate.setText(sdf.format(new Date(clima.dt * 1000L)));

        tvTemperature.setText(String.format(Locale.getDefault(), "%.0f°C", clima.main.temp));

        String desc = clima.weather.get(0).description;
        tvDescription.setText(desc.substring(0, 1).toUpperCase() + desc.substring(1));

        tvFeelsLike.setText(String.format(Locale.getDefault(), "%.0f°C", clima.main.feelsLike));
        tvHumidity.setText(String.format(Locale.getDefault(), "%d%%", clima.main.humidity));
        tvWind.setText(String.format(Locale.getDefault(), "%.1f km/h", clima.wind.speed * 3.6));

        lottieWeather.setAnimation(elegirAnimacion(
                clima.weather.get(0).id,
                clima.dt,
                clima.sys.sunrise,
                clima.sys.sunset
        ));
        lottieWeather.playAnimation();
    }

    private int elegirAnimacion(int weatherId, long ahora, long sunrise, long sunset) {
        boolean esDeNoche = ahora < sunrise || ahora > sunset;

        if (weatherId >= 200 && weatherId <= 232) return R.raw.anim_storm;
        if (weatherId >= 781 && weatherId <= 799) return R.raw.anim_thunder;
        if ((weatherId >= 300 && weatherId <= 321) ||
                (weatherId >= 500 && weatherId <= 531)) {
            return esDeNoche ? R.raw.anim_rainy_night : R.raw.anim_rainy_day;
        }
        if (weatherId >= 600 && weatherId <= 780) {
            return esDeNoche ? R.raw.anim_cloudy_night : R.raw.anim_cloudy_day;
        }
        return esDeNoche ? R.raw.anim_night : R.raw.anim_sunny;
    }
}