package com.bplus.share;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("deprecation")
public class ScanActivity extends Activity implements TextureView.SurfaceTextureListener {
    private Camera camera;
    private TextureView preview;
    private LinearLayout container;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean handled;
    private boolean qrConfirmed;
    private JSONObject payload;
    private String pairError="";
    private JSONArray files;
    private String peerId = "", peerName = "", peerToken = "";
    private TextView overallPct, overallSub;
    private ProgressBar overallBar;
    private ProgressBar[] bars;
    private TextView[] marks;
    private long[] done;
    private long totalBytes, finishedBytes;
    private int state; // 0 scan, 1 connecting, 2 receiving, 3 done, 4 paired

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.applyLocale(this);
        Ui.applyTheme(this);
        container = Ui.col(this);
        container.setBackgroundColor(Ui.bg);
        setContentView(container);
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 5);
        else showScan();
    }

    @Override public void onRequestPermissionsResult(int req, String[] p, int[] r) {
        if (req == 5 && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) showScan();
        else {
            TextView t = Ui.t(this, getString(R.string.perm_camera), 14, Ui.text, App.cairoB);
            t.setGravity(Gravity.CENTER);
            Ui.pad(t, 30, 200, 30, 0, this);
            container.addView(t);
        }
    }

    // ---------- scanning ----------
    private void showScan() {
        if(Net.localIp(this)==null){PairUi.setup(this,container,this::showScan);return;}
        handled=false;
        container.removeAllViews();
        LinearLayout top = Ui.topBar(this, getString(R.string.scan_title), true, null);
        container.addView(top);
        preview = new TextureView(this);
        preview.setSurfaceTextureListener(this);
        container.addView(preview, Ui.lpW1());
        TextView hint = Ui.t(this, getString(R.string.scan_hint), 13.5f, Ui.text, App.cairoB);
        hint.setGravity(Gravity.CENTER);
        Ui.pad(hint, 40, 16, 40, 30, this);
        container.addView(hint);
    }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) { startCamera(); }
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { stopCamera(); return true; }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture st) {}

    private void startCamera() {
        exec.execute(() -> {
            try {
                camera = Camera.open();
                Camera.Parameters p = camera.getParameters();
                Camera.Size best = p.getSupportedPreviewSizes().get(0);
                int bestScore = Integer.MAX_VALUE;
                for (Camera.Size s : p.getSupportedPreviewSizes()) {
                    int score = Math.abs(s.width - 1280) + Math.abs(s.height - 720);
                    if (score < bestScore) { bestScore = score; best = s; }
                }
                p.setPreviewSize(best.width, best.height);
                try { p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO); } catch (Exception ignored) {}
                camera.setParameters(p);
                final int pw = best.width, ph = best.height;
                camera.setDisplayOrientation(90);
                camera.setPreviewTexture(preview.getSurfaceTexture());
                camera.setPreviewCallback((data, cam) -> {
                    if (handled) return;
                    String s = Qr.decodeYUV(data, pw, ph);
                    if (s != null && !handled) {
                        handled = true;
                        main.post(() -> onQr(s));
                    }
                });
                camera.startPreview();
            } catch (Exception e) {
                main.post(() -> {
                    TextView t = Ui.t(this, getString(R.string.perm_camera), 14, Ui.text, App.cairoB);
                    t.setGravity(Gravity.CENTER);
                    container.addView(t);
                });
            }
        });
    }

    private void stopCamera() {
        try {
            if (camera != null) { camera.setPreviewCallback(null); camera.stopPreview(); camera.release(); }
        } catch (Exception ignored) {}
        camera = null;
    }

    @Override protected void onDestroy() {
        stopCamera();
        super.onDestroy();
    }

    // ---------- after scan ----------
    private void onQr(String text) {
        try {
            JSONObject o = new JSONObject(text);
            if (!"bplus-share".equals(o.optString("app"))) throw new Exception("not ours");
            payload = o;
        } catch (Exception e) {
            handled = false;
            return;
        }
        if(!qrConfirmed){stopCamera();new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.accept_pair)).setMessage(payload.optString("name")+"\n@"+payload.optString("username")+"\n"+getString(R.string.identity_notice)).setNegativeButton(getString(R.string.cancel),(d,n)->{handled=false;showScan();}).setPositiveButton(getString(R.string.accept_pair),(d,n)->{qrConfirmed=true;onQr(payload.toString());}).setOnCancelListener(d->{handled=false;showScan();}).show();return;}
        state = 1;
        stopCamera();
        container.removeAllViews();
        container.addView(Ui.topBar(this, getString(R.string.connected), true, null));
        TextView wait = Ui.t(this, getString(R.string.waiting), 14, Ui.dim, App.cairoSB);
        wait.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = Ui.dp(this, 150);
        container.addView(wait, lp);

        exec.execute(() -> {
            String ip = payload.optString("ip");
            int port = payload.optInt("port", Store.PORT);
            String qrToken = payload.optString("token");
            JSONObject res = Net.pair(ip, port, qrToken);
            if (res == null) { pairError=Net.lastError(); main.post(this::failPair); return; }
            peerId = res.optString("id");
            peerName = res.optString("name");
            peerToken = qrToken;
            try {
                JSONObject peer = Profile.clean(res);
                peer.put("id", peerId);
                peer.put("name", peerName);
                peer.put("ip", ip);
                peer.put("port", port);
                peer.put("token", qrToken);
                peer.put("lastSeen", System.currentTimeMillis());
                Store.upsertPeer(peer);
            } catch (Exception ignored) {}
            // History migration requires the explicit action on the paired screen.
            String batch = payload.optString("batch", "");
            if (!batch.isEmpty()) {
                JSONArray mf = Net.manifest(ip, port, qrToken, batch);
                if (mf == null) { pairError=Net.lastError(); main.post(this::failPair); return; }
                files = mf;
                main.post(this::showReceive);
                downloadAll(ip, port, qrToken, batch);
            } else {
                main.post(this::showPaired);
            }
        });
    }

    private boolean maybeMoveHistory(String ip, int port, String token, String newPeerId) {
        try {
            boolean isNew = Store.thread(newPeerId).length() == 0;
            if (!isNew) return false;
            JSONObject h = Net.history(ip, port, token);
            if (h == null) return false;
            JSONObject threads = h.optJSONObject("threads");
            if (threads == null) return false;
            java.util.Iterator<String> keys = threads.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                if (k.equals(Store.get("deviceId", ""))) continue;
                JSONArray t = threads.getJSONArray(k);
                for(int j=0;j<t.length();j++){JSONObject item=t.optJSONObject(j);if(item!=null)item.remove("uri");}
                if (Store.thread(k).length() == 0) Store.saveThread(k, t);
            }
            return true;
        } catch (Exception ignored) {return false;}
    }

    private void failPair() {
        PairUi.failure(this,container,payload.optString("name",getString(R.string.target_device)),payload.optString("ip"),payload.optInt("port",Store.PORT),pairError,()->{if(Net.localIp(this)==null)PairUi.setup(this,container,()->onQr(payload.toString()));else onQr(payload.toString());});
    }

    // ---------- paired only ----------
    private void showPaired() {
        state = 4;
        container.removeAllViews();
        LinearLayout wrap = Ui.col(this);
        wrap.setGravity(Gravity.CENTER);
        TextView check = Ui.t(this, "✓", 44, Ui.green, App.cairoB);
        check.setGravity(Gravity.CENTER);
        check.setBackground(Ui.round(Color.argb(30, Color.red(Ui.green), Color.green(Ui.green), Color.blue(Ui.green)), 48, this));
        wrap.addView(check, Ui.lp(Ui.dp(this, 96), Ui.dp(this, 96)));
        TextView t = Ui.t(this, getString(R.string.phones_paired), 18, Ui.text, App.cairoB);
        LinearLayout.LayoutParams tlp = Ui.lp(-2, -2);
        tlp.topMargin = Ui.dp(this, 22);
        wrap.addView(t, tlp);
        wrap.addView(Ui.t(this, peerName + " · " + getString(R.string.direct_note), 12.5f, Ui.dim, App.cairoSB));
        TextView done = Ui.cta(this, getString(R.string.done));
        LinearLayout.LayoutParams dlp = Ui.lp(-2, -2);
        dlp.topMargin = Ui.dp(this, 34);
        wrap.addView(done, dlp);
        done.setOnClickListener(v -> finish());
        TextView migrate=Ui.ghost(this,getString(R.string.move_history));
        Ui.pad(migrate,10,20,10,20,this);wrap.addView(migrate);
        migrate.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.move_history)).setMessage(getString(R.string.history_confirm,peerName)).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.copy_history),(dialog,which)->{
            migrate.setEnabled(false);migrate.setText(getString(R.string.checking));
            exec.execute(()->{boolean ok=maybeMoveHistory(payload.optString("ip"),payload.optInt("port",Store.PORT),peerToken,peerId);main.post(()->{migrate.setEnabled(true);migrate.setText(getString(R.string.move_history));android.widget.Toast.makeText(this,getString(ok?R.string.history_copied:R.string.history_failed),android.widget.Toast.LENGTH_LONG).show();});});
        }).show());
        container.addView(wrap, Ui.lp(-1, -1));
    }

    // ---------- receiving ----------
    private void showReceive() {
        state = 2;
        container.removeAllViews();
        container.addView(Ui.topBar(this, getString(R.string.receiving), false, null));
        LinearLayout overall = Ui.col(this);
        overall.setBackground(Ui.roundBorder(Ui.card, Ui.border, 20, this));
        Ui.pad(overall, 18, 18, 18, 18, this);
        LinearLayout orow = Ui.row(this);
        orow.addView(Ui.t(this, getString(R.string.overall) + " · " + peerName, 13, Ui.text, App.cairoB), Ui.lpw(1));
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

        int n = files.length();
        bars = new ProgressBar[n];
        marks = new TextView[n];
        done = new long[n];
        totalBytes = 0;
        LinearLayout list = Ui.col(this);
        Ui.pad(list, 16, 10, 16, 0, this);
        try {
            for (int i = 0; i < n; i++) {
                JSONObject f = files.getJSONObject(i);
                totalBytes += f.getLong("size");
                LinearLayout r = Ui.row(this);
                Ui.pad(r, 8, 10, 8, 10, this);
                TextView ic = Ui.t(this, ext(f.getString("name")), 11, Color.WHITE, App.cairoB);
                ic.setGravity(Gravity.CENTER);
                ic.setBackground(Ui.round(Ui.accent, 12, this));
                r.addView(ic, Ui.lp(Ui.dp(this, 42), Ui.dp(this, 42)));
                LinearLayout meta = Ui.col(this);
                Ui.pad(meta, 13, 0, 13, 0, this);
                TextView nm = Ui.t(this, f.getString("name"), 14.5f, Ui.text, App.cairoB);
                nm.setSingleLine(true);
                meta.addView(nm);
                ProgressBar pb = Ui.hbar(this, 6);
                LinearLayout.LayoutParams plp = Ui.lp(-1, Ui.dp(this, 6));
                plp.topMargin = Ui.dp(this, 7);
                meta.addView(pb, plp);
                bars[i] = pb;
                r.addView(meta, Ui.lpw(1));
                TextView st = Ui.t(this, "0%", 12.5f, Ui.accent, App.cairoB);
                marks[i] = st;
                r.addView(st);
                list.addView(r);
            }
        } catch (Exception ignored) {}
        container.addView(new android.widget.ScrollView(this) {{ addView(list); }},
                Ui.lpW1());
        LinearLayout bottom = Ui.row(this);
        bottom.setBackgroundColor(Ui.bar);
        Ui.pad(bottom, 20, 14, 20, 22, this);
        LinearLayout info = Ui.col(this);
        info.addView(Ui.t(this, getString(R.string.keep_open), 13.5f, Ui.text, App.cairoB));
        info.addView(Ui.t(this, getString(R.string.dont_lock), 11, Ui.dim, App.cairoSB));
        bottom.addView(info, Ui.lpw(1));
        container.addView(bottom);
    }

    private void downloadAll(String ip, int port, String token, String batch) {
        exec.execute(() -> {
            int n = files.length();
            for (int i = 0; i < n; i++) {
                try {
                    JSONObject f = files.getJSONObject(i);
                    String name = f.getString("name");
                    long size = f.getLong("size");
                    final int idx = i;
                    boolean ok = Net.download(ip, port, token, batch, i, name, size, this, (d, tot) -> {
                        done[idx] = d;
                        main.post(() -> {
                            if (bars[idx] != null) bars[idx].setProgress((int) (1000 * d / Math.max(1, tot)));
                            if (marks[idx] != null) marks[idx].setText((int) (100 * d / Math.max(1, tot)) + "%");
                            updateOverall();
                        });
                    });
                    if (ok) {
                        finishedBytes += size;
                        JSONObject msg = new JSONObject();
                        msg.put("kind", "file"); msg.put("name", name); msg.put("size", size);
                        msg.put("uri",Net.receivedUri());msg.put("mime",Save.mime(name));
                        msg.put("me", false); msg.put("ts", System.currentTimeMillis()); msg.put("state", "received");
                        Store.addMsg(peerId, msg);
                        main.post(() -> {
                            if (marks[idx] != null) { marks[idx].setText("✓"); marks[idx].setTextColor(Ui.green); }
                            updateOverall();
                        });
                    }
                } catch (Exception ignored) {}
            }
            batchDone(ip, port, token, batch);
            main.post(this::showDone);
        });
    }

    private void batchDone(String ip, int port, String token, String batch) {
        try {
            java.net.HttpURLConnection h = (java.net.HttpURLConnection)
                    new java.net.URL("http://" + ip + ":" + port + "/batchdone?token=" + token + "&batch=" + batch).openConnection();
            h.setConnectTimeout(5000);
            h.getResponseCode();
        } catch (Exception ignored) {}
    }

    private void updateOverall() {
        long d = finishedBytes;
        for (long x : done) d += x;
        int pct = (int) (100 * d / Math.max(1, totalBytes));
        if (overallPct != null) overallPct.setText(pct + "%");
        if (overallBar != null) overallBar.setProgress(pct * 10);
        if (overallSub != null) overallSub.setText(Ui.fmtSize(d) + " / " + Ui.fmtSize(totalBytes));
    }

    private void showDone() {
        state = 3;
        container.removeAllViews();
        LinearLayout wrap = Ui.col(this);
        wrap.setGravity(Gravity.CENTER);
        TextView check = Ui.t(this, "✓", 44, Ui.green, App.cairoB);
        check.setGravity(Gravity.CENTER);
        check.setBackground(Ui.round(Color.argb(30, Color.red(Ui.green), Color.green(Ui.green), Color.blue(Ui.green)), 48, this));
        wrap.addView(check, Ui.lp(Ui.dp(this, 96), Ui.dp(this, 96)));
        TextView t = Ui.t(this, getString(R.string.done_title), 20, Ui.text, App.cairoB);
        LinearLayout.LayoutParams tlp = Ui.lp(-2, -2);
        tlp.topMargin = Ui.dp(this, 20);
        wrap.addView(t, tlp);
        int n = files != null ? files.length() : 0;
        wrap.addView(Ui.t(this, getString(R.string.done_sub, n, Ui.fmtSize(totalBytes)), 13, Ui.dim, App.cairoSB));
        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams rlp = Ui.lp(-2, -2);
        rlp.topMargin = Ui.dp(this, 34);
        wrap.addView(row, rlp);
        TextView done = Ui.cta(this, getString(R.string.done));
        row.addView(done);
        done.setOnClickListener(v -> finish());
        container.addView(wrap, Ui.lp(-1, -1));
    }

    private static String ext(String name) {
        int i = name.lastIndexOf('.');
        String e = i >= 0 ? name.substring(i + 1).toUpperCase() : "FILE";
        return e.length() > 4 ? e.substring(0, 4) : e;
    }
}
