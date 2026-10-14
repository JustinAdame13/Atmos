package com.justin.atmos.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.justin.atmos.Adapter.SearchAdapter;
import com.justin.atmos.R;
import com.justin.atmos.ViewModel.WeatherViewModel;
import com.justin.atmos.model.Ubicacion;
import com.justin.atmos.storage.FavoritesManager;

import java.util.List;

/**
 * Buscador con autocompletado: mientras escribes, aparecen ciudades del mundo
 * (Open-Meteo Geocoding). Al tocar una, se abre en Inicio.
 */
public class SearchFragment extends Fragment {

    private WeatherViewModel viewModel;
    private SearchAdapter adapter;
    private EditText etBuscar;
    private TextView tvHint;
    private LinearProgressIndicator progreso;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        etBuscar = view.findViewById(R.id.et_buscar);
        tvHint = view.findViewById(R.id.tv_search_hint);
        progreso = view.findViewById(R.id.progress_busqueda);

        RecyclerView rv = view.findViewById(R.id.rv_search);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SearchAdapter(new FavoritesManager(requireContext()), new SearchAdapter.Listener() {
            @Override
            public void alSeleccionar(Ubicacion ubicacion) {
                abrirEnInicio(ubicacion);
            }

            @Override
            public void alCambiarFavorito(Ubicacion ubicacion, boolean ahoraEsFavorito) {
                Toast.makeText(requireContext(),
                        ahoraEsFavorito ? ubicacion.nombre + " agregado a favoritos ⭐" : "Quitado de favoritos",
                        Toast.LENGTH_SHORT).show();
            }
        });
        rv.setAdapter(adapter);

        // Cada tecla → el ViewModel espera 350 ms y busca (debounce)
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.buscar(s.toString());
            }
        });
        etBuscar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                ocultarTeclado();
                List<Ubicacion> res = viewModel.resultadosBusqueda.getValue();
                if (res != null && !res.isEmpty()) abrirEnInicio(res.get(0));
                return true;
            }
            return false;
        });

        view.findViewById(R.id.btn_mi_ubicacion).setOnClickListener(v -> {
            Ubicacion gps = com.justin.atmos.storage.Preferencias.ultimoGps(requireContext());
            if (gps != null) {
                abrirEnInicio(gps);
            } else {
                // Sin GPS previo: Inicio pedirá el permiso / ubicación
                ((MainActivity) requireActivity()).irAPestana(R.id.nav_home);
            }
        });

        viewModel.resultadosBusqueda.observe(getViewLifecycleOwner(), lista -> {
            adapter.setLista(lista);
            String texto = etBuscar.getText() != null ? etBuscar.getText().toString().trim() : "";
            if (lista.isEmpty() && texto.length() >= 2 && !Boolean.TRUE.equals(viewModel.buscando.getValue())) {
                tvHint.setVisibility(View.VISIBLE);
                tvHint.setText("No encontramos \"" + texto + "\".\nRevisa la ortografía o prueba con otra ciudad.");
            } else if (lista.isEmpty()) {
                tvHint.setVisibility(View.VISIBLE);
                tvHint.setText("Escribe al menos 2 letras.\nPuedes buscar cualquier ciudad del mundo 🌎");
            } else {
                tvHint.setVisibility(View.GONE);
            }
        });
        viewModel.buscando.observe(getViewLifecycleOwner(), b ->
                progreso.setVisibility(Boolean.TRUE.equals(b) ? View.VISIBLE : View.INVISIBLE));
    }

    private void abrirEnInicio(Ubicacion ubicacion) {
        ocultarTeclado();
        viewModel.cargarUbicacion(ubicacion, false);
        ((MainActivity) requireActivity()).irAPestana(R.id.nav_home);
    }

    private void ocultarTeclado() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etBuscar.getWindowToken(), 0);
    }
}
