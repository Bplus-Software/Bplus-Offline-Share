package com.bplus.share;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;
import java.util.concurrent.Executors;

public class DevicesActivity extends Activity {
    private LinearLayout listBox, groupsBox;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.applyLocale(this);
        Ui.applyTheme(this);
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.bg);
        root.addView(Ui.topBar(this, getString(R.string.devices_groups), true, null));
        root.addView(Ui.sub(this, getString(R.string.pair_hint)));

        LinearLayout content = Ui.col(this);
        listBox = Ui.col(this);
        Ui.pad(listBox, 16, 4, 16, 0, this);
        content.addView(listBox);

        content.addView(Ui.secHead(this, getString(R.string.groups)));
        groupsBox = Ui.col(this);
        Ui.pad(groupsBox, 16, 0, 16, 10, this);
        content.addView(groupsBox);

        ScrollView sc = new ScrollView(this);
        sc.addView(content);
        root.addView(sc, Ui.lpW1());

        LinearLayout bottom = Ui.row(this);
        bottom.setBackgroundColor(Ui.bar);
        Ui.pad(bottom, 20, 14, 20, 22, this);
        TextView pair = Ui.cta(this, getString(R.string.pair_new));
        pair.setOnClickListener(v -> showPairOptions());
        bottom.addView(pair, Ui.lpw(1));
        TextView ng = Ui.ghost(this, getString(R.string.new_group));
        LinearLayout.LayoutParams nlp = Ui.lp(-2, -2);
        nlp.setMarginStart(Ui.dp(this, 10));
        bottom.addView(ng, nlp);
        ng.setOnClickListener(v -> showNewGroupDialog());
        root.addView(bottom);
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();TransferService.listen("device-profile",(type,data)->runOnUiThread(()->{if(!isFinishing())refreshRowsOnly();}));
        refresh();
    }

    private void refresh() {
        listBox.removeAllViews();
        JSONArray peers = Store.listedPeers();
        if (peers.length() == 0) {
            TextView none = Ui.t(this, "—", 14, Ui.dim, App.cairoSB);
            none.setGravity(Gravity.CENTER);
            listBox.addView(none);
        }
        for (int i = 0; i < peers.length(); i++) {
            JSONObject peer = peers.optJSONObject(i);
            if (peer == null) continue;
            listBox.addView(peerRow(peer));
        }
        groupsBox.removeAllViews();
        JSONArray groups = Store.groups();
        if (groups.length() == 0) {
            TextView none = Ui.t(this, getString(R.string.no_groups), 13, Ui.dim, App.cairoSB);
            none.setGravity(Gravity.CENTER);
            groupsBox.addView(none);
        }
        for (int i = 0; i < groups.length(); i++) {
            JSONObject g = groups.optJSONObject(i);
            if (g == null) continue;
            groupsBox.addView(groupRow(g));
        }
        // refresh online status
        Executors.newSingleThreadExecutor().execute(() -> {
            for (int i = 0; i < peers.length(); i++) {
                JSONObject peer = peers.optJSONObject(i);
                if (peer == null) continue;
                boolean on = Net.ping(peer.optString("ip"), peer.optInt("port", Store.PORT), peer.optString("token")) != null;
                try { peer.put("online", on); } catch (Exception ignored) {}
            }
            runOnUiThread(() -> { if (!isFinishing()) refreshRowsOnly(); });
        });
    }

    private void refreshRowsOnly() {
        // simple approach: rebuild (cheap for a small list)
        listBox.removeAllViews();
        JSONArray peers = Store.listedPeers();
        for (int i = 0; i < peers.length(); i++) {
            JSONObject peer = peers.optJSONObject(i);
            if (peer != null) listBox.addView(peerRow(peer));
        }
    }

    private View peerRow(JSONObject peer) {
        LinearLayout r = Ui.col(this);
        r.setBackground(Ui.roundBorder(Ui.card, Ui.border, 16, this));
        Ui.pad(r, 12, 12, 12, 12, this);
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);lp.bottomMargin = Ui.dp(this, 8);r.setLayoutParams(lp);
        LinearLayout identity=Ui.row(this);r.addView(identity,Ui.lp(-1,-2));
        TextView ic=Ui.t(this,"📱",20,Ui.dim,App.cairoSB);ic.setGravity(Gravity.CENTER);identity.addView(ic,Ui.lp(Ui.dp(this,42),Ui.dp(this,42)));
        LinearLayout meta=Ui.col(this);Ui.pad(meta,12,0,0,0,this);
        TextView title=Ui.t(this,peer.optString("name"),16,Ui.text,App.cairoB);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);meta.addView(title,Ui.lp(-1,-2));
        boolean unpaired=peer.optBoolean("unpaired"),on=peer.optBoolean("online");TextView st=Ui.t(this,unpaired?getString(R.string.not_paired):"● "+getString(on?R.string.online_wifi:R.string.not_nearby),12,unpaired?0xffff6565:on?Ui.green:Ui.dim,App.cairoSB);st.setSingleLine(true);st.setEllipsize(android.text.TextUtils.TruncateAt.END);meta.addView(st,Ui.lp(-1,-2));identity.addView(meta,Ui.lpw(1));
        if(unpaired){st.setBackground(Ui.roundBorder(Ui.bg,0xffff6565,3,this));Ui.pad(st,6,1,6,1,this);}
        LinearLayout actions=Ui.row(this);Ui.pad(actions,0,10,0,0,this);r.addView(actions,Ui.lp(-1,-2));
        TextView send=Ui.t(this,getString(unpaired?R.string.pair_device:R.string.send),13,Color.WHITE,App.cairoB);send.setGravity(Gravity.CENTER);send.setSingleLine(true);send.setEllipsize(android.text.TextUtils.TruncateAt.END);send.setBackground(Ui.round(Ui.accent,12,this));send.setEnabled(true);send.setAlpha(1f);actions.addView(send,new LinearLayout.LayoutParams(0,Ui.dp(this,44),1));
        send.setOnClickListener(v->{if(unpaired){Repair.open(this,peer.optString("id"));return;}Intent i=new Intent(this,ChatActivity.class);i.putExtra("peer",peer.optString("id"));i.putExtra("attach",true);startActivity(i);});
        if(!unpaired){TextView settings=Ui.icon(this,"⚙",getString(R.string.device_settings));LinearLayout.LayoutParams settingsLp=Ui.lp(Ui.dp(this,44),Ui.dp(this,44));settingsLp.setMarginStart(Ui.dp(this,8));actions.addView(settings,settingsLp);settings.setOnClickListener(v->DeviceSettingsActivity.open(this,peer.optString("id")));}
        TextView menu=Ui.icon(this,"⋮",getString(R.string.device_actions));actions.addView(menu);menu.setOnClickListener(v->DeviceRemoval.menu(this,peer.optString("id"),this::refresh));

        r.setOnClickListener(v -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("peer", peer.optString("id"));
            startActivity(i);
        });
        return r;
    }

    private View groupRow(JSONObject g) {
        LinearLayout r = Ui.row(this);
        r.setBackground(Ui.roundBorder(Ui.card, Ui.border, 16, this));
        Ui.pad(r, 14, 14, 14, 14, this);
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.bottomMargin = Ui.dp(this, 8);
        r.setLayoutParams(lp);
        TextView ic = Ui.t(this, "▦", 18, Ui.accent, App.cairoB);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(Ui.round(Ui.field, 12, this));
        r.addView(ic, Ui.lp(Ui.dp(this, 42), Ui.dp(this, 42)));
        LinearLayout meta = Ui.col(this);
        Ui.pad(meta, 13, 0, 13, 0, this);
        meta.addView(Ui.t(this, g.optString("name"), 14.5f, Ui.text, App.cairoB));
        JSONArray m = g.optJSONArray("members");
        meta.addView(Ui.t(this, (m != null ? m.length() : 0) + " · " + getString(R.string.send_to_group_sub), 11.5f, Ui.dim, App.cairoSB));
        r.addView(meta, Ui.lpw(1));
        r.setOnClickListener(v -> {
            Intent i = new Intent(this, GroupActivity.class);
            i.putExtra("group", g.optString("id"));
            startActivity(i);
        });
        return r;
    }

    private void showPairOptions() {
        String[] items = {getString(R.string.show_my_code), getString(R.string.scan_code)};
        new AppDialog(this)
                .setTitle(getString(R.string.pair_title))
                .setItems(items, (d, which) -> {
                    if (which == 0) showMyCode();
                    else startActivity(new Intent(this, ScanActivity.class));
                }).show();
    }

    private void showMyCode() {
        if(!PairUi.ensure(this,this::showMyCode))return;
        TransferService.start(this);
        LinearLayout wrap = Ui.col(this);
        wrap.setGravity(Gravity.CENTER);
        Ui.pad(wrap, 24, 20, 24, 20, this);
        ImageView qr = new ImageView(this);
        JSONObject payload = new JSONObject();
        try {
            payload.put("v", 1); payload.put("app", "bplus-share");payload.put("pairEpoch",Recall.secret());
            payload.put("id", Store.get("deviceId", ""));
            payload.put("name", Profile.local().optString("name"));payload.put("username",Profile.local().optString("username"));
            payload.put("ip", Net.localIp(this)); payload.put("port", Store.PORT);
            payload.put("token", Store.token());
        } catch (Exception ignored) {}
        Bitmap bm = Qr.encode(payload.toString(), Ui.dp(this, 220));
        qr.setImageBitmap(bm);
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(Color.WHITE, 18, this));
        Ui.pad(card, 14, 14, 14, 14, this);
        card.addView(qr);
        wrap.addView(card);
        TextView hint = Ui.t(this, getString(R.string.qr_hint), 13, Ui.dim, App.cairoSB);
        hint.setGravity(Gravity.CENTER);
        Ui.pad(hint, 0, 14, 0, 0, this);
        wrap.addView(hint);
        new AppDialog(this).setView(wrap).show();
    }

    private void showNewGroupDialog() {
        JSONArray peers = Store.peers();
        LinearLayout wrap = Ui.col(this);
        Ui.pad(wrap, 20, 16, 20, 4, this);
        EditText name = new EditText(this);
        name.setHint(getString(R.string.group_name_hint));
        name.setTypeface(App.cairoSB);
        wrap.addView(name, Ui.lp(-1, -2));
        final boolean[] checked = new boolean[peers.length()];
        for (int i = 0; i < peers.length(); i++) {
            JSONObject p = peers.optJSONObject(i);
            if (p == null) continue;
            android.widget.CheckBox cb = new android.widget.CheckBox(this);
            cb.setText(p.optString("name"));
            cb.setTypeface(App.cairoSB);
            final int idx = i;
            cb.setOnCheckedChangeListener((v, on) -> checked[idx] = on);
            wrap.addView(cb, Ui.lp(-1, -2));
        }
        new AppDialog(this)
                .setTitle(getString(R.string.new_group))
                .setView(wrap)
                .setPositiveButton(getString(R.string.create), (d, w) -> {
                    try {
                        JSONObject g = new JSONObject();
                        g.put("id", UUID.randomUUID().toString().substring(0, 8));
                        g.put("name", name.getText().toString().trim());
                        JSONArray members = new JSONArray();
                        for (int i = 0; i < peers.length(); i++)
                            if (checked[i]) members.put(peers.getJSONObject(i).getString("id"));
                        g.put("members", members);
                        JSONArray groups = Store.groups();
                        groups.put(g);
                        Store.saveGroups(groups);
                        refresh();
                    } catch (Exception ignored) {}
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
    @Override protected void onPause(){TransferService.unlisten("device-profile");super.onPause();}

}
