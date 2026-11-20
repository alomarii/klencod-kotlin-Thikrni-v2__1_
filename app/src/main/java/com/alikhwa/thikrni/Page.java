package com.alikhwa.thikrni;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** صفحة داخل التطبيق (تبويب). */
abstract class Page {
    final MainActivity act;
    final Ui ui;
    final Prefs p;
    final ScrollView root;
    final LinearLayout content;

    Page(MainActivity a, Ui ui) {
        this.act = a;
        this.ui = ui;
        this.p = new Prefs(a);
        root = new ScrollView(a);
        root.setVerticalScrollBarEnabled(false);
        root.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, ui.dp(8), 0, ui.dp(112));
        root.addView(content, new ViewGroup.LayoutParams(-1, -2));
    }

    abstract void build();

    /** يُستدعى كل مرة يظهر فيها التبويب. */
    void onShow() {
    }

    /** يُستدعى عند مغادرة التبويب. */
    void onHide() {
    }

    /** يُستدعى كل ثانية والتبويب ظاهر. */
    void tick() {
    }

    /** يخفي عناصر الصفحة تمهيدًا لحركة الظهور. */
    void prepareEntrance() {
        for (int i = 0; i < content.getChildCount(); i++) {
            View v = content.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(ui.dp(34));
            v.setScaleX(0.94f);
            v.setScaleY(0.94f);
        }
    }

    /** ظهور متتابع نابضي للبطاقات. */
    void playEntrance() {
        for (int i = 0; i < content.getChildCount(); i++) {
            View v = content.getChildAt(i);
            v.animate().cancel();
            v.setAlpha(0f);
            v.setTranslationY(ui.dp(40));
            v.setScaleX(0.94f);
            v.setScaleY(0.94f);
            v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setStartDelay(75L * i)
                    .setDuration(620).setInterpolator(new OvershootInterpolator(0.9f)).start();
        }
    }
}
