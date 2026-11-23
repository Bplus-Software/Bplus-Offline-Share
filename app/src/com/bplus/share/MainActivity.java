package com.bplus.share;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.applyLocale(this);
        Ui.applyTheme(this);
        getWindow().setStatusBarColor(Ui.bg);
        getWindow().setNavigationBarColor(Ui.bg);

        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.bg);

        SpannableString name = new SpannableString("B+ Share");
        name.setSpan(new ForegroundColorSpan(Ui.accent), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView title = Ui.t(this, name.toString(), 20, Ui.text, App.cairoB);
        title.setText(name);

        LinearLayout top = Ui.row(this);
        Ui.pad(top, 20, 14, 20, 6, this);
        top.addView(title, Ui.lpw(1));
        TextView gear = Ui.t(this, "⚙", 17, Ui.dim, App.cairoSB);
        gear.setGravity(Gravity.CENTER);
        gear.setBackground(Ui.round(Ui.field, 12, this));
        top.addView(gear, Ui.lp(Ui.dp(this, 38), Ui.dp(this, 38)));
        gear.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        root.addView(top);

        // hero logo
        LinearLayout hero = Ui.col(this);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView logo = new TextView(this);
        logo.setText("B+");
        logo.setTextSize(34);
        logo.setTypeface(App.cairoB);
        logo.setTextColor(Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable lg = Ui.round(Ui.light ? Color.rgb(47,110,220) : Color.rgb(29,38,53), 26, this);
        if (!Ui.light) lg.setStroke(Ui.dp(this, 1), Ui.border);
        logo.setBackground(lg);
        LinearLayout.LayoutParams llp = Ui.lp(Ui.dp(this, 86), Ui.dp(this, 86));
        llp.topMargin = Ui.dp(this, 34);
        hero.addView(logo, llp);
        TextView tag = Ui.t(this, getString(R.string.tagline), 13.5f, Ui.dim, App.cairoSB);
        tag.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = Ui.lp(-2, -2);
        tlp.topMargin = Ui.dp(this, 14);
        hero.addView(tag, tlp);
        root.addView(hero);

        LinearLayout btns = Ui.col(this);
        Ui.pad(btns, 24, 36, 24, 0, this);
        btns.addView(bigCard("↗", Ui.accent, getString(R.string.send_files), getString(R.string.send_sub),
                v -> startActivity(new Intent(this, SendActivity.class))));
        btns.addView(bigCard("↙", Ui.green, getString(R.string.receive_files), getString(R.string.receive_sub),
                v -> startActivity(new Intent(this, ScanActivity.class))));
        btns.addView(bigCard("▦", Ui.dim, getString(R.string.devices_groups), "",
                v -> startActivity(new Intent(this, DevicesActivity.class))));
        root.addView(btns);

        TextView footer = Ui.t(this, getString(R.string.footer), 12, Color.rgb(94,104,120), App.cairoSB);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp = Ui.lp(-1, -2);
        flp.topMargin = Ui.dp(this, 40);
        root.addView(footer, flp);

        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 7);
        if (Build.VERSION.SDK_INT <= 28 &&
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 8);

        TransferService.start(this);
    }

    private View bigCard(String icon, int iconColor, String title, String sub, View.OnClickListener click) {
        LinearLayout card = Ui.row(this);
        card.setBackground(Ui.roundBorder(Ui.card, Ui.border, 20, this));
        Ui.pad(card, 20, 18, 20, 18, this);
        TextView ic = Ui.t(this, icon, 22, iconColor, App.cairoB);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(Ui.round(tint(iconColor), 16, this));
        card.addView(ic, Ui.lp(Ui.dp(this, 52), Ui.dp(this, 52)));
        LinearLayout meta = Ui.col(this);
        Ui.pad(meta, 16, 0, 16, 0, this);
        meta.addView(Ui.t(this, title, 17, Ui.text, App.cairoB));
        if (!sub.isEmpty()) meta.addView(Ui.t(this, sub, 12.5f, Ui.dim, App.cairoSB));
        card.addView(meta, Ui.lpw(1));
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.bottomMargin = Ui.dp(this, 16);
        card.setLayoutParams(lp);
        card.setOnClickListener(click);
        return card;
    }

    private static int tint(int color) {
        return Color.argb(36, Color.red(color), Color.green(color), Color.blue(color));
    }
}
