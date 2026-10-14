package com.justin.atmos.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.justin.atmos.R;
import com.justin.atmos.ViewModel.WeatherViewModel;
import com.justin.atmos.notifications.Notificaciones;
import com.justin.atmos.notifications.ProgramadorTareas;
import com.justin.atmos.ui.views.FondoDinamico;

/**
 * Actividad única de la app. Contiene:
 *  - el fondo degradado dinámico (cambia según el clima de Inicio),
 *  - el contenedor de fragments,
 *  - la barra de navegación inferior.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG_AJUSTES = "ajustes";

    private BottomNavigationView bottomNav;
    private FondoDinamico fondo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this); // dibujar detrás de la barra de estado y de navegación
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View raiz = findViewById(R.id.main);
        View contenedor = findViewById(R.id.fragment_container);
        bottomNav = findViewById(R.id.bottom_navigation);
        fondo = new FondoDinamico(raiz);

        // Márgenes para no quedar debajo de las barras del sistema
        int margenNav = (int) (12 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            contenedor.setPadding(barras.left, barras.top, barras.right, 0);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
            lp.bottomMargin = barras.bottom + margenNav;
            bottomNav.setLayoutParams(lp);
            return insets;
        });
        // La barra flotante no necesita su propio padding inferior
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> WindowInsetsCompat.CONSUMED);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) selected = new HomeFragment();
            else if (id == R.id.nav_forecast) selected = new ForecastFragment();
            else if (id == R.id.nav_search) selected = new SearchFragment();
            else if (id == R.id.nav_favorites) selected = new FavoritesFragment();

            if (selected != null) {
                // Si Ajustes estaba abierto, lo cerramos
                getSupportFragmentManager().popBackStack(TAG_AJUSTES, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, selected)
                        .commit();
            }
            return true;
        });

        // Tocar la pestaña que ya está activa cierra Ajustes si estaba abierto
        bottomNav.setOnItemReselectedListener(item ->
                getSupportFragmentManager().popBackStack(TAG_AJUSTES, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE));

        // ── ViewModel compartido ─────────────────────────────────────────────
        WeatherViewModel viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        // El fondo sigue el clima de la ciudad mostrada en Inicio
        viewModel.clima.observe(this, clima -> {
            if (clima != null) fondo.cambiarA(clima.actual.condicion.colores(clima.actual.esDeDia));
        });

        // Los errores se muestran en un solo lugar
        viewModel.error.observe(this, evento -> {
            String msg = evento != null ? evento.obtenerSiNoManejado() : null;
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Pantalla inicial (solo la primera vez, no al girar la pantalla)
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        Notificaciones.crearCanales(this);
        ProgramadorTareas.programarTodo(this);
    }

    /** Cambia de pestaña desde un fragment (ej. "Ver pronóstico de 16 días"). */
    public void irAPestana(int idMenu) {
        bottomNav.setSelectedItemId(idMenu);
    }

    /** Abre Ajustes encima de la pestaña actual (con botón "atrás"). */
    public void abrirAjustes() {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, new AjustesFragment())
                .addToBackStack(TAG_AJUSTES)
                .commit();
    }
}
