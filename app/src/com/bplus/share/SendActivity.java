package com.bplus.share;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SendActivity extends Activity {
    private static final int REQ_PICK = 41;

    private LinearLayout container;
    private final List<Uri> uris = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final List<Long> sizes = new ArrayList<>();
    private String batchId;
    private String peerName = "", pairedId="";
    private TextView[] fileBars;
    private ProgressBar[] fileProgress;
    private long[] fileDone;
    private TextView overallPct, overallSub;
    private ProgressBar overallBar;
    private long totalBytes, overallDone;
    private boolean[] served;
    private int state; // 0 pick, 1 qr, 2 transfer, 3 done

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.applyLocale(this);
        Ui.applyTheme(this);
        container = Ui.col(this);
        container.setBackgroundColor(Ui.bg);
        setContentView(container);
        showPick();
        TransferService.listen("send", (type, data) -> runOnUiThread(() -> onEvent(type, data)));
    }

    @Override protected void onDestroy() {
        TransferService.unlisten("send");
        super.onDestroy();
    }

    private void onEvent(String type, JSONObject data) {
        try {
            if ("pair".equals(type) && state == 1) {
                peerName = data.optString("name");pairedId=data.optString("id");
                state = 2;
                showTransfer();
            } else if ("serve-progress".equals(type) && state == 2) {
                if (!batchId.equals(data.optString("batch"))) return;
                int i = data.getInt("i");
                fileDone[i] = data.getLong("done");
                long size = data.getLong("size");
                if (fileProgress != null && fileProgress[i] != null) {
                    fileProgress[i].setProgress((int) (1000 * fileDone[i] / Math.max(1, size)));
                    if (fileDone[i] >= size && fileBars != null && fileBars[i] != null) {
                        fileBars[i].setText("✓");
                        fileBars[i].setTextColor(Ui.green);
                        if (!served[i]) { served[i] = true; overallDone += size;
                            if(!pairedId.isEmpty()){JSONObject message=new JSONObject();message.put("kind","file");message.put("name",names.get(i));message.put("size",sizes.get(i));message.put("uri",uris.get(i).toString());message.put("mime",Save.mime(names.get(i)));message.put("me",true);message.put("ts",System.currentTimeMillis());message.put("state","sent");Store.addMsg(pairedId,message);}
                        }
                    }
                }
                updateOverall(size);
            } else if ("batchdone".equals(type) && state == 2) {
                if (batchId.equals(data.optString("batch"))) { state = 3; showDone(); }
            }
        } catch (Exception ignored) {}
    }

    private void updateOverall(long currentSize) {
        long d = overallDone;
        for (int i = 0; i < fileDone.length; i++) if (!served[i]) d += fileDone[i];
        int pct = (int) (100 * d / Math.max(1, totalBytes));
        if (overallPct != null) overallPct.setText(pct + "%");
        if (overallBar != null) overallBar.setProgress(pct * 10);
        if (overallSub != null)
            overallSub.setText(Ui.fmtSize(d) + " / " + Ui.fmtSize(totalBytes));
    }

    // ---------- state 0: pick files ----------
    private void showPick() {
        state = 0;
        container.removeAllViews();
        container.addView(Ui.topBar(this, getString(R.string.select_files), true, null));
        if (uris.isEmpty()) {
            TextView hint = Ui.t(this, getString(R.string.send_sub), 14, Ui.dim, App.cairoSB);
            hint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
            lp.topMargin = Ui.dp(this, 120);
            container.addView(hint, lp);
        } else {
            LinearLayout list = Ui.col(this);
            Ui.pad(list, 16, 4, 16, 0, this);
            for (int i = 0; i < names.size(); i++) {
                LinearLayout r = Ui.row(this);
                Ui.pad(r, 8, 10, 8, 10, this);
                TextView ic = Ui.t(this, ext(names.get(i)), 11, Color.WHITE, App.cairoB);
                ic.setGravity(Gravity.CENTER);
                ic.setBackground(Ui.round(Ui.accent, 12, this));
                r.addView(ic, Ui.lp(Ui.dp(this, 42), Ui.dp(this, 42)));
                LinearLayout meta = Ui.col(this);
                Ui.pad(meta, 13, 0, 13, 0, this);
                TextView n = Ui.t(this, names.get(i), 14.5f, Ui.text, App.cairoB);
                n.setSingleLine(true);
                meta.addView(n);
                meta.addView(Ui.t(this, Ui.fmtSize(sizes.get(i)), 11.5f, Ui.dim, App.cairoSB));
                r.addView(meta, Ui.lpw(1));
                list.addView(r);
                View div = new View(this);
                div.setBackgroundColor(Ui.border);
                list.addView(div, Ui.lp(-1, Ui.dp(this, 1)));
            }
            container.addView(new android.widget.ScrollView(this) {{ addView(list); }},
                    Ui.lpW1());
        }
        LinearLayout bottom = Ui.row(this);
        bottom.setBackgroundColor(Ui.bar);
        Ui.pad(bottom, 20, 14, 20, 22, this);
        TextView pick = Ui.ghost(this, "+ " + getString(R.string.select_files));
        pick.setOnClickListener(v -> pickFiles());
        bottom.addView(pick);
        LinearLayout info = Ui.col(this);
        Ui.pad(info, 16, 0, 16, 0, this);
        long total = 0;
        for (long s : sizes) total += s;
        info.addView(Ui.t(this, getString(R.string.files_selected, uris.size()), 13.5f, Ui.text, App.cairoB));
        info.addView(Ui.t(this, Ui.fmtSize(total), 11, Ui.dim, App.cairoSB));
        bottom.addView(info, Ui.lpw(1));
        TextView next = Ui.cta(this, getString(R.string.next));
        next.setOnClickListener(v -> { if (!uris.isEmpty()) showQr(); });
        bottom.addView(next);
        container.addView(bottom);
    }

    private void pickFiles() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_PICK);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK && res == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++)
                    addUri(data.getClipData().getItemAt(i).getUri(),data.getFlags());
            } else if (data.getData() != null) addUri(data.getData(),data.getFlags());
            showPick();
        }
    }

    private void addUri(Uri u,int flags) {
        try { getContentResolver().takePersistableUriPermission(u, flags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION)); } catch (Exception ignored) {}
        uris.add(u);
        String name = "file"; long size = 0;
        try (Cursor c = getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int si = c.getColumnIndex(OpenableColumns.SIZE);
                if (ni >= 0) name = c.getString(ni);
                if (si >= 0) size = c.getLong(si);
            }
        }
        names.add(name); sizes.add(size);
    }

    // ---------- state 1: QR ----------
    private void showQr() {
        if(Net.localIp(this)==null){PairUi.setup(this,container,this::showQr);return;}
        state = 1;
        TransferService.start(this);
        container.removeAllViews();
        batchId = UUID.randomUUID().toString().substring(0, 8);
        TransferService.Batch batch = new TransferService.Batch();
        batch.id = batchId;
        try {
            for (int i = 0; i < uris.size(); i++) {
                JSONObject f = new JSONObject();
                f.put("i", i); f.put("name", names.get(i)); f.put("size", (long) sizes.get(i));
                batch.files.put(f);
                batch.uris.put(String.valueOf(i), uris.get(i));
            }
        } catch (Exception ignored) {}
        TransferService.batches.put(batchId, batch);

        container.addView(Ui.topBar(this, getString(R.string.show_code), true, null));
        LinearLayout wrap = Ui.col(this);
        wrap.setGravity(Gravity.CENTER_HORIZONTAL);
        String ip = Net.localIp(this);
        JSONObject payload = new JSONObject();
        try {
            payload.put("v", 1); payload.put("app", "bplus-share");
            payload.put("id", Store.get("deviceId", ""));
            payload.put("name", Profile.local().optString("name"));payload.put("username",Profile.local().optString("username"));
            payload.put("ip", ip); payload.put("port", Store.PORT);
            payload.put("token", Store.token());
            payload.put("batch", batchId);
        } catch (Exception ignored) {}
        ImageView qr = new ImageView(this);
        Bitmap bm = Qr.encode(payload.toString(), Ui.dp(this, 232));
        qr.setImageBitmap(bm);
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(Color.WHITE, 22, this));
        Ui.pad(card, 16, 16, 16, 16, this);
        card.addView(qr);
        LinearLayout.LayoutParams clp = Ui.lp(-2, -2);
        clp.topMargin = Ui.dp(this, 26);
        wrap.addView(card, clp);
        TextView hint = Ui.t(this, getString(R.string.qr_hint), 14, Ui.text, App.cairoB);
        hint.setGravity(Gravity.CENTER);
        Ui.pad(hint, 30, 18, 30, 0, this);
        wrap.addView(hint);
        TextView wait = Ui.t(this, getString(R.string.waiting), 12, Ui.dim, App.cairoSB);
        Ui.pad(wait, 0, 6, 0, 0, this);
        wrap.addView(wait);
        TextView chip = Ui.t(this, getString(R.string.this_phone, Store.deviceName()), 12.5f, Ui.dim, App.cairoB);
        chip.setBackground(Ui.round(Ui.card, 12, this));
        Ui.pad(chip, 16, 8, 16, 8, this);
        LinearLayout.LayoutParams chlp = Ui.lp(-2, -2);
        chlp.topMargin = Ui.dp(this, 16);
        wrap.addView(chip, chlp);
        container.addView(wrap, Ui.lpW1());
        TextView cancel = Ui.ghost(this, getString(R.string.cancel));
        cancel.setOnClickListener(v -> finish());
        LinearLayout bottom = Ui.row(this);
        bottom.setGravity(Gravity.CENTER);
        bottom.setBackgroundColor(Ui.bar);
        Ui.pad(bottom, 20, 14, 20, 22, this);
        bottom.addView(cancel);
        container.addView(bottom);
    }

    // ---------- state 2: transfer ----------
    private void showTransfer() {
        container.removeAllViews();
        container.addView(Ui.topBar(this, getString(R.string.sending), false, null));
        LinearLayout overall = Ui.col(this);
        overall.setBackground(Ui.roundBorder(Ui.card, Ui.border, 20, this));
        Ui.pad(overall, 18, 18, 18, 18, this);
        LinearLayout orow = Ui.row(this);
        orow.addView(Ui.t(this, getString(R.string.overall) + (peerName.isEmpty() ? "" : " → " + peerName), 13, Ui.text, App.cairoB), Ui.lpw(1));
        overallPct = Ui.t(this, "0%", 15, Ui.accent, App.cairoB);
        orow.addView(overallPct);
        overall.addView(orow);
        overallBar = Ui.hbar(this, 10);
        LinearLayout.LayoutParams blp = Ui.lp(-1, Ui.dp(this, 10));
        blp.topMargin = Ui.dp(this, 12);
        overall.addView(overallBar, blp);
        overallSub = Ui.t(this, "", 11.5f, Ui.dim, App.cairoSB);
        Ui.pad(overallSub, 0, 8, 0, 0, this);
        overall.addView(overallSub);
        LinearLayout.LayoutParams olp = Ui.lp(-1, -2);
        olp.setMargins(Ui.dp(this, 22), Ui.dp(this, 8), Ui.dp(this, 22), 0);
        container.addView(overall, olp);

        totalBytes = 0;
        for (long s : sizes) totalBytes += s;
        overallDone = 0;
        int n = uris.size();
        fileBars = new TextView[n];
        fileProgress = new ProgressBar[n];
        fileDone = new long[n];
        served = new boolean[n];
        LinearLayout list = Ui.col(this);
        Ui.pad(list, 16, 10, 16, 0, this);
        for (int i = 0; i < n; i++) {
            LinearLayout r = Ui.row(this);
            Ui.pad(r, 8, 10, 8, 10, this);
            TextView ic = Ui.t(this, ext(names.get(i)), 11, Color.WHITE, App.cairoB);
            ic.setGravity(Gravity.CENTER);
            ic.setBackground(Ui.round(Ui.accent, 12, this));
            r.addView(ic, Ui.lp(Ui.dp(this, 42), Ui.dp(this, 42)));
            LinearLayout meta = Ui.col(this);
            Ui.pad(meta, 13, 0, 13, 0, this);
            TextView nm = Ui.t(this, names.get(i), 14.5f, Ui.text, App.cairoB);
            nm.setSingleLine(true);
            meta.addView(nm);
            ProgressBar pb = Ui.hbar(this, 6);
            LinearLayout.LayoutParams plp = Ui.lp(-1, Ui.dp(this, 6));
            plp.topMargin = Ui.dp(this, 7);
            meta.addView(pb, plp);
            fileProgress[i] = pb;
            r.addView(meta, Ui.lpw(1));
            TextView st = Ui.t(this, "0%", 12.5f, Ui.accent, App.cairoB);
            fileBars[i] = st;
            r.addView(st);
            list.addView(r);
        }
        container.addView(new android.widget.ScrollView(this) {{ addView(list); }},
                Ui.lpW1());

        LinearLayout bottom = Ui.row(this);
        bottom.setBackgroundColor(Ui.bar);
        Ui.pad(bottom, 20, 14, 20, 22, this);
        LinearLayout info = Ui.col(this);
        info.addView(Ui.t(this, getString(R.string.keep_open), 13.5f, Ui.text, App.cairoB));
        info.addView(Ui.t(this, getString(R.string.dont_lock), 11, Ui.dim, App.cairoSB));
        bottom.addView(info, Ui.lpw(1));
        TextView cancel = Ui.ghost(this, getString(R.string.cancel));
        cancel.setOnClickListener(v -> finish());
        bottom.addView(cancel);
        container.addView(bottom);
    }

    // ---------- state 3: done ----------
    private void showDone() {
        container.removeAllViews();
        LinearLayout wrap = Ui.col(this);
        wrap.setGravity(Gravity.CENTER);
        wrap.setLayoutParams(Ui.lp(-1, -1));
        TextView check = Ui.t(this, "✓", 44, Ui.green, App.cairoB);
        check.setGravity(Gravity.CENTER);
        check.setBackground(Ui.round(Color.argb(30, Color.red(Ui.green), Color.green(Ui.green), Color.blue(Ui.green)), 48, this));
        wrap.addView(check, Ui.lp(Ui.dp(this, 96), Ui.dp(this, 96)));
        TextView t = Ui.t(this, getString(R.string.done_title), 20, Ui.text, App.cairoB);
        LinearLayout.LayoutParams tlp = Ui.lp(-2, -2);
        tlp.topMargin = Ui.dp(this, 20);
        wrap.addView(t, tlp);
        wrap.addView(Ui.t(this, getString(R.string.done_sub_sent, uris.size(), Ui.fmtSize(totalBytes)), 13, Ui.dim, App.cairoSB));
        TextView done = Ui.cta(this, getString(R.string.done));
        LinearLayout.LayoutParams dlp = Ui.lp(-2, -2);
        dlp.topMargin = Ui.dp(this, 34);
        wrap.addView(done, dlp);
        done.setOnClickListener(v -> finish());
        container.addView(wrap);
    }

    private static String ext(String name) {
        int i = name.lastIndexOf('.');
        String e = i >= 0 ? name.substring(i + 1).toUpperCase() : "FILE";
        return e.length() > 4 ? e.substring(0, 4) : e;
    }
}
