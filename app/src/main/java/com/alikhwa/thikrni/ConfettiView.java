package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Random;

/** قصاصات ملونة تتساقط عند إتمام الهدف اليومي. */
public class ConfettiView extends View {
    private static final int N = 90;
    private final float[] x = new float[N], y = new float[N], vx = new float[N], vy = new float[N],
            sz = new float[N], rot = new float[N], vr = new float[N];
    private final int[] col = new int[N];
    private final boolean[] round = new boolean[N];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator anim;
    private long last;
    private final float d;
    private boolean ready = false;
    private final Theme th;

    public ConfettiView(Context c, Theme theme) {
        super(c);
        th = theme;
        d = c.getResources().getDisplayMetrics().density;
    }

    private void init() {
        Random r = new Random();
        int[] palette = {th.accent, th.accentLight, 0xFFFFFFFF, 0xFF20C997, th.accentDark, 0xFFFF8FA3};
        for (int i = 0; i < N; i++) {
            x[i] = r.nextFloat() * getWidth();
            y[i] = -r.nextFloat() * getHeight() * 0.6f;
            vx[i] = (r.nextFloat() - 0.5f) * 90 * d;
            vy[i] = (160 + r.nextFloat() * 260) * d;
            sz[i] = (4 + r.nextFloat() * 6) * d;
            rot[i] = r.nextFloat() * 360f;
            vr[i] = (r.nextFloat() - 0.5f) * 540f;
            col[i] = palette[r.nextInt(palette.length)];
            round[i] = r.nextBoolean();
        }
        ready = true;
    }

    public void start() {
        last = System.nanoTime();
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(3600);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            long now = System.nanoTime();
            float dt = Math.min(0.05f, (now - last) / 1e9f);
            last = now;
            if (!ready && getWidth() > 0) init();
            if (ready) {
                for (int i = 0; i < N; i++) {
                    x[i] += vx[i] * dt;
                    y[i] += vy[i] * dt;
                    rot[i] += vr[i] * dt;
                }
            }
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (!ready) return;
        for (int i = 0; i < N; i++) {
            if (y[i] < -20 * d || y[i] > getHeight() + 20 * d) continue;
            paint.setColor(col[i]);
            cv.save();
            cv.translate(x[i], y[i]);
            cv.rotate(rot[i]);
            if (round[i]) cv.drawCircle(0, 0, sz[i] / 2f, paint);
            else cv.drawRect(-sz[i] / 2f, -sz[i] / 4f, sz[i] / 2f, sz[i] / 4f, paint);
            cv.restore();
        }
    }
}
