package com.justin.atmos.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.justin.atmos.Adapter.FavoritesAdapter;
import com.justin.atmos.R;
import com.justin.atmos.ViewModel.WeatherViewModel;
import com.justin.atmos.model.Ubicacion;
import com.justin.atmos.storage.FavoritesManager;

import java.util.List;

/**
 * Lista de ciudades favoritas con su clima actual.
 * - Tocar: abre la ciudad en Inicio.
 * - Deslizar a un lado: elimina (con opción de "Deshacer").
 */
public class FavoritesFragment extends Fragment {

    private WeatherViewModel viewModel;
    private FavoritesManager favoritesManager;
    private FavoritesAdapter adapter;
    private View layoutVacio;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        favoritesManager = new FavoritesManager(requireContext());
        layoutVacio = view.findViewById(R.id.layout_vacio);

        RecyclerView rv = view.findViewById(R.id.rv_favorites);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FavoritesAdapter(ubicacion -> {
            viewModel.cargarUbicacion(ubicacion, false);
            ((MainActivity) requireActivity()).irAPestana(R.id.nav_home);
        });
        rv.setAdapter(adapter);

        // Deslizar para eliminar
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder a, @NonNull RecyclerView.ViewHolder b) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                Ubicacion borrada = adapter.getItem(pos);
                favoritesManager.eliminarFavorito(borrada);
                adapter.eliminar(pos);
                actualizarVacio();
                Snackbar.make(view, borrada.nombre + " eliminado", Snackbar.LENGTH_LONG)
                        .setAnchorView(requireActivity().findViewById(R.id.bottom_navigation))
                        .setAction("Deshacer", v -> {
                            favoritesManager.insertarFavorito(pos, borrada);
                            adapter.insertar(pos, borrada);
                            actualizarVacio();
                        })
                        .show();
            }
        }).attachToRecyclerView(rv);

        view.findViewById(R.id.btn_fav_buscar).setOnClickListener(v ->
                ((MainActivity) requireActivity()).irAPestana(R.id.nav_search));

        viewModel.resumenes.observe(getViewLifecycleOwner(), adapter::setResumenes);
    }

    @Override
    public void onResume() {
        super.onResume();
        List<Ubicacion> lista = favoritesManager.obtenerFavoritos();
        adapter.setLista(lista);
        actualizarVacio();
        viewModel.cargarResumenes(lista);
    }

    private void actualizarVacio() {
        layoutVacio.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}
