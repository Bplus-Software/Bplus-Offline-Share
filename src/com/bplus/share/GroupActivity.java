package com.bplus.share;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.Executors;

public class GroupActivity extends Activity {
    private static final int REQ_PICK = 51;
    private String groupId;
    private LinearLayout membersBox;
    private TextView statusLine;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        groupId = getIntent().getStringExtra("group");
        Ui.applyLocale(this);
        Ui.applyTheme(this);
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.bg);
        JSONObject g = group();
        String name = g != null ? g.optString("name") : "";
        root.addView(Ui.topBar(this, name, true, null));
        root.addView(Ui.sub(this, getString(R.string.group_sub)));
        membersBox = Ui.col(this);
        Ui.pad(membersBox, 16, 4, 16, 0, this);
        ScrollView sc = new ScrollView(this);
        LinearLayout content = Ui.col(this);
        content.addView(membersBox);
        statusLine = Ui.sub(this, "");
        content.addView(statusLine);
        sc.addView(content);
        root.addView(sc, Ui.lpW1());

        LinearLayout bottom = Ui.row(this);
        bottom.setBackgroundColor(Ui.bar);
        Ui.pad(bottom, 20, 14, 20, 22, this);
        TextView sendAll = Ui.cta(this, getString(R.string.send_to_group));
        sendAll.setOnClickListener(v -> pickFiles());
        bottom.addView(sendAll, Ui.lpw(1));
        TextView add = Ui.ghost(this, getString(R.string.add_device));
        LinearLayout.LayoutParams alp = Ui.lp(-2, -2);
        alp.setMarginStart(Ui.dp(this, 10));
        bottom.addView(add, alp);
        add.setOnClickListener(v -> showAddDevice());
        root.addView(bottom);
        setContentView(root);
        refresh();
    }

    private JSONObject group() {
        JSONArray groups = Store.groups();
        for (int i = 0; i < groups.length(); i++) {
            JSONObject g = groups.optJSONObject(i);
            if (g != null && groupId.equals(g.optString("id"))) return g;
        }
        return null;
    }

    private void refresh() {
        membersBox.removeAllViews();
        JSONObject g = group();
        if (g == null) return;
        JSONArray members = g.optJSONArray("members");
        if (members == null) return;
        for (int i = 0; i < members.length(); i++) {
            JSONObject peer = Store.peerById(members.optString(i));
            if (peer == null) continue;
            LinearLayout r = Ui.row(this);
            r.setBackground(Ui.roundBorder(Ui.card, Ui.border, 16, this));
            Ui.pad(r, 12, 12, 12, 12, this);
            LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
            lp.bottomMargin = Ui.dp(this, 8);
            r.setLayoutParams(lp);
            TextView ic = Ui.t(this, "📱", 20, Ui.dim, App.cairoSB);
            ic.setGravity(Gravity.CENTER);
            ic.setBackground(Ui.round(Ui.field, 12, this));
            r.addView(ic, Ui.lp(Ui.dp(this, 42), Ui.dp(this, 42)));
            LinearLayout meta = Ui.col(this);
            Ui.pad(meta, 13, 0, 13, 0, this);
            meta.addView(Ui.t(this, peer.optString("name"), 14.5f, Ui.text, App.cairoB));
            meta.addView(Ui.t(this, peer.optString("ip"), 11.5f, Ui.dim, App.cairoSB));
            r.addView(meta, Ui.lpw(1));
            membersBox.addView(r);
        }
    }

    private void showAddDevice() {
        JSONObject g = group();
        if (g == null) return;
        JSONArray members = g.optJSONArray("members");
        JSONArray peers = Store.peers();
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < peers.length(); i++) {
            JSONObject p = peers.optJSONObject(i);
            if (p == null) continue;
            String id = p.optString("id");
            boolean in = false;
            if (members != null)
                for (int m = 0; m < members.length(); m++)
                    if (id.equals(members.optString(m))) { in = true; break; }
            if (!in) { names.add(p.optString("name")); ids.add(id); }
        }
        if (ids.isEmpty()) return;
        new AppDialog(this)
                .setTitle(getString(R.string.add_device))
                .setItems(names.toArray(new String[0]), (d, which) -> {
                    try {
                        JSONArray m2 = g.optJSONArray("members") != null ? g.optJSONArray("members") : new JSONArray();
                        m2.put(ids.get(which));
                        g.put("members", m2);
                        JSONArray groups = Store.groups();
                        for (int i = 0; i < groups.length(); i++)
                            if (groupId.equals(groups.getJSONObject(i).optString("id"))) groups.put(i, g);
                        Store.saveGroups(groups);
                        refresh();
                    } catch (Exception ignored) {}
                }).show();
    }

    private void pickFiles() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_PICK);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_PICK || res != RESULT_OK || data == null) return;
        java.util.ArrayList<Uri> uris = new java.util.ArrayList<>();
        if (data.getClipData() != null)
            for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        sendToGroup(uris);
    }

    private void sendToGroup(java.util.List<Uri> uris) {
        JSONObject g = group();
        if (g == null) return;
        JSONArray members = g.optJSONArray("members");
        if (members == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            int okCount = 0, failCount = 0;
            for (int m = 0; m < members.length(); m++) {
                JSONObject peer = Store.peerById(members.optString(m));
                if (peer == null) continue;
                boolean allOk = true;
                for (Uri u : uris) {
                    try { getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
                    String name = "file"; long size = 0;
                    try (Cursor c = getContentResolver().query(u, null, null, null, null)) {
                        if (c != null && c.moveToFirst()) {
                            int ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            int si = c.getColumnIndex(OpenableColumns.SIZE);
                            if (ni >= 0) name = c.getString(ni);
                            if (si >= 0) size = c.getLong(si);
                        }
                    }
                    boolean ok = TransferService.sendToPeer(peer, u, name, size);
                    if (!ok) allOk = false;
                }
                if (allOk) okCount++; else failCount++;
            }
            final int ok2 = okCount, fail2 = failCount;
            runOnUiThread(() -> statusLine.setText("✓ " + ok2 + (fail2 > 0 ? " · ✗ " + fail2 : "")));
        });
    }
}
