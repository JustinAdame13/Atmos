package com.justin.atmos.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.justin.atmos.Adapter.FavoritesAdapter;
import com.justin.atmos.R;
import com.justin.atmos.ViewModel.WeatherViewModel;
import com.justin.atmos.model.ClimaActual;
import com.justin.atmos.storage.FavoritesManager;

import java.util.List;

public class FavoritesFragment extends Fragment {

    private FavoritesManager favoritesManager;
    private FavoritesAdapter adapter;
    private WeatherViewModel viewModel;
    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView  = view.findViewById(R.id.rv_favorites);
        layoutEmpty   = view.findViewById(R.id.layout_empty_favorites);

        favoritesManager = new FavoritesManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        cargarFavoritos();
    }

    // Se llama también en onResume para reflejar cambios hechos desde Search
    @Override
    public void onResume() {
        super.onResume();
        cargarFavoritos();
    }

    private void cargarFavoritos() {
        List<ClimaActual> lista = favoritesManager.obtenerFavoritos();

        if (lista.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        adapter = new FavoritesAdapter(lista, new FavoritesAdapter.OnFavoriteListener() {

            @Override
            public void onDelete(ClimaActual clima) {
                favoritesManager.eliminarFavorito(clima.cityName);
                cargarFavoritos(); // recarga la lista completa
            }

            @Override
            public void onCardClick(ClimaActual clima) {
                // Re-consulta la API con el nombre guardado y navega a Home
                viewModel.buscarClima(clima.cityName);
                BottomNavigationView nav =
                        requireActivity().findViewById(R.id.bottom_navigation);
                nav.setSelectedItemId(R.id.nav_home);
            }
        });

        recyclerView.setAdapter(adapter);
    }
}