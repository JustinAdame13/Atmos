package com.justin.atmos.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.justin.atmos.utils.Formato;

import java.util.ArrayList;
import java.util.List;

/**
 * Gráfica de temperatura por hora dibujada a mano con Canvas (sin librerías).
 *
 * Por cada hora dibuja (de arriba a abajo):
 *   temperatura · punto en la curva · ícono · % de lluvia · hora
 *
 * - Línea sólida de colores  = Open-Meteo (cada hora).
 * - Línea punteada celeste   = OpenWeather (cada 3 horas).
 *
 * Se pone dentro de un HorizontalScrollView porque es más ancha que la pantalla.
 * Es el equivalente a dibujar en un Canvas de JavaFX: medimos (onMeasure) y pintamos (onDraw).
 */
public class GraficaTemperaturaView extends View {

    /** Un punto de la gráfica (ya convertido a las unidades del usuario). */
    public static class Punto {
        public String hora;
        public double temp;            // valor a mostrar (°C o °F)
        public Double tempSecundaria;  // OpenWeather, puede ser null
        public double tempCelsius;     // para elegir el color
        public int probLluvia;
        public int icono;
    }

    private final List<Punto> puntos = new ArrayList<>();

    private final Paint pLinea = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pRelleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSecundaria = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPunto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTemp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pHora = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLluvia = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float anchoColumna;
    private final float altoTotal;
    private final float tamIcono;

    public GraficaTemperaturaView(Context context) { this(context, null); }

    public GraficaTemperaturaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        anchoColumna = dp(58);
        altoTotal = dp(196);
        tamIcono = dp(26);

        pLinea.setStyle(Paint.Style.STROKE);
        pLinea.setStrokeWidth(dp(3));
        pLinea.setStrokeCap(Paint.Cap.ROUND);

        pRelleno.setStyle(Paint.Style.FILL);

        pSecundaria.setStyle(Paint.Style.STROKE);
        pSecundaria.setStrokeWidth(dp(2));
        pSecundaria.setColor(0xFF80DEEA);
        pSecundaria.setPathEffect(new DashPathEffect(new float[]{dp(5), dp(4)}, 0));

        pPunto.setStyle(Paint.Style.FILL);

        pTemp.setColor(0xFFFFFFFF);
        pTemp.setTextSize(sp(15));
        pTemp.setTextAlign(Paint.Align.CENTER);
        pTemp.setFakeBoldText(true);

        pHora.setColor(0xC7FFFFFF);
        pHora.setTextSize(sp(12));
        pHora.setTextAlign(Paint.Align.CENTER);

        pLluvia.setColor(0xFF81D4FA);
        pLluvia.setTextSize(sp(11));
        pLluvia.setTextAlign(Paint.Align.CENTER);
    }

    public void setPuntos(List<Punto> nuevos) {
        puntos.clear();
        puntos.addAll(nuevos);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ancho = (int) (Math.max(1, puntos.size()) * anchoColumna + getPaddingLeft() + getPaddingRight());
        setMeasuredDimension(ancho, (int) altoTotal);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (puntos.isEmpty()) return;

        // Zona vertical de la curva
        float curvaArriba = dp(40);
        float curvaAbajo = dp(92);
        float yIcono = dp(104);
        float yLluvia = dp(150);
        float yHora = dp(178);

        // Rango de temperaturas (incluye la serie secundaria para que quepa)
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (Punto p : puntos) {
            min = Math.min(min, p.temp);
            max = Math.max(max, p.temp);
            if (p.tempSecundaria != null) {
                min = Math.min(min, p.tempSecundaria);
                max = Math.max(max, p.tempSecundaria);
            }
        }
        if (max - min < 1) { max += 0.5; min -= 0.5; }

        int n = puntos.size();
        float[] xs = new float[n];
        float[] ys = new float[n];
        int[] colores = new int[n];
        float[] posiciones = new float[n];
        float x0 = getPaddingLeft() + anchoColumna / 2f;
        for (int i = 0; i < n; i++) {
            xs[i] = x0 + i * anchoColumna;
            ys[i] = mapear(puntos.get(i).temp, min, max, curvaAbajo, curvaArriba);
            colores[i] = Formato.colorTemperatura(puntos.get(i).tempCelsius);
            posiciones[i] = n == 1 ? 0 : (float) i / (n - 1);
        }

        // 1) Curva suave (Bézier cúbica entre puntos)
        Path curva = curvaSuave(xs, ys);

        // 2) Relleno degradado bajo la curva
        Path relleno = new Path(curva);
        relleno.lineTo(xs[n - 1], curvaAbajo + dp(16));
        relleno.lineTo(xs[0], curvaAbajo + dp(16));
        relleno.close();
        pRelleno.setShader(new LinearGradient(0, curvaArriba, 0, curvaAbajo + dp(16),
                0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawPath(relleno, pRelleno);

        // 3) Línea con colores según la temperatura
        if (n > 1) {
            pLinea.setShader(new LinearGradient(xs[0], 0, xs[n - 1], 0, colores, posiciones, Shader.TileMode.CLAMP));
        } else {
            pLinea.setShader(null);
            pLinea.setColor(colores[0]);
        }
        canvas.drawPath(curva, pLinea);

        // 4) Serie secundaria (OpenWeather) punteada
        Path sec = new Path();
        boolean empezo = false;
        for (int i = 0; i < n; i++) {
            Double t = puntos.get(i).tempSecundaria;
            if (t == null) continue;
            float y = mapear(t, min, max, curvaAbajo, curvaArriba);
            if (!empezo) { sec.moveTo(xs[i], y); empezo = true; } else { sec.lineTo(xs[i], y); }
            pPunto.setColor(0xFF80DEEA);
            canvas.drawCircle(xs[i], y, dp(3), pPunto);
        }
        if (empezo) canvas.drawPath(sec, pSecundaria);

        // 5) Puntos, textos e íconos
        for (int i = 0; i < n; i++) {
            Punto p = puntos.get(i);
            pPunto.setColor(0xFFFFFFFF);
            canvas.drawCircle(xs[i], ys[i], dp(4), pPunto);
            pPunto.setColor(colores[i]);
            canvas.drawCircle(xs[i], ys[i], dp(2.5f), pPunto);

            canvas.drawText(Math.round(p.temp) + "°", xs[i], ys[i] - dp(11), pTemp);

            Drawable icono = ContextCompat.getDrawable(getContext(), p.icono);
            if (icono != null) {
                int l = (int) (xs[i] - tamIcono / 2);
                int t = (int) yIcono;
                icono.setBounds(l, t, (int) (l + tamIcono), (int) (t + tamIcono));
                icono.draw(canvas);
            }

            if (p.probLluvia >= 10) {
                canvas.drawText(p.probLluvia + "%", xs[i], yLluvia, pLluvia);
            }

            pHora.setFakeBoldText(i == 0);
            canvas.drawText(p.hora, xs[i], yHora, pHora);
        }
    }

    /** Curva que pasa por todos los puntos usando puntos de control a la mitad (spline sencilla). */
    private Path curvaSuave(float[] xs, float[] ys) {
        Path path = new Path();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) {
            float medioX = (xs[i - 1] + xs[i]) / 2f;
            path.cubicTo(medioX, ys[i - 1], medioX, ys[i], xs[i], ys[i]);
        }
        return path;
    }

    private static float mapear(double v, double min, double max, float yMin, float yMax) {
        return (float) (yMin + (v - min) / (max - min) * (yMax - yMin));
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }
}
