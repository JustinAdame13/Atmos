package com.justin.atmos.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.justin.atmos.Adapter.FavoritesAdapter;
import com.justin.atmos.API.RetrofitClient;
import com.justin.atmos.API.WeatherService;
import com.justin.atmos.BuildConfig;
import com.justin.atmos.R;
import com.justin.atmos.ViewModel.WeatherViewModel;
import com.justin.atmos.model.ClimaActual;
import com.justin.atmos.storage.FavoritesManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.lifecycle.ViewModelProvider;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesFragment extends Fragment {

    private FavoritesManager favoritesManager;
    private FavoritesAdapter adapter;
    private WeatherViewModel viewModel;
    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private ProgressBar progressLoading;

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

        recyclerView    = view.findViewById(R.id.rv_favorites);
        layoutEmpty     = view.findViewById(R.id.layout_empty_favorites);
        progressLoading = view.findViewById(R.id.progress_loading);

        favoritesManager = new FavoritesManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        cargarFavoritos();
    }

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

        // Mostrar datos viejos inmediatamente
        recyclerView.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        mostrarLista(lista);

        // Iniciar actualización en background
        actualizarFavoritos(lista);
    }

    private void actualizarFavoritos(List<ClimaActual> lista) {
        progressLoading.setVisibility(View.VISIBLE);

        WeatherService service = RetrofitClient.getInstance()
                .createService(WeatherService.class);

        // Contador para saber cuándo terminaron todas las llamadas
        AtomicInteger pendientes = new AtomicInteger(lista.size());

        for (ClimaActual climaViejo : lista) {
            service.getClimaActual(
                    climaViejo.cityName,
                    BuildConfig.OPENWEATHER_API_KEY,
                    "metric",
                    "es"
            ).enqueue(new Callback<ClimaActual>() {

                @Override
                public void onResponse(Call<ClimaActual> call,
                                       Response<ClimaActual> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Actualizar en SharedPreferences
                        favoritesManager.guardarFavorito(response.body());
                    } else {
                        // Mantener datos viejos, mostrar error pequeño
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(),
                                            "No se pudo actualizar " + climaViejo.cityName,
                                            Toast.LENGTH_SHORT).show());
                        }
                    }
                    verificarSiTerminaron(pendientes);
                }

                @Override
                public void onFailure(Call<ClimaActual> call, Throwable t) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Sin conexión: " + climaViejo.cityName,
                                        Toast.LENGTH_SHORT).show());
                    }
                    verificarSiTerminaron(pendientes);
                }
            });
        }
    }

    private void verificarSiTerminaron(AtomicInteger pendientes) {
        if (pendientes.decrementAndGet() == 0) {
            // Todas las llamadas terminaron
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    // Recargar lista con datos frescos
                    List<ClimaActual> listaActualizada =
                            favoritesManager.obtenerFavoritos();
                    mostrarLista(listaActualizada);
                });
            }
        }
    }

    private void mostrarLista(List<ClimaActual> lista) {
        adapter = new FavoritesAdapter(lista, new FavoritesAdapter.OnFavoriteListener() {

            @Override
            public void onDelete(ClimaActual clima) {
                favoritesManager.eliminarFavorito(clima.cityName);
                cargarFavoritos();
            }

            @Override
            public void onCardClick(ClimaActual clima) {
                viewModel.cargarCiudadEnHome(clima);
                BottomNavigationView nav =
                        requireActivity().findViewById(R.id.bottom_navigation);
                nav.setSelectedItemId(R.id.nav_home);
            }
        });

        recyclerView.setAdapter(adapter);
    }
}