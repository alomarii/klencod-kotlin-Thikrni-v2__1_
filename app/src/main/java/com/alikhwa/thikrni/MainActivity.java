package com.alikhwa.thikrni;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.animation.ValueAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** الشاشة الرئيسية: أربع صفحات بشريط تنقل سفلي وانتقالات متحركة. */
public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 21;
    private static final String STATE_TAB = "tab";

    private Prefs p;
    private Ui ui;
    private final Handler h = new Handler(Looper.getMainLooper());

    private FrameLayout root, pager;
    private BottomNav nav;
    private final Page[] pages = new Page[4];
    private Page curPage;
    private int current = -1;
    private boolean celebrating = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (curPage != null) curPage.tick();
            h.postDelayed(this, 1000);
        }
    };

    private final Stats.Listener statsListener = reached -> {
        HomePage home = (HomePage) pages[0];
        if (home != null) home.refreshCount(true);
        StatsPage st = (StatsPage) pages[1];
        if (st != null && current == 1) st.refreshValues(false);
        if (reached) celebrate();
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);
        Theme t = Theme.current(this);
        ui = new Ui(this, t);
        getWindow().setStatusBarColor(t.bgTop);
        getWindow().setNavigationBarColor(t.bgBottom);

        Tracks.migrateLegacy(this);

        root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(new StarsView(this, t), new FrameLayout.LayoutParams(-1, -1));

        pager = new FrameLayout(this);
        root.addView(pager, new FrameLayout.LayoutParams(-1, -1));

        int startTab = b != null ? b.getInt(STATE_TAB, 0) : 0;
        nav = new BottomNav(this, ui, startTab, i -> showTab(i, true, true));
        FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(-1, ui.dp(68),
                Gravity.BOTTOM);
        nlp.setMargins(ui.dp(14), 0, ui.dp(14), ui.dp(12));
        root.addView(nav, nlp);
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
        ReminderScheduler.syncAll(this);
        DeviceCounter.register(this);

        boolean cold = b == null;
        showTab(startTab, false, !cold);
        if (cold) {
            curPage.prepareEntrance();
            showSplash();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(STATE_TAB, Math.max(0, current));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Stats.listener = statsListener;
        if (curPage != null) curPage.onShow();
        SalawatWidget.updateAll(this);
        h.post(tick);
    }

    @Override
    protected void onPause() {
        Stats.listener = null;
        h.removeCallbacks(tick);
        if (curPage != null) curPage.onHide();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (current > 0) {
            nav.select(0, true);
            showTab(0, true, true);
        } else {
            super.onBackPressed();
        }
    }

    // ------------------------------------------------------------- التبويبات

    private Page page(int i) {
        if (pages[i] == null) {
            switch (i) {
                case 0:
                    pages[i] = new HomePage(this, ui);
                    break;
                case 1:
                    pages[i] = new StatsPage(this, ui);
                    break;
                case 2:
                    pages[i] = new AudioPage(this, ui);
                    break;
                default:
                    pages[i] = new SettingsPage(this, ui);
                    break;
            }
        }
        return pages[i];
    }

    private void showTab(int i, boolean animate, boolean entrance) {
        if (i == current && curPage != null) return;
        final Page prev = curPage;
        int prevIdx = current;
        final Page next = page(i);
        current = i;
        curPage = next;
        if (prev != null) prev.onHide();
        next.onShow();

        final View nv = next.root;
        if (nv.getParent() == null) pager.addView(nv, new FrameLayout.LayoutParams(-1, -1));
        nv.animate().cancel();
        nv.setVisibility(View.VISIBLE);
        next.root.scrollTo(0, 0);

        if (!animate || prev == null) {
            nv.setAlpha(1f);
            nv.setTranslationX(0f);
            nv.setScaleX(1f);
            nv.setScaleY(1f);
            if (entrance) next.playEntrance();
            return;
        }

        // الصفحة التالية في الترتيب تدخل من اليسار (واجهة عربية) والقديمة تخرج لليمين
        float shift = ui.dp(64);
        float inFrom = i > prevIdx ? -shift : shift;
        nv.setAlpha(0f);
        nv.setTranslationX(inFrom);
        nv.setScaleX(0.93f);
        nv.setScaleY(0.93f);
        nv.animate().alpha(1f).translationX(0f).scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(420)
                .setInterpolator(new DecelerateInterpolator(2.2f)).start();

        final View ov = prev.root;
        ov.animate().cancel();
        ov.animate().alpha(0f).translationX(-inFrom * 0.7f).scaleX(0.93f).scaleY(0.93f)
                .setStartDelay(0).setDuration(260)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    if (curPage == null || ov != curPage.root) {
                        ov.setVisibility(View.GONE);
                        ov.setScaleX(1f);
                        ov.setScaleY(1f);
                    }
                }).start();
        next.playEntrance();
    }

    // ---------------------------------------------------------------- مساعدات

    void pickAudio(boolean anyFile) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        if (!anyFile) {
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "application/ogg",
                    "application/x-ogg", "video/mp4", "video/3gpp"});
        }
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_AUDIO);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_AUDIO && res == RESULT_OK && data != null) {
            List<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    uris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
            ((AudioPage) page(2)).onPicked(uris);
        }
    }

    /** تطبيق ثيم جديد بإعادة بناء الواجهة مع تلاشٍ ناعم. */
    void applyTheme() {
        SalawatWidget.updateAll(this);
        recreate();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // ------------------------------------------------------------ شاشة البداية

    private void showSplash() {
        final Theme t = ui.t;
        final FrameLayout sp = new FrameLayout(this);
        sp.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{t.bgTop, t.bgMid, t.bgBottom}));
        sp.setClickable(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        final ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_alikhwa);
        box.addView(logo, new LinearLayout.LayoutParams(ui.dp(130), ui.dp(130)));
        final TextView title = ui.text("ذكرني", 44, t.accent, true);
        title.setPadding(0, ui.dp(12), 0, 0);
        final LinearGradient shader = new LinearGradient(0, 0, ui.dp(260), 0,
                new int[]{t.accentDark, t.accentLight, t.accent, t.accentDark},
                new float[]{0f, 0.35f, 0.6f, 1f}, Shader.TileMode.REPEAT);
        title.getPaint().setShader(shader);
        final Matrix m = new Matrix();
        ValueAnimator shimmer = ValueAnimator.ofFloat(0f, ui.dp(260));
        shimmer.setDuration(1400);
        shimmer.setRepeatCount(ValueAnimator.INFINITE);
        shimmer.setInterpolator(new LinearInterpolator());
        shimmer.addUpdateListener(a -> {
            m.setTranslate((float) a.getAnimatedValue(), 0);
            shader.setLocalMatrix(m);
            title.invalidate();
        });
        shimmer.start();
        final TextView verse = ui.text("اللهم صلِّ وسلِّم على نبينا محمد", 16, t.sub, false);
        verse.setPadding(0, ui.dp(6), 0, 0);
        box.addView(title);
        box.addView(verse);
        sp.addView(box, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        sp.addView(new RingsView(this, t), 0, new FrameLayout.LayoutParams(-1, -1));
        root.addView(sp, new FrameLayout.LayoutParams(-1, -1));

        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        logo.setAlpha(0f);
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(750)
                .setInterpolator(new OvershootInterpolator(1.6f)).start();
        title.setAlpha(0f);
        title.setTranslationY(ui.dp(16));
        title.animate().alpha(1f).translationY(0).setStartDelay(300).setDuration(600).start();
        verse.setAlpha(0f);
        verse.animate().alpha(1f).setStartDelay(600).setDuration(600).start();

        h.postDelayed(() -> {
            sp.animate().alpha(0f).setDuration(450).withEndAction(() -> {
                shimmer.cancel();
                root.removeView(sp);
            }).start();
            if (curPage != null) curPage.playEntrance();
        }, 1700);
    }

    // ----------------------------------------------------------------- احتفال

    private void celebrate() {
        if (celebrating) return;
        celebrating = true;
        final Theme t = ui.t;
        final FrameLayout ov = new FrameLayout(this);
        ConfettiView cf = new ConfettiView(this, t);
        ov.addView(cf, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Theme.a(t.bgMid, 0xF2), Theme.a(t.bgBottom, 0xF2)});
        g.setCornerRadius(ui.dp(28));
        g.setStroke(ui.dp(2), t.accent);
        box.setBackground(g);
        box.setPadding(ui.dp(28), ui.dp(24), ui.dp(28), ui.dp(24));
        box.addView(ui.text("ما شاء الله!", 32, t.accent, true));
        TextView sub = ui.text("أتممت هدفك اليومي: " + HijriUtil.num(p.goal()) + " صلاة على النبي", 16, Color.WHITE, true);
        sub.setPadding(0, ui.dp(8), 0, ui.dp(4));
        box.addView(sub);
        box.addView(ui.text("تقبّل الله منك، اللهم صلِّ وسلِّم على نبينا محمد", 13, t.sub, false));
        ov.addView(box, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        root.addView(ov, new FrameLayout.LayoutParams(-1, -1));

        box.setScaleX(0.5f);
        box.setScaleY(0.5f);
        box.setAlpha(0f);
        box.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(520)
                .setInterpolator(new OvershootInterpolator(1.8f)).start();
        cf.start();
        root.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.LONG_PRESS);

        h.postDelayed(() -> ov.animate().alpha(0f).setDuration(500).withEndAction(() -> {
            root.removeView(ov);
            celebrating = false;
        }).start(), 3300);
    }
}
