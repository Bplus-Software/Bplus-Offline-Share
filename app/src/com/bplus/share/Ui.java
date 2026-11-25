package com.bplus.share;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

public class Ui {
    // palette
    public static int bg, card, border, text, dim, accent, green, bar, field;
    public static boolean light;

    public static void applyTheme(Context c) {
        String mode = Store.get("theme", "system");
        boolean sysDark = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        light = mode.equals("light") || (mode.equals("system") && !sysDark);
        if (light) {
            bg = Color.rgb(245,247,250); card = Color.WHITE; border = Color.rgb(226,231,238);
            text = Color.rgb(24,30,40); dim = Color.rgb(110,120,135); accent = Color.rgb(47,110,220);
            green = Color.rgb(24,150,100); bar = Color.WHITE; field = Color.rgb(238,242,247);
        } else {
            bg = Color.rgb(16,20,27); card = Color.rgb(26,32,41); border = Color.rgb(36,44,58);
            text = Color.rgb(238,241,246); dim = Color.rgb(139,150,166); accent = Color.rgb(76,141,255);
            green = Color.rgb(46,196,140); bar = Color.rgb(21,27,36); field = Color.rgb(26,32,41);
        }
    }

    public static void applyLocale(Context c) {
        String lang = Store.get("lang", "system");
        if ("system".equals(lang)) return;
        java.util.Locale locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale);
        Configuration cfg = c.getResources().getConfiguration();
        cfg.setLocale(locale);
        cfg.setLayoutDirection(locale);
        c.getResources().updateConfiguration(cfg, c.getResources().getDisplayMetrics());
    }

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable round(int color, float rDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(c, rDp));
        return g;
    }

    public static GradientDrawable roundBorder(int color, int borderColor, float rDp, Context c) {
        GradientDrawable g = round(color, rDp, c);
        g.setStroke(dp(c, 1), borderColor);
        return g;
    }

    public static TextView t(Context c, String s, float sp, int color, Typeface tf) {
        TextView v = new TextView(c);
        v.setText(s); v.setTextSize(sp); v.setTextColor(color);
        v.setTypeface(tf != null ? tf : App.cairo);
        return v;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    public static LinearLayout.LayoutParams lpw(float weight) { return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight); }
    public static LinearLayout.LayoutParams lpW1() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f); }

    public static void pad(View v, int l, int t, int r, int b, Context c) {
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    public static void margins(View v, int l, int t, int r, int b, Context c) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        if (p instanceof ViewGroup.MarginLayoutParams)
            ((ViewGroup.MarginLayoutParams) p).setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    public static TextView header(Context c, String s) {
        TextView v = t(c, s, 21, text, App.cairoB);
        pad(v, 22, 18, 22, 10, c);
        return v;
    }

    public static TextView sub(Context c, String s) {
        TextView v = t(c, s, 13, dim, App.cairoSB);
        pad(v, 22, 0, 22, 12, c);
        v.setLineSpacing(0, 1.15f);
        return v;
    }

    public static TextView secHead(Context c, String s) {
        TextView v = t(c, s.toUpperCase(java.util.Locale.ENGLISH), 11.5f, Color.rgb(94,104,120), App.cairoB);
        pad(v, 22, 16, 22, 6, c);
        return v;
    }

    public static TextView cta(Context c, String s) {
        TextView v = t(c, s, 15, Color.WHITE, App.cairoB);
        v.setBackground(round(accent, 14, c));
        v.setGravity(Gravity.CENTER);
        pad(v, 26, 11, 26, 11, c);
        return v;
    }

    public static TextView ghost(Context c, String s) {
        TextView v = t(c, s, 14, light ? dim : Color.rgb(198,205,216), App.cairoB);
        v.setBackground(round(border, 14, c));
        v.setGravity(Gravity.CENTER);
        pad(v, 22, 10, 22, 10, c);
        return v;
    }

    /** Transparent icon with a separate 44dp touch target; never uses filled ghost-button padding. */
    public static TextView icon(Context c,String symbol,String label){TextView v=t(c,symbol,22,dim,null);v.setTypeface(android.graphics.Typeface.DEFAULT);v.setGravity(Gravity.CENTER);v.setIncludeFontPadding(false);v.setSingleLine(true);v.setBackgroundColor(Color.TRANSPARENT);v.setPadding(0,0,0,0);v.setMinWidth(dp(c,44));v.setMinHeight(dp(c,44));v.setLayoutParams(lp(dp(c,44),dp(c,44)));v.setContentDescription(label);v.setTooltipText(label);return v;}

    // settings-style row: name + sub on the left, value on the right
    public static LinearLayout setRow(Context c, String name, String sub, String value) {
        LinearLayout r = row(c);
        pad(r, 20, 14, 20, 14, c);
        LinearLayout meta = col(c);
        TextView n = t(c, name, 15, text, App.cairoB);
        meta.addView(n);
        if (sub != null) {
            TextView s = t(c, sub, 11.5f, dim, App.cairoSB);
            s.setLineSpacing(0, 1.15f);
            meta.addView(s);
        }
        r.addView(meta, lpw(1));
        if (value != null) {
            TextView v = t(c, value, 12.5f, accent, App.cairoB);
            v.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            pad(v, 12, 0, 0, 0, c);
            r.addView(v, lp(-2, -2));
        }
        View div = new View(c);
        div.setBackgroundColor(border);
        LinearLayout wrap = col(c);
        wrap.addView(r);
        wrap.addView(div, lp(-1, dp(c, 1)));
        return wrap;
    }

    public static LinearLayout topBar(Context c, String title, boolean back, View.OnClickListener onGear) {
        LinearLayout b = row(c);
        pad(b, 14, 12, 14, 8, c);
        if (back) {
            TextView bk = t(c, "‹", 26, dim, App.cairoB);
            bk.setGravity(Gravity.CENTER);
            bk.setBackground(round(field, 12, c));
            b.addView(bk, lp(dp(c, 38), dp(c, 38)));
            bk.setOnClickListener(v -> ((android.app.Activity) c).finish());
        }
        TextView t = t(c, title, back ? 17 : 20, text, App.cairoB);
        pad(t, 12, 0, 12, 0, c);
        b.addView(t, lpw(1));
        if (onGear != null) {
            TextView g = t(c, "⚙", 17, dim, App.cairoSB);
            g.setGravity(Gravity.CENTER);
            g.setBackground(round(field, 12, c));
            b.addView(g, lp(dp(c, 38), dp(c, 38)));
            g.setOnClickListener(onGear);
        }
        return b;
    }

    public static ProgressBar hbar(Context c, int heightDp) {
        ProgressBar p = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(1000);
        android.graphics.drawable.GradientDrawable bgd = round(border, heightDp / 2f, c);
        android.graphics.drawable.ClipDrawable fill = new android.graphics.drawable.ClipDrawable(round(accent, heightDp / 2f, c), Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL);
        android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{bgd, fill});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        p.setProgressDrawable(layers);
        return p;
    }

    public static ScrollView scroll(Context c, LinearLayout content) {
        ScrollView s = new ScrollView(c);
        s.addView(content);
        s.setFillViewport(true);
        return s;
    }

    public static String fmtSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format(java.util.Locale.US, "%.1f KB", b / 1024f);
        if (b < 1024L * 1024 * 1024) return String.format(java.util.Locale.US, "%.1f MB", b / 1048576f);
        return String.format(java.util.Locale.US, "%.2f GB", b / 1073741824f);
    }
}
