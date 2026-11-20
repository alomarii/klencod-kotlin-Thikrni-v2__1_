package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** مؤشر شريط التنقل السائل: كبسولة تنفصل عنها كبسولة تتأخر خلفها ثم تلتحم بها. */
public class NavGoo extends View {
    private final Theme th;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Goo.Blob lead = new Goo.Blob(0, 0, 0);
    private final Goo.Blob trail = new Goo.Blob(0, 0, 0);
    private float pw = 0, ph = 0;
    private ValueAnimator loop;
    private long last;

    public NavGoo(Context c, Theme theme) {
        super(c);
        th = theme;
        loop = ValueAnimator.ofFloat(0f, 1f);
        loop.setDuration(1000);
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.setInterpolator(new LinearInterpolator());
        loop.addUpdateListener(a -> frame());
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        fill.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{th.accentLight, th.accent, th.accentDark}, null, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDetachedFromWindow() {
        loop.cancel();
        super.onDetachedFromWindow();
    }

    void setPillSize(float w, float h) {
        pw = w;
        ph = h;
        invalidate();
    }

    void snapTo(float x) {
        loop.cancel();
        lead.x = lead.tx = trail.x = trail.tx = x;
        lead.vx = trail.vx = 0;
        invalidate();
    }

    void moveTo(float x) {
        lead.tx = x;
        trail.tx = x;
        last = System.nanoTime();
        if (!loop.isStarted()) loop.start();
    }

    private void frame() {
        long now = System.nanoTime();
        float dt = Math.min(0.033f, (now - last) / 1e9f);
        last = now;
        lead.step(dt, 380f, 26f);
        trail.step(dt, 120f, 13f);
        invalidate();
        if (lead.settled() && trail.settled()) {
            lead.x = trail.x = lead.tx;
            lead.vx = trail.vx = 0;
            loop.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (pw <= 0 || ph <= 0) return;
        float cy = getHeight() / 2f;
        drawPill(cv, lead.x, cy);
        // بدون Goo (أندرويد أقدم من ١٢) تُرسم الكبسولة الأمامية فقط
        if (Goo.supported()) drawPill(cv, trail.x, cy);
    }

    private void drawPill(Canvas cv, float x, float cy) {
        rect.set(x - pw / 2f, cy - ph / 2f, x + pw / 2f, cy + ph / 2f);
        cv.drawRoundRect(rect, ph / 2f, ph / 2f, fill);
    }
}
