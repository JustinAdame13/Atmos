package com.justin.atmos.ui;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.justin.atmos.R;
import com.justin.atmos.Repository.WeatherRepository;
import com.justin.atmos.ViewModel.WeatherViewModel;
import com.justin.atmos.notifications.Notificaciones;
import com.justin.atmos.notifications.ProgramadorTareas;
import com.justin.atmos.storage.Preferencias;
import com.justin.atmos.widget.WidgetChico;
import com.justin.atmos.widget.WidgetGrande;
import com.justin.atmos.widget.WidgetMediano;
import com.justin.atmos.widget.WidgetUpdater;

import java.util.Locale;

/**
 * Pantalla de ajustes: unidades, fuentes, notificaciones y widgets.
 * Cada cambio se guarda al instante en Preferencias.
 */
public class AjustesFragment extends Fragment {

    private WeatherViewModel viewModel;
    private MaterialSwitch swNotifDiaria, swNotifLluvia;
    private TextView tvHora;

    /** Qué switch pidió el permiso de notificaciones (para activarlo si lo aceptan). */
    private MaterialSwitch switchPendiente;

    private final ActivityResultLauncher<String> permisoNotif =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), aceptado -> {
                if (switchPendiente == null) return;
                if (aceptado) {
                    switchPendiente.setChecked(true); // dispara el listener y guarda
                } else {
                    Toast.makeText(requireContext(), "Sin permiso no podemos enviarte notificaciones", Toast.LENGTH_LONG).show();
                }
                switchPendiente = null;
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ajustes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        view.findViewById(R.id.btn_volver).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        configurarUnidades(view);
        configurarFuentes(view);
        configurarNotificaciones(view);
        configurarWidgets(view);
    }

    // ── unidades ─────────────────────────────────────────────────────────────

    private void configurarUnidades(View view) {
        MaterialButtonToggleGroup grupoTemp = view.findViewById(R.id.grupo_temp);
        grupoTemp.check(Preferencias.usaFahrenheit(requireContext()) ? R.id.btn_fahrenheit : R.id.btn_celsius);
        grupoTemp.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            Preferencias.setFahrenheit(requireContext(), id == R.id.btn_fahrenheit);
            alCambiarUnidades();
        });

        MaterialButtonToggleGroup grupoViento = view.findViewById(R.id.grupo_viento);
        String viento = Preferencias.unidadViento(requireContext());
        grupoViento.check("mph".equals(viento) ? R.id.btn_mph : "ms".equals(viento) ? R.id.btn_ms : R.id.btn_kmh);
        grupoViento.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            Preferencias.setUnidadViento(requireContext(), id == R.id.btn_mph ? "mph" : id == R.id.btn_ms ? "ms" : "kmh");
            alCambiarUnidades();
        });

        MaterialSwitch sw24 = view.findViewById(R.id.sw_24h);
        sw24.setChecked(Preferencias.formato24h(requireContext()));
        sw24.setOnCheckedChangeListener((b, checked) -> {
            Preferencias.setFormato24h(requireContext(), checked);
            actualizarTextoHora();
            alCambiarUnidades();
        });
    }

    /** Vuelve a pintar todo con las nuevas unidades (no hace falta descargar de nuevo). */
    private void alCambiarUnidades() {
        viewModel.clima.setValue(viewModel.clima.getValue());
        WidgetUpdater.actualizarTodos(requireContext());
    }

    // ── fuentes ──────────────────────────────────────────────────────────────

    private void configurarFuentes(View view) {
        MaterialSwitch sw = view.findViewById(R.id.sw_openweather);
        TextView estado = view.findViewById(R.id.tv_estado_fuentes);

        if (!WeatherRepository.tieneApiKey()) {
            sw.setChecked(false);
            sw.setEnabled(false);
            estado.setText("No se encontró OPENWEATHER_API_KEY en local.properties. "
                    + "Open-Meteo sigue funcionando sin llave.");
            return;
        }
        sw.setChecked(Preferencias.usarOpenWeather(requireContext()));
        estado.setText("Open-Meteo siempre está activo. Con OpenWeather se comparan ambas fuentes y se muestra el rango probable.");
        sw.setOnCheckedChangeListener((b, checked) -> {
            Preferencias.setUsarOpenWeather(requireContext(), checked);
            viewModel.refrescar();
        });
    }

    // ── notificaciones ───────────────────────────────────────────────────────

    private void configurarNotificaciones(View view) {
        swNotifDiaria = view.findViewById(R.id.sw_notif_diaria);
        swNotifLluvia = view.findViewById(R.id.sw_notif_lluvia);
        tvHora = view.findViewById(R.id.tv_hora_notif);

        swNotifDiaria.setChecked(Preferencias.notifDiaria(requireContext()) && Notificaciones.tienePermiso(requireContext()));
        swNotifLluvia.setChecked(Preferencias.notifLluvia(requireContext()) && Notificaciones.tienePermiso(requireContext()));
        actualizarTextoHora();

        swNotifDiaria.setOnCheckedChangeListener((b, checked) -> {
            if (checked && pedirPermisoSiFalta(swNotifDiaria)) return;
            Preferencias.setNotifDiaria(requireContext(), checked);
            ProgramadorTareas.programarTodo(requireContext());
            if (checked) Toast.makeText(requireContext(), "Recibirás el resumen a las " + tvHora.getText(), Toast.LENGTH_SHORT).show();
        });

        swNotifLluvia.setOnCheckedChangeListener((b, checked) -> {
            if (checked && pedirPermisoSiFalta(swNotifLluvia)) return;
            Preferencias.setNotifLluvia(requireContext(), checked);
            ProgramadorTareas.programarTodo(requireContext());
        });

        view.findViewById(R.id.fila_hora).setOnClickListener(v -> elegirHora());
    }

    /** En Android 13+ hay que pedir permiso para notificar. Devuelve true si lo tuvo que pedir. */
    private boolean pedirPermisoSiFalta(MaterialSwitch sw) {
        if (Build.VERSION.SDK_INT >= 33 && !Notificaciones.tienePermiso(requireContext())) {
            sw.setChecked(false);
            switchPendiente = sw;
            permisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS);
            return true;
        }
        return false;
    }

    private void elegirHora() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(Preferencias.formato24h(requireContext()) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(Preferencias.horaNotif(requireContext()))
                .setMinute(Preferencias.minutoNotif(requireContext()))
                .setTitleText("Hora del resumen diario")
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            Preferencias.setHoraNotif(requireContext(), picker.getHour());
            Preferencias.setMinutoNotif(requireContext(), picker.getMinute());
            actualizarTextoHora();
            if (Preferencias.notifDiaria(requireContext())) {
                ProgramadorTareas.programarResumen(requireContext(), true);
            }
        });
        picker.show(getParentFragmentManager(), "hora");
    }

    private void actualizarTextoHora() {
        int h = Preferencias.horaNotif(requireContext());
        int m = Preferencias.minutoNotif(requireContext());
        if (Preferencias.formato24h(requireContext())) {
            tvHora.setText(String.format(Locale.US, "%02d:%02d", h, m));
        } else {
            int h12 = h % 12 == 0 ? 12 : h % 12;
            tvHora.setText(String.format(Locale.US, "%d:%02d %s", h12, m, h < 12 ? "a. m." : "p. m."));
        }
    }

    // ── widgets ──────────────────────────────────────────────────────────────

    private void configurarWidgets(View view) {
        view.findViewById(R.id.btn_widget_chico).setOnClickListener(v -> fijarWidget(WidgetChico.class));
        view.findViewById(R.id.btn_widget_mediano).setOnClickListener(v -> fijarWidget(WidgetMediano.class));
        view.findViewById(R.id.btn_widget_grande).setOnClickListener(v -> fijarWidget(WidgetGrande.class));
    }

    /** Pide al launcher que agregue el widget directamente (Android 8+). */
    private void fijarWidget(Class<?> clase) {
        AppWidgetManager m = AppWidgetManager.getInstance(requireContext());
        if (m.isRequestPinAppWidgetSupported()) {
            m.requestPinAppWidget(new ComponentName(requireContext(), clase), null, null);
        } else {
            Toast.makeText(requireContext(),
                    "Tu launcher no permite agregarlo desde aquí. Mantén presionada la pantalla de inicio → Widgets → Atmos.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
