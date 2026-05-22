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

        // ── al tocar la tarjeta carga en Home y navega ───────────────────────
        adapter.setOnItemClickListener(clima -> {
            viewModel.cargarCiudadEnHome(clima);
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

        // ── observar resultados de búsqueda (no toca climaActual) ────────────
        viewModel.climaBusqueda.observe(getViewLifecycleOwner(), clima -> {
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

        // ── si ya hay datos previos de búsqueda mostrarlos ───────────────────
        if (viewModel.climaBusqueda.getValue() != null) {
            adapter.setClima(viewModel.climaBusqueda.getValue());
            mostrarResultados(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        busquedaIniciada = false;
    }

    // ── métodos privados ──────────────────────────────────────────────────────

    private void buscarCiudad(String ciudad) {
        adapter.setClima(null);
        mostrarResultados(false);
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