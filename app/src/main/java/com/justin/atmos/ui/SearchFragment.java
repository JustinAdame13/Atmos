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

        // ── favorito: delegar en FavoritesManager (Integrante B) ────────────
        // Cuando el Integrante B entregue FavoritesManager, reemplazar el Toast
        // por: FavoritesManager.getInstance(requireContext()).guardarFavorito(clima)
        adapter.setOnFavoriteClickListener(clima -> {
            // TODO: descomentar cuando FavoritesManager esté disponible (Integrante B)
            // FavoritesManager.getInstance(requireContext()).guardarFavorito(clima);
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
                // Mostrar resultado en el RecyclerView
                adapter.setClima(clima);
                mostrarResultados(true);

                // Navegar a Home después de un momento para que el usuario
                // vea el resultado antes de ser redirigido
                // (Si prefieren ir directo, descomentar la siguiente línea
                //  y comentar el bloque de mostrarResultados + adapter.setClima)
                // navegarAHome();
            }
        });

        // ── observar errores ─────────────────────────────────────────────────
        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (busquedaIniciada) {
                mostrarResultados(false);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // ── si ya hay datos previos de una búsqueda anterior, mostrarlos ─────
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
        // Limpiar resultado anterior y mostrar estado vacío mientras carga
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
