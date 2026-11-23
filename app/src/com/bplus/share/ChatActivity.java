package com.bplus.share;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ChatActivity extends Activity {
    private static final int REQ_PICK = 61;
    private String peerId;
    private JSONObject peer;
    private LinearLayout msgs;
    private ScrollView scroll;
    private final java.util.concurrent.ExecutorService exec = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.applyLocale(this);
        Ui.applyTheme(this);
        peerId = getIntent().getStringExtra("peer");
        peer = Store.peerById(peerId);

        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.bg);
        String name = peer != null ? peer.optString("name") : "";
        LinearLayout head = Ui.row(this);
        Ui.pad(head, 14, 10, 14, 10, this);
        TextView back = Ui.t(this, "‹", 26, Ui.dim, App.cairoB);
        back.setGravity(Gravity.CENTER);
        head.addView(back, Ui.lp(Ui.dp(this, 38), Ui.dp(this, 38)));
        back.setOnClickListener(v -> finish());
        LinearLayout meta = Ui.col(this);
        Ui.pad(meta, 12, 0, 12, 0, this);
        meta.addView(Ui.t(this, name, 16, Ui.text, App.cairoB));
        TextView status = Ui.t(this, "", 11, Ui.green, App.cairoB);
        meta.addView(status);
        head.addView(meta, Ui.lpw(1));
        head.setBackgroundColor(Ui.bar);
        root.addView(head);
        checkOnline(status);

        msgs = Ui.col(this);
        Ui.pad(msgs, 16, 8, 16, 8, this);
        scroll = new ScrollView(this);
        scroll.addView(msgs);
        root.addView(scroll, Ui.lpW1());

        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Ui.bar);
        Ui.pad(bar, 14, 12, 14, 20, this);
        TextView attach = Ui.t(this, "＋", 19, Ui.dim, App.cairoB);
        attach.setGravity(Gravity.CENTER);
        attach.setBackground(Ui.round(Ui.field, 20, this));
        bar.addView(attach, Ui.lp(Ui.dp(this, 40), Ui.dp(this, 40)));
        attach.setOnClickListener(v -> pickFile());
        EditText input = new EditText(this);
        input.setHint(getString(R.string.message_hint));
        input.setHintTextColor(Color.rgb(94,104,120));
        input.setTextColor(Ui.text);
        input.setTypeface(App.cairoSB);
        input.setTextSize(13.5f);
        input.setBackground(Ui.round(Ui.field, 22, this));
        Ui.pad(input, 16, 10, 16, 10, this);
        LinearLayout.LayoutParams ilp = Ui.lpw(1);
        ilp.setMarginStart(Ui.dp(this, 10));
        ilp.setMarginEnd(Ui.dp(this, 10));
        bar.addView(input, ilp);
        TextView send = Ui.t(this, "➤", 16, Color.WHITE, App.cairoB);
        send.setGravity(Gravity.CENTER);
        send.setBackground(Ui.round(Ui.accent, 20, this));
        bar.addView(send, Ui.lp(Ui.dp(this, 40), Ui.dp(this, 40)));
        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            input.setText("");
            sendText(text);
        });
        root.addView(bar);
        setContentView(root);

        renderThread();
        TransferService.listen("chat", (type, data) -> {
            if (!peerId.equals(data.optString("from"))) return;
            runOnUiThread(this::renderThread);
        });
        if (getIntent().getBooleanExtra("attach", false)) pickFile();
    }

    @Override protected void onDestroy() {
        TransferService.unlisten("chat");
        super.onDestroy();
    }

    private void checkOnline(TextView status) {
        if (peer == null) return;
        exec.execute(() -> {
            boolean on = Net.ping(peer.optString("ip"), peer.optInt("port", Store.PORT), peer.optString("token")) != null;
            runOnUiThread(() -> {
                status.setText("● " + getString(on ? R.string.online_wifi : R.string.not_nearby));
                status.setTextColor(on ? Ui.green : Ui.dim);
            });
        });
    }

    private void renderThread() {
        msgs.removeAllViews();
        JSONArray t = Store.thread(peerId);
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.US);
        for (int i = 0; i < t.length(); i++) {
            JSONObject m = t.optJSONObject(i);
            if (m == null) continue;
            boolean me = m.optBoolean("me");
            String kind = m.optString("kind", "text");
            TextView bub;
            if (kind.equals("text")) {
                bub = Ui.t(this, m.optString("body"), 14, me ? Color.WHITE : Ui.text, App.cairoSB);
            } else {
                String state = m.optString("state", "");
                String label = "📎 " + m.optString("name") + "\n" + Ui.fmtSize(m.optLong("size"));
                if ("sent".equals(state)) label += " · " + getString(R.string.sent);
                else if ("failed".equals(state)) label += " · " + getString(R.string.failed_retry);
                bub = Ui.t(this, label, 13.5f, me ? Color.WHITE : Ui.text, App.cairoSB);
                if ("failed".equals(state)) {
                    final int idx = i;
                    bub.setOnClickListener(v -> retryFile(idx));
                }
            }
            bub.setLineSpacing(0, 1.25f);
            GradientDrawable bg = Ui.round(me ? Ui.accent : Ui.card, 16, this);
            bub.setBackground(bg);
            Ui.pad(bub, 13, 9, 13, 9, this);
            String time = tf.format(new Date(m.optLong("ts")));
            TextView stamp = Ui.t(this, time, 10, me ? Color.argb(160,255,255,255) : Ui.dim, App.cairoSB);
            LinearLayout cell = Ui.col(this);
            cell.setGravity(me ? Gravity.END : Gravity.START);
            LinearLayout.LayoutParams bp = Ui.lp(-2, -2);
            bp.bottomMargin = Ui.dp(this, 10);
            bp.gravity = me ? Gravity.END : Gravity.START;
            cell.addView(bub, bp);
            msgs.addView(cell);
        }
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void sendText(String text) {
        JSONObject msg = new JSONObject();
        try {
            msg.put("kind", "text"); msg.put("body", text);
            msg.put("me", true); msg.put("ts", System.currentTimeMillis()); msg.put("state", "sending");
            Store.addMsg(peerId, msg);
        } catch (Exception ignored) {}
        renderThread();
        exec.execute(() -> {
            boolean ok = peer != null && Net.sendMsg(peer.optString("ip"), peer.optInt("port", Store.PORT), peer.optString("token"), text);
            markLast(ok ? "sent" : "failed");
        });
    }

    private void markLast(String state) {
        JSONArray t = Store.thread(peerId);
        for (int i = t.length() - 1; i >= 0; i--) {
            JSONObject m = t.optJSONObject(i);
            if (m != null && m.optBoolean("me") && "sending".equals(m.optString("state"))) {
                try { m.put("state", state); } catch (Exception ignored) {}
                break;
            }
        }
        Store.saveThread(peerId, t);
        runOnUiThread(this::renderThread);
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_PICK);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_PICK || res != RESULT_OK || data == null || peer == null) return;
        java.util.ArrayList<Uri> uris = new java.util.ArrayList<>();
        if (data.getClipData() != null)
            for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        exec.execute(() -> {
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
                runOnUiThread(() -> {
                    renderThread();
                    if (!ok) android.widget.Toast.makeText(this, getString(R.string.no_device_nearby), android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void retryFile(int idx) {
        // re-send is only possible for QR batches; direct sends would need the original uri.
        // v1.0: inform user the device is unreachable / ask to resend from picker.
        android.widget.Toast.makeText(this, getString(R.string.no_device_nearby), android.widget.Toast.LENGTH_LONG).show();
    }
}
