package com.bplus.share;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.Executors;

public class SettingsActivity extends Activity {
    private static final int REQ_TREE = 71;
    private static final String REPO = "basem-sadoun/Bplus-Share";
    private LinearLayout box;
    private TextView updateValue;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.applyLocale(this);
        Ui.applyTheme(this);
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.bg);
        root.addView(Ui.topBar(this, getString(R.string.settings), true, null));
        box = Ui.col(this);
        ScrollView sc = new ScrollView(this);
        sc.addView(box);
        root.addView(sc, Ui.lpW1());
        setContentView(root);
        build();
    }

    private void build() {
        box.removeAllViews();
        box.addView(Ui.secHead(this, getString(R.string.sec_transfer)));

        // transfer mode
        boolean internet = Store.get("mode", "wifi").equals("internet");
        LinearLayout modeRow = Ui.setRow(this, getString(R.string.transfer_mode), getString(R.string.transfer_mode_sub), null);
        box.addView(modeRow);
        LinearLayout seg = Ui.row(this);
        seg.setBackground(Ui.round(Ui.field, 12, this));
        Ui.pad(seg, 3, 3, 3, 3, this);
        LinearLayout.LayoutParams slp = Ui.lp(-1, -2);
        slp.setMargins(Ui.dp(this, 20), Ui.dp(this, 6), Ui.dp(this, 20), Ui.dp(this, 12));
        seg.setLayoutParams(slp);
        TextView wifi = segBtn(getString(R.string.mode_wifi), !internet);
        TextView net = segBtn(getString(R.string.mode_internet) + " · " + getString(R.string.internet_soon), internet);
        wifi.setOnClickListener(v -> { Store.set("mode", "wifi"); build(); });
        net.setOnClickListener(v -> android.widget.Toast.makeText(this, getString(R.string.internet_soon), android.widget.Toast.LENGTH_SHORT).show());
        seg.addView(wifi, Ui.lpw(1));
        seg.addView(net, Ui.lpw(1));
        box.addView(seg);

        box.addView(Ui.setRow(this, getString(R.string.device_name), getString(R.string.device_name_sub), Store.deviceName()));
        box.getChildAt(box.getChildCount() - 1).setOnClickListener(v -> editDeviceName());

        box.addView(Ui.setRow(this, getString(R.string.save_to), null, "Downloads/B+ Share"));

        LinearLayout testVersions=Ui.setRow(this,getString(R.string.include_test_versions),getString(R.string.include_test_versions_note),Store.get("includeTestVersions","1").equals("1")?getString(R.string.autosend_on):getString(R.string.autosend_off));box.addView(testVersions);testVersions.setOnClickListener(v->{Store.set("includeTestVersions",Store.get("includeTestVersions","1").equals("1")?"0":"1");build();});
        LinearLayout autoCheck=Ui.setRow(this,getString(R.string.auto_update_check),getString(R.string.auto_update_check_note),Store.get("autoUpdateCheck","0").equals("1")?getString(R.string.autosend_on):getString(R.string.autosend_off));box.addView(autoCheck);autoCheck.setOnClickListener(v->{Store.set("autoUpdateCheck",Store.get("autoUpdateCheck","0").equals("1")?"0":"1");build();});
        String st = Store.get("updateState", "");
        String uv = st.isEmpty() ? null : st;
        LinearLayout upd = Ui.setRow(this, getString(R.string.check_updates), getString(R.string.updates_sub), uv);
        updateValue = findValue(upd);
        upd.setOnClickListener(v -> checkUpdates());
        box.addView(upd);

        boolean auto = Store.autosendOn();
        String autoVal = auto ? getString(R.string.autosend_on) : getString(R.string.autosend_off);
        LinearLayout autoRow = Ui.setRow(this, getString(R.string.autosend), getString(R.string.autosend_sub), autoVal);
        autoRow.setOnClickListener(v -> configureAutosend());
        box.addView(autoRow);

        box.addView(Ui.secHead(this, getString(R.string.general)));

        String lang = Store.get("lang", "system");
        String langLabel = lang.equals("ar") ? "العربية" : lang.equals("en") ? "English" : getString(R.string.follow_system);
        LinearLayout langRow = Ui.setRow(this, getString(R.string.language), null, langLabel);
        langRow.setOnClickListener(v -> pickLanguage());
        box.addView(langRow);

        String theme = Store.get("theme", "system");
        String themeLabel = theme.equals("dark") ? getString(R.string.dark) : theme.equals("light") ? getString(R.string.light) : getString(R.string.follow_system);
        LinearLayout themeRow = Ui.setRow(this, getString(R.string.theme), null, themeLabel);
        themeRow.setOnClickListener(v -> pickTheme());
        box.addView(themeRow);

        box.addView(Ui.setRow(this, getString(R.string.about), getString(R.string.about_sub, "1.1.16"), null));
        TextView prayer = Ui.t(this, getString(R.string.prayer), 12, Ui.dim, App.cairoSB);
        Ui.pad(prayer, 22, 8, 22, 24, this);
        box.addView(prayer);
    }

    private TextView segBtn(String s, boolean on) {
        TextView v = Ui.t(this, s, 12.5f, on ? android.graphics.Color.WHITE : Ui.dim, App.cairoB);
        v.setGravity(android.view.Gravity.CENTER);
        Ui.pad(v, 0, 9, 0, 9, this);
        if (on) v.setBackground(Ui.round(Ui.accent, 9, this));
        return v;
    }

    private TextView findValue(View row) {
        // value is the last TextView in the row layout (row -> [meta, value])
        try {
            LinearLayout wrap = (LinearLayout) row;
            LinearLayout inner = (LinearLayout) wrap.getChildAt(0);
            View last = inner.getChildAt(inner.getChildCount() - 1);
            if (last instanceof TextView) return (TextView) last;
        } catch (Exception ignored) {}
        return null;
    }

    private void editDeviceName() {
        EditText e = new EditText(this);
        e.setText(Store.deviceName());
        e.setTypeface(App.cairoSB);
        int pad = Ui.dp(this, 20);
        LinearLayout wrap = Ui.col(this);
        wrap.setPadding(pad, pad, pad, 0);
        wrap.addView(e);
        new AppDialog(this)
                .setTitle(getString(R.string.device_name))
                .setView(wrap)
                .setPositiveButton("OK", (d, w) -> {
                    String n = e.getText().toString().trim();
                    if (!n.isEmpty()) Store.set("deviceName", n);
                    build();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void checkUpdates() {
        startActivity(new Intent(this,UpdateActivity.class));
    }

    private void configureAutosend() {
        if (!Store.autosendOn() && Store.autosendFolder().isEmpty()) {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(i, REQ_TREE);
            return;
        }
        String[] items = {getString(R.string.autosend_on), getString(R.string.autosend_off), getString(R.string.pick_folder), getString(R.string.pick_target)};
        new AppDialog(this)
                .setTitle(getString(R.string.autosend))
                .setItems(items, (d, which) -> {
                    if (which == 0) Store.set("autosend", "1");
                    else if (which == 1) Store.set("autosend", "0");
                    else if (which == 2) startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ_TREE);
                    else pickAutosendTarget();
                    build();
                }).show();
    }

    private void pickAutosendTarget() {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        JSONArray peers = Store.peers();
        for (int i = 0; i < peers.length(); i++) {
            JSONObject p = peers.optJSONObject(i);
            if (p == null) continue;
            labels.add(p.optString("name"));
            ids.add("dev:" + p.optString("id"));
        }
        JSONArray groups = Store.groups();
        for (int i = 0; i < groups.length(); i++) {
            JSONObject g = groups.optJSONObject(i);
            if (g == null) continue;
            labels.add("▦ " + g.optString("name"));
            ids.add("grp:" + g.optString("id"));
        }
        if (ids.isEmpty()) return;
        new AppDialog(this)
                .setTitle(getString(R.string.pick_target))
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    Store.set("autosendTarget", ids.get(which));
                    Store.set("autosend", "1");
                    build();
                }).show();
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_TREE && res == RESULT_OK && data != null) {
            Uri tree = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(tree,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            Store.set("autosendFolder", tree.toString());
            if (Store.autosendTarget().isEmpty()) pickAutosendTarget();
            else { Store.set("autosend", "1"); build(); }
        }
    }

    private void pickLanguage() {
        String[] items = {getString(R.string.follow_system), "English", "العربية"};
        new AppDialog(this)
                .setTitle(getString(R.string.language))
                .setItems(items, (d, which) -> {
                    Store.set("lang", which == 1 ? "en" : which == 2 ? "ar" : "system");
                    Ui.applyLocale(getApplicationContext());
                    recreate();
                }).show();
    }

    private void pickTheme() {
        String[] items = {getString(R.string.follow_system), getString(R.string.dark), getString(R.string.light)};
        new AppDialog(this)
                .setTitle(getString(R.string.theme))
                .setItems(items, (d, which) -> {
                    Store.set("theme", which == 1 ? "dark" : which == 2 ? "light" : "system");
                    Store.set("themeSet", "1");
                    Ui.applyLocale(this);
        Ui.applyTheme(this);
                    recreate();
                }).show();
    }
}
