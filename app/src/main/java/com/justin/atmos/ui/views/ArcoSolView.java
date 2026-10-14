package com.justin.atmos.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Semicírculo que muestra el recorrido del sol entre el amanecer y el atardecer.
 * El sol se dibuja en la posición correspondiente a la hora actual.
 */
public class ArcoSolView extends View {

    private long amanecer, atardecer;

    private final Paint pArco = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pProgreso = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pHorizonte = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSol = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pHalo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ovalo = new RectF();

    public ArcoSolView(Context context) { this(context, null); }

    public ArcoSolView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        pArco.setStyle(Paint.Style.STROKE);
        pArco.setStrokeWidth(dp(2));
        pArco.setColor(0x55FFFFFF);
        pArco.setPathEffect(new DashPathEffect(new float[]{dp(4), dp(4)}, 0));

        pProgreso.setStyle(Paint.Style.STROKE);
        pProgreso.setStrokeWidth(dp(3));
        pProgreso.setStrokeCap(Paint.Cap.ROUND);
        pProgreso.setColor(0xFFFFD54F);

        pHorizonte.setStrokeWidth(dp(1));
        pHorizonte.setColor(0x40FFFFFF);

        pSol.setColor(0xFFFFD54F);
        pHalo.setColor(0x44FFD54F);
    }

    public void setHorario(long amanecer, long atardecer) {
        this.amanecer = amanecer;
        this.atardecer = atardecer;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ancho = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        setMeasuredDimension(ancho, (int) (ancho / 2f + dp(10)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float margen = dp(10);
        float radio = (getWidth() - 2 * margen) / 2f;
        float cx = getWidth() / 2f;
        float cy = radio + margen;

        ovalo.set(cx - radio, cy - radio, cx + radio, cy + radio);
        canvas.drawArc(ovalo, 180, 180, false, pArco);
        canvas.drawLine(0, cy, getWidth(), cy, pHorizonte);

        if (amanecer <= 0 || atardecer <= amanecer) return;

        long ahora = System.currentTimeMillis() / 1000L;
        float f = (float) (ahora - amanecer) / (atardecer - amanecer);
        if (f < 0 || f > 1) return; // es de noche: no dibujamos el sol

        canvas.drawArc(ovalo, 180, 180 * f, false, pProgreso);
        double angulo = Math.toRadians(180 + 180 * f);
        float sx = (float) (cx + radio * Math.cos(angulo));
        float sy = (float) (cy + radio * Math.sin(angulo));
        canvas.drawCircle(sx, sy, dp(10), pHalo);
        canvas.drawCircle(sx, sy, dp(6), pSol);
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
