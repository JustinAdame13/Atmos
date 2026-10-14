package com.justin.atmos.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justin.atmos.Adapter.ForecastAdapter;
import com.justin.atmos.R;
import com.justin.atmos.ViewModel.WeatherViewModel;
import com.justin.atmos.model.ClimaCombinado;

/**
 * Pestaña de pronóstico extendido (hasta 16 días).
 * Usa el mismo ViewModel que Inicio, así que no hace peticiones propias.
 */
public class ForecastFragment extends Fragment {

    private ForecastAdapter adapter;
    private TextView tvForecastTitle, tvForecastCity, tvVacio;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forecast, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvForecastTitle = view.findViewById(R.id.tv_forecast_title);
        tvForecastCity = view.findViewById(R.id.tv_forecast_city);
        tvVacio = view.findViewById(R.id.tv_forecast_vacio);

        RecyclerView rv = view.findViewById(R.id.rv_forecast);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ForecastAdapter();
        rv.setAdapter(adapter);

        WeatherViewModel viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        viewModel.clima.observe(getViewLifecycleOwner(), this::mostrar);
    }

    private void mostrar(ClimaCombinado c) {
        if (c == null || c.dias.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
            return;
        }
        tvVacio.setVisibility(View.GONE);
        tvForecastTitle.setText("Pronóstico de " + c.dias.size() + " días");
        String fuentes = c.tieneOpenWeather ? "Open-Meteo + OpenWeather" : "Open-Meteo";
        tvForecastCity.setText(c.ubicacion.nombre + "  ·  " + fuentes);
        adapter.setDatos(c);
    }
}
