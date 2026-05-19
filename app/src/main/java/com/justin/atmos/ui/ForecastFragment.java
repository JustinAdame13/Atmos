package com.justin.atmos.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justin.atmos.R;
import com.justin.atmos.Adapter.ForecastAdapter;
import com.justin.atmos.ViewModel.WeatherViewModel;

public class ForecastFragment extends Fragment {

    private WeatherViewModel viewModel;
    private ForecastAdapter adapter;
    private TextView tvForecastCity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forecast, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── conectar vistas ──────────────────────────────────
        tvForecastCity = view.findViewById(R.id.tv_forecast_city);
        RecyclerView recyclerView = view.findViewById(R.id.rv_forecast);

        // ── configurar RecyclerView ──────────────────────────
        adapter = new ForecastAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // ── obtener ViewModel compartido ─────────────────────
        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // ── observar datos ───────────────────────────────────
        viewModel.pronostico.observe(getViewLifecycleOwner(), pronostico -> {
            tvForecastCity.setText(pronostico.city.name);
            adapter.setDatos(pronostico);
        });

        viewModel.error.observe(getViewLifecycleOwner(), error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show());
    }
}