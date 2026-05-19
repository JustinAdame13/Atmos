package com.justin.atmos.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.justin.atmos.R;
import com.justin.atmos.ViewModel.WeatherViewModel;

public class SearchFragment extends Fragment {

    private WeatherViewModel viewModel;
    private EditText etSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.et_search);
        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // ── buscar cuando presiona el botón de buscar del teclado ──
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String ciudad = etSearch.getText().toString().trim();
                if (!ciudad.isEmpty()) {
                    buscarCiudad(ciudad);
                } else {
                    Toast.makeText(requireContext(),
                            "Escribe una ciudad", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // ── observar si hay error ────────────────────────────────
        viewModel.error.observe(getViewLifecycleOwner(), error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show());

        // ── observar cuando lleguen datos exitosos ───────────────
        viewModel.climaActual.observe(getViewLifecycleOwner(), clima -> {
            if (clima != null && etSearch.getText().length() > 0) {
                navegarAHome();
            }
        });
    }

    private void buscarCiudad(String ciudad) {
        viewModel.buscarClima(ciudad);
    }

    private void navegarAHome() {
        requireActivity().runOnUiThread(() -> {
            com.google.android.material.bottomnavigation.BottomNavigationView nav =
                    requireActivity().findViewById(R.id.bottom_navigation);
            nav.setSelectedItemId(R.id.nav_home);
        });
    }
}