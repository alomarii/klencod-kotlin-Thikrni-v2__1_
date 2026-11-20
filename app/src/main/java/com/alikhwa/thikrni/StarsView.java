package com.alikhwa.thikrni;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

/** خلفية متحركة: تدرج زمردي + نجوم متلألئة + توهج ذهبي متحرك. */
public class StarsView extends View {
    private static final int N = 80;
    private final float[] x = new float[N], y = new float[N], r = new float[N],
            ph = new float[N], sp = new float[N], drift = new float[N];
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long t0 = System.nanoTime();
    private final Theme th;
    private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random shotRnd = new Random();
    private float nextShot = 2.5f, shotStart = -10f, shx, shy, shdx, shdy;
    private static final float SHOT_DUR = 1.0f;

    public StarsView(Context c, Theme theme) {
        super(c);
        th = theme;
        Random rnd = new Random(11);
        for (int i = 0; i < N; i++) {
            x[i] = rnd.nextFloat();
            y[i] = rnd.nextFloat();
            r[i] = 0.8f + rnd.nextFloat() * 2.2f;
            ph[i] = rnd.nextFloat() * 6.28f;
            sp[i] = 0.8f + rnd.nextFloat() * 2.2f;
            drift[i] = 0.004f + rnd.nextFloat() * 0.012f;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        bgPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{th.bgTop, th.bgMid, th.bgBottom},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        float t = (System.nanoTime() - t0) / 1e9f;
        cv.drawRect(0, 0, w, h, bgPaint);

        // توهج أخضر علوي
        float g1x = w * (0.5f + 0.18f * (float) Math.sin(t * 0.35f));
        glowPaint.setShader(new RadialGradient(g1x, h * 0.10f, w * 0.95f,
                Theme.a(th.glowA, 0x44), Theme.a(th.glowA, 0x00), Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glowPaint);

        // توهج ذهبي متحرك
        float g2x = w * (0.5f + 0.40f * (float) Math.sin(t * 0.22f + 1.3f));
        float g2y = h * (0.62f + 0.12f * (float) Math.cos(t * 0.27f));
        glowPaint.setShader(new RadialGradient(g2x, g2y, w * 0.75f,
                Theme.a(th.glowB, 0x22), Theme.a(th.glowB, 0x00), Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glowPaint);

        // شريط شفقي بطيء
        float bx = w * (0.5f + 0.5f * (float) Math.sin(t * 0.13f + 0.6f));
        glowPaint.setShader(new RadialGradient(bx, h * 0.34f, w * 0.55f,
                Theme.a(th.accent, 0x14), Theme.a(th.accent, 0x00), Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glowPaint);

        // النجوم
        for (int i = 0; i < N; i++) {
            float yy = ((y[i] - t * drift[i]) % 1f + 1f) % 1f;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * sp[i] + ph[i]);
            int a = (int) (50 + 205 * tw);
            starPaint.setColor((a << 24) | 0xFFF1C6);
            float px = x[i] * w, py = yy * h;
            float rad = r[i] * (0.7f + 0.5f * tw) * getResources().getDisplayMetrics().density * 0.6f;
            cv.drawCircle(px, py, rad, starPaint);
            if (r[i] > 2.4f) {
                starPaint.setStrokeWidth(1.2f);
                float L = rad * 3.2f * tw;
                cv.drawLine(px - L, py, px + L, py, starPaint);
                cv.drawLine(px, py - L, px, py + L, starPaint);
            }
        }
        drawShootingStar(cv, t, w, h);
        postInvalidateOnAnimation();
    }

    /** شهاب يعبر الشاشة كل بضع ثوانٍ. */
    private void drawShootingStar(Canvas cv, float t, int w, int h) {
        float dens = getResources().getDisplayMetrics().density;
        if (t >= nextShot && shotStart < 0) {
            shotStart = t;
            shx = w * (0.35f + 0.6f * shotRnd.nextFloat());
            shy = h * (0.04f + 0.25f * shotRnd.nextFloat());
            float ang = (float) Math.toRadians(145 + shotRnd.nextInt(25)); // يسار-أسفل
            float speed = w * 0.95f;
            shdx = (float) Math.cos(ang) * speed;
            shdy = (float) Math.sin(ang) * speed;
        }
        if (shotStart < 0) return;
        float k = (t - shotStart) / SHOT_DUR;
        if (k >= 1f) {
            shotStart = -10f;
            nextShot = t + 5f + shotRnd.nextFloat() * 8f;
            return;
        }
        float hx = shx + shdx * k * SHOT_DUR, hy = shy + shdy * k * SHOT_DUR;
        float fade = (float) Math.sin(Math.PI * k);
        int segs = 14;
        float len = 0.16f;
        for (int i = 0; i < segs; i++) {
            float f0 = i / (float) segs, f1 = (i + 1) / (float) segs;
            float ax = hx - shdx * len * f0, ay = hy - shdy * len * f0;
            float bx = hx - shdx * len * f1, by = hy - shdy * len * f1;
            int a = (int) (230 * fade * (1f - f0));
            trailPaint.setColor((a << 24) | 0xFFFFFF);
            trailPaint.setStrokeWidth(Math.max(1f, (2.6f - 2.2f * f0) * dens));
            trailPaint.setStrokeCap(Paint.Cap.ROUND);
            cv.drawLine(ax, ay, bx, by, trailPaint);
        }
        trailPaint.setColor(((int) (255 * fade) << 24) | 0xFFFFFF);
        cv.drawCircle(hx, hy, 2.2f * dens, trailPaint);
    }
}
