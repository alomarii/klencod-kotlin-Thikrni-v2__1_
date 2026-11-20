package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * زر Venom: كبسولة سائلة تتموّج حوافها بقطرات متحركة، وتتجمع القطرات نحو إصبعك
 * وتنتفخ الحواف عندك، ثم تتناثر وتلتئم عند الرفع.
 */
public class VenomButton extends FrameLayout {
    private static final int N = 4;

    private final boolean dark;
    private final Theme th;
    private final float d;
    private final int pad;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final TextView label;

    private final Goo.Blob[] drops = new Goo.Blob[N];
    private final float[] ph = new float[N], spd = new float[N];
    private final Goo.Blob tail = new Goo.Blob(0, 0, 0);
    private boolean touching;
    private float touchX, touchY, time, scale = 1f;
    private float pw, ph2, cx, cy; // قياسات الكبسولة
    private ValueAnimator loop, scaleAnim;
    private long last;

    public VenomButton(Context c, Theme theme, String text, boolean dark) {
        super(c);
        th = theme;
        this.dark = dark;
        d = c.getResources().getDisplayMetrics().density;
        pad = Math.round(10 * d);
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);

        for (int i = 0; i < N; i++) {
            drops[i] = new Goo.Blob(0, 0, 0);
            ph[i] = i * 1.7f;
            spd[i] = 0.7f + i * 0.23f;
        }

        View gooLayer = new View(c) {
            @Override
            protected void onDraw(Canvas cv) {
                drawGoo(cv);
            }
        };
        View shine = new View(c) {
            @Override
            protected void onDraw(Canvas cv) {
                drawShine(cv);
            }
        };
        label = new TextView(c);
        label.setText(text);
        label.setTextSize(17);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(dark ? th.accent : th.onAccent);
        label.setGravity(Gravity.CENTER);
        label.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        addView(gooLayer, new LayoutParams(-1, -1));
        addView(shine, new LayoutParams(-1, -1));
        addView(label, new LayoutParams(-2, -2, Gravity.CENTER));
        Goo.apply(gooLayer, 7f * d);

        loop = ValueAnimator.ofFloat(0f, 1f);
        loop.setDuration(1000);
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.setInterpolator(new LinearInterpolator());
        loop.addUpdateListener(a -> frame());
    }

    public void setText(String s) {
        label.setText(s);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        pw = w - 2f * pad;
        ph2 = h - 2f * pad;
        cx = w / 2f;
        cy = h / 2f;
        rect.set(pad, pad, w - pad, h - pad);
        if (dark) {
            fill.setShader(new LinearGradient(0, pad, 0, h - pad,
                    new int[]{0xFF222A33, 0xFF06090C}, null, Shader.TileMode.CLAMP));
        } else {
            fill.setShader(new LinearGradient(0, pad, 0, h - pad,
                    new int[]{th.accentLight, th.accent, th.accentDark}, null, Shader.TileMode.CLAMP));
        }
        shinePaint.setShader(new LinearGradient(0, pad, 0, cy,
                0x44FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        for (int i = 0; i < N; i++) {
            drops[i].x = drops[i].tx = cx;
            drops[i].y = drops[i].ty = cy;
            drops[i].r = drops[i].tr = 0.3f * ph2;
        }
        tail.x = tail.tx = cx;
        tail.y = tail.ty = cy;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        last = System.nanoTime();
        if (!loop.isStarted()) loop.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        loop.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    public void onVisibilityAggregated(boolean visible) {
        super.onVisibilityAggregated(visible);
        if (visible) {
            last = System.nanoTime();
            if (isAttachedToWindow() && !loop.isStarted()) loop.start();
        } else {
            loop.cancel();
        }
    }

    private void frame() {
        long now = System.nanoTime();
        float dt = Math.min(0.033f, (now - last) / 1e9f);
        last = now;
        time += dt;
        if (ph2 <= 0) return;
        float span = Math.max(0f, pw / 2f - ph2 / 2f);
        for (int i = 0; i < N; i++) {
            Goo.Blob b = drops[i];
            float side = (i % 2 == 0) ? -1f : 1f;
            float wob = 4f * d * (float) Math.sin(time * 1.9f + ph[i]);
            if (touching) {
                // القطرات تتجمع نحو الإصبع فتنتفخ الحواف عنده
                b.tx = Math.max(pad, Math.min(getWidth() - pad, touchX)) + (i - 1.5f) * 6f * d;
                b.tr = 0.36f * ph2;
            } else {
                b.tx = cx + span * (float) Math.sin(time * spd[i] * 0.8f + ph[i]);
                b.tr = 0.30f * ph2 + wob * 0.5f;
            }
            b.ty = cy + side * (ph2 / 2f - 8f * d + wob);
            b.step(dt, 110f, 9f);
        }
        if (touching) {
            tail.tx = Math.max(pad, Math.min(getWidth() - pad, touchX));
            tail.ty = Math.max(pad * 0.6f, Math.min(getHeight() - pad * 0.6f, touchY));
            tail.tr = 0.46f * ph2;
        } else {
            tail.tx = cx;
            tail.ty = cy;
            tail.tr = 0f;
        }
        tail.step(dt, 240f, 17f);
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).invalidate();
    }

    private void drawGoo(Canvas cv) {
        cv.save();
        cv.scale(scale, scale, cx, cy);
        cv.drawRoundRect(rect, ph2 / 2f, ph2 / 2f, fill);
        for (Goo.Blob b : drops) if (b.r > 0.5f) cv.drawCircle(b.x, b.y, b.r, fill);
        if (tail.r > 0.5f) cv.drawCircle(tail.x, tail.y, tail.r, fill);
        cv.restore();
    }

    private void drawShine(Canvas cv) {
        cv.save();
        cv.scale(scale, scale, cx, cy);
        RectF top = new RectF(pad + 2 * d, pad + 2 * d, getWidth() - pad - 2 * d, cy);
        cv.drawRoundRect(top, ph2 / 2f, ph2 / 2f, shinePaint);
        cv.restore();
    }

    private void animateScale(float to, boolean overshoot) {
        if (scaleAnim != null) scaleAnim.cancel();
        scaleAnim = ValueAnimator.ofFloat(scale, to);
        scaleAnim.setDuration(overshoot ? 380 : 110);
        if (overshoot) scaleAnim.setInterpolator(new android.view.animation.OvershootInterpolator(3f));
        scaleAnim.addUpdateListener(a -> {
            scale = (float) a.getAnimatedValue();
            label.setScaleX(scale);
            label.setScaleY(scale);
        });
        scaleAnim.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touching = true;
                touchX = e.getX();
                touchY = e.getY();
                tail.x = touchX;
                tail.y = touchY;
                animateScale(0.96f, false);
                break;
            case MotionEvent.ACTION_MOVE:
                touchX = e.getX();
                touchY = e.getY();
                break;
            case MotionEvent.ACTION_UP:
                touching = false;
                for (int i = 0; i < N; i++) {
                    drops[i].vy += ((i % 2 == 0) ? -1f : 1f) * ph2 * 9f;
                    drops[i].vx += (drops[i].x - cx) * 3f;
                }
                animateScale(1f, true);
                break;
            case MotionEvent.ACTION_CANCEL:
                touching = false;
                animateScale(1f, true);
                break;
            default:
                break;
        }
        return super.onTouchEvent(e);
    }
}
