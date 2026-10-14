package com.justin.atmos.ui.views;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

/**
 * Fondo degradado de toda la app que cambia según el clima y si es de día o de noche.
 * Al cambiar de condición, los colores se mezclan suavemente (animación de 700 ms).
 */
public class FondoDinamico {

    private final GradientDrawable degradado;
    private int[] coloresActuales = {0xFF0F1923, 0xFF1E2D3D};
    private ValueAnimator animador;

    public FondoDinamico(View vista) {
        degradado = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, coloresActuales);
        vista.setBackground(degradado);
    }

    public void cambiarA(int[] nuevos) {
        if (nuevos[0] == coloresActuales[0] && nuevos[1] == coloresActuales[1]) return;
        if (animador != null) animador.cancel();

        final int[] desde = coloresActuales.clone();
        final ArgbEvaluator mezcla = new ArgbEvaluator();
        animador = ValueAnimator.ofFloat(0f, 1f);
        animador.setDuration(700);
        animador.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            int arriba = (int) mezcla.evaluate(f, desde[0], nuevos[0]);
            int abajo = (int) mezcla.evaluate(f, desde[1], nuevos[1]);
            coloresActuales = new int[]{arriba, abajo};
            degradado.setColors(coloresActuales);
        });
        animador.start();
    }
}
