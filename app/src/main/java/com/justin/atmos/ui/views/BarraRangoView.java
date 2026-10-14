package com.justin.atmos.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.justin.atmos.utils.Formato;

/**
 * Barra horizontal de mínima → máxima (estilo apps de clima de iOS/Pixel).
 *
 * - El riel gris representa el rango de TODA la semana.
 * - La parte de color es el rango de ese día (promedio de las dos fuentes).
 * - Las partes tenues a los lados muestran cuánto difieren las fuentes
 *   (la "incertidumbre" entre OpenWeather y Open-Meteo).
 * - Si es hoy, un punto blanco marca la temperatura actual.
 *
 * Todos los valores se pasan en °C; solo sirven para calcular posiciones y colores.
 */
public class BarraRangoView extends View {

    private double minGlobal = 0, maxGlobal = 1;
    private double min = 0, max = 1;
    private double minBaja = 0, maxAlta = 1;
    private Double actual = null;

    private final Paint pRiel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pRango = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pIncertidumbre = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPunto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public BarraRangoView(Context context) { this(context, null); }

    public BarraRangoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        pRiel.setColor(0x26FFFFFF);
        pIncertidumbre.setColor(0x55FFFFFF);
        pPunto.setColor(0xFFFFFFFF);
        pPunto.setShadowLayer(dp(2), 0, 0, 0x66000000);
    }

    public void setRango(double minGlobal, double maxGlobal,
                         double min, double max,
                         double minBaja, double maxAlta,
                         @Nullable Double actual) {
        this.minGlobal = minGlobal;
        this.maxGlobal = Math.max(maxGlobal, minGlobal + 1);
        this.min = min;
        this.max = max;
        this.minBaja = minBaja;
        this.maxAlta = maxAlta;
        this.actual = actual;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int alto = (int) dp(12);
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), alto);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float alto = dp(6);
        float cy = getHeight() / 2f;
        float izq = getPaddingLeft() + dp(6);
        float der = getWidth() - getPaddingRight() - dp(6);
        float radio = alto / 2;

        rect.set(izq, cy - alto / 2, der, cy + alto / 2);
        canvas.drawRoundRect(rect, radio, radio, pRiel);

        // Incertidumbre entre fuentes
        float xb = x(minBaja, izq, der), xa = x(maxAlta, izq, der);
        rect.set(xb, cy - alto / 2, xa, cy + alto / 2);
        canvas.drawRoundRect(rect, radio, radio, pIncertidumbre);

        // Rango del día
        float x1 = x(min, izq, der), x2 = x(max, izq, der);
        if (x2 - x1 < alto) x2 = x1 + alto;
        pRango.setShader(new LinearGradient(x1, 0, x2, 0,
                Formato.colorTemperatura(min), Formato.colorTemperatura(max), Shader.TileMode.CLAMP));
        rect.set(x1, cy - alto / 2, x2, cy + alto / 2);
        canvas.drawRoundRect(rect, radio, radio, pRango);

        if (actual != null) {
            canvas.drawCircle(x(actual, izq, der), cy, dp(4.5f), pPunto);
        }
    }

    private float x(double v, float izq, float der) {
        double f = (v - minGlobal) / (maxGlobal - minGlobal);
        f = Math.max(0, Math.min(1, f));
        return (float) (izq + f * (der - izq));
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
