package com.justin.atmos.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justin.atmos.Adapter.SearchAdapter;
import com.justin.atmos.R;
import com.justin.atmos.ViewModel.WeatherViewModel;

public class SearchFragment extends Fragment {

    private WeatherViewModel viewModel;
    private EditText etSearch;
    private SearchAdapter adapter;
    private LinearLayout layoutEmpty;
    private RecyclerView rvResults;

    // ── bandera para saber si la búsqueda fue iniciada en este fragment ──────
    private boolean busquedaIniciada = false;

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

        // ── conectar vistas ──────────────────────────────────────────────────
        etSearch    = view.findViewById(R.id.et_search);
        layoutEmpty = view.findViewById(R.id.layout_empty_search);
        rvResults   = view.findViewById(R.id.rv_search_results);

        // ── configurar RecyclerView ──────────────────────────────────────────
        adapter = new SearchAdapter();
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(adapter);

        // ── al tocar la tarjeta navega a Home ────────────────────────────────
        adapter.setOnItemClickListener(clima -> {
            navegarAHome();
        });

        // ── al tocar favorito ────────────────────────────────────────────────
        adapter.setOnFavoriteClickListener(clima -> {
            // TODO: reemplazar por FavoritesManager cuando esté disponible (Integrante B)
            Toast.makeText(requireContext(),
                    clima.cityName + " guardado en favoritos",
                    Toast.LENGTH_SHORT).show();
        });

        // ── obtener ViewModel compartido ─────────────────────────────────────
        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // ── buscar al presionar la tecla de búsqueda ─────────────────────────
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String ciudad = etSearch.getText().toString().trim();
                if (!ciudad.isEmpty()) {
                    busquedaIniciada = true;
                    buscarCiudad(ciudad);
                } else {
                    Toast.makeText(requireContext(),
                            "Escribe una ciudad", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // ── observar resultados del clima ────────────────────────────────────
        viewModel.climaActual.observe(getViewLifecycleOwner(), clima -> {
            if (clima != null && busquedaIniciada) {
                adapter.setClima(clima);
                mostrarResultados(true);
                busquedaIniciada = false;
            }
        });

        // ── observar errores ─────────────────────────────────────────────────
        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (busquedaIniciada) {
                mostrarResultados(false);
                busquedaIniciada = false;
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // ── si ya hay datos previos mostrarlos sin navegar ───────────────────
        if (viewModel.climaActual.getValue() != null) {
            adapter.setClima(viewModel.climaActual.getValue());
            mostrarResultados(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        busquedaIniciada = false;
    }

    // ── métodos privados ─────────────────────────────────────────────────────

    private void buscarCiudad(String ciudad) {
        adapter.setClima(null);
        mostrarResultados(false);
        // La API busca por nombre parcial automáticamente,
        // por ejemplo "Tor" puede encontrar "Torreón"
        viewModel.buscarClima(ciudad);
    }

    private void mostrarResultados(boolean hayResultados) {
        if (hayResultados) {
            rvResults.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        } else {
            rvResults.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void navegarAHome() {
        requireActivity().runOnUiThread(() -> {
            com.google.android.material.bottomnavigation.BottomNavigationView nav =
                    requireActivity().findViewById(R.id.bottom_navigation);
            nav.setSelectedItemId(R.id.nav_home);
        });
    }
}
