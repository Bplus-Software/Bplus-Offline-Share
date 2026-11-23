package com.bplus.share;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransferService extends Service {

    // a batch of files the user is offering via QR
    public static class Batch {
        public String id;
        public JSONArray files = new JSONArray(); // [{i,name,size,uri,mime}]
        public Map<String, Uri> uris = new HashMap<>();
    }

    public static final Map<String, Batch> batches = new ConcurrentHashMap<>();
    private static TransferService self;
    private ServerSocket server;
    private ExecutorService pool = Executors.newCachedThreadPool();
    private Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean running;

    public interface Listener {
        void onEvent(String type, JSONObject data); // "connected","serve-progress","msg","recv-file","pair"
    }
    private static final Map<String, Listener> listeners = new ConcurrentHashMap<>();
    public static void listen(String tag, Listener l) { listeners.put(tag, l); }
    public static void unlisten(String tag) { listeners.remove(tag); }
    private static void emit(String type, JSONObject data) {
        for (Listener l : listeners.values()) {
            try { l.onEvent(type, data); } catch (Exception ignored) {}
        }
    }

    public static void start(Context c) {
        Intent i = new Intent(c, TransferService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    public static TransferService get() { return self; }

    @Override public void onCreate() {
        super.onCreate();
        self = this;
        Notification n = buildNotification(getString(R.string.notif_ready), getString(R.string.notif_ready_sub));
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(1, n);
        startServer();
        startAutosendWatcher();
    }

    private Notification buildNotification(String title, String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, App.CH)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title).setContentText(text)
                .setContentIntent(pi).setOngoing(true).build();
    }

    private void notifyEvent(String title, String text) {
        try {
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            Notification n = new Notification.Builder(this, App.CH)
                    .setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
                    .setAutoCancel(true).build();
            nm.notify((int) (System.currentTimeMillis() % 100000) + 2, n);
        } catch (Exception ignored) {}
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        self = null;
        super.onDestroy();
    }

    // ---------- server ----------
    private void startServer() {
        running = true;
        pool.execute(() -> {
            try {
                server = new ServerSocket(Store.PORT);
                while (running) {
                    Socket s = server.accept();
                    pool.execute(() -> handle(s));
                }
            } catch (Exception ignored) {}
        });
    }

    private void handle(Socket s) {
        try {
            s.setSoTimeout(30000);
            InputStream in = new BufferedInputStream(s.getInputStream());
            OutputStream out = new BufferedOutputStream(s.getOutputStream());
            String line = readLine(in);
            if (line == null) { s.close(); return; }
            String[] parts = line.split(" ");
            String method = parts[0];
            String path = parts.length > 1 ? parts[1] : "/";
            Map<String, String> q = query(path);
            Map<String, String> headers = new HashMap<>();
            while (!(line = readLine(in)).isEmpty()) {
                int c = line.indexOf(':');
                if (c > 0) headers.put(line.substring(0, c).trim().toLowerCase(), line.substring(c + 1).trim());
            }
            int len = headers.containsKey("content-length") ? Integer.parseInt(headers.get("content-length")) : -1;

            if (path.startsWith("/ping")) {
                JSONObject o = new JSONObject();
                o.put("id", Store.get("deviceId", ""));
                o.put("name", Store.deviceName());
                json(out, o);
            } else if (path.startsWith("/pair")) {
                if(!q.getOrDefault("token", "").equals(Store.token())) { err(out,403); return; }
                byte[] body = readBody(in, len);
                JSONObject peer = new JSONObject(new String(body, "UTF-8"));
                peer.put("ip", s.getInetAddress().getHostAddress());
                peer.put("port", Store.PORT);
                peer.put("lastSeen", System.currentTimeMillis());
                Store.upsertPeer(peer);
                JSONObject res = new JSONObject();
                res.put("id", Store.get("deviceId", ""));
                res.put("name", Store.deviceName());
                emit("pair", peer);
                json(out, res);
            } else if (path.startsWith("/manifest")) {
                Batch b = batches.get(q.get("batch"));
                if (b == null || !q.getOrDefault("token", "").equals(Store.token())) { err(out, 403); return; }
                JSONObject o = new JSONObject();
                o.put("name", Store.deviceName());
                o.put("files", b.files);
                json(out, o);
            } else if (path.startsWith("/file")) {
                Batch b = batches.get(q.get("batch"));
                if (b == null || !q.getOrDefault("token", "").equals(Store.token())) { err(out, 403); return; }
                int idx = Integer.parseInt(q.get("i"));
                JSONObject f = b.files.getJSONObject(idx);
                Uri uri = b.uris.get(String.valueOf(idx));
                long size = f.getLong("size");
                String h = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " + size + "\r\nConnection: close\r\n\r\n";
                out.write(h.getBytes("UTF-8"));
                InputStream fin = getContentResolver().openInputStream(uri);
                byte[] buf = new byte[65536];
                long done = 0; int n;
                while ((n = fin.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    done += n;
                    final long d = done;
                    JSONObject ev = new JSONObject();
                    ev.put("batch", b.id); ev.put("i", idx); ev.put("done", d); ev.put("size", size);
                    main.post(() -> emit("serve-progress", ev));
                }
                fin.close();
                out.flush();
            } else if (path.startsWith("/recv")) {
                if (!peerTokenOk(q.get("token"))) { err(out, 403); return; }
                String name = URLDecoder.decode(q.getOrDefault("name", "file"), "UTF-8");
                OutputStream fout = Save.openOut(this, name);
                byte[] buf = new byte[65536];
                long remaining = len; int n;
                while (remaining > 0) {
                    n = in.read(buf, 0, (int) Math.min(buf.length, remaining));
                    if (n < 0) break;
                    if (fout != null) fout.write(buf, 0, n);
                    remaining -= n;
                }
                if (fout != null) fout.close();
                String from = q.getOrDefault("from", "");
                JSONObject msg = new JSONObject();
                msg.put("kind", "file"); msg.put("name", name); msg.put("size", len);
                msg.put("me", false); msg.put("ts", System.currentTimeMillis()); msg.put("state", "received");
                if (!from.isEmpty()) Store.addMsg(from, msg);
                notifyEvent(getString(R.string.app_name), getString(R.string.notif_file, name));
                JSONObject ev = new JSONObject(); ev.put("from", from); ev.put("name", name);
                emit("recv-file", ev);
                ok(out);
            } else if (path.startsWith("/msg")) {
                if (!peerTokenOk(q.get("token"))) { err(out, 403); return; }
                byte[] body = readBody(in, len);
                JSONObject m = new JSONObject(new String(body, "UTF-8"));
                if (m.optString("text", "").isEmpty()) { ok(out); return; }
                String from = m.optString("from", "");
                JSONObject msg = new JSONObject();
                msg.put("kind", "text"); msg.put("body", m.optString("text", ""));
                msg.put("me", false); msg.put("ts", System.currentTimeMillis()); msg.put("state", "received");
                if (!from.isEmpty()) Store.addMsg(from, msg);
                JSONObject peer = Store.peerById(from);
                String pname = peer != null ? peer.optString("name") : "B+ Share";
                notifyEvent(pname, m.optString("text", ""));
                JSONObject ev = new JSONObject(); ev.put("from", from);
                emit("msg", ev);
                ok(out);
            } else if (path.startsWith("/batchdone")) {
                if(!q.getOrDefault("token", "").equals(Store.token())) { err(out,403); return; }
                JSONObject ev = new JSONObject();
                ev.put("batch", q.getOrDefault("batch", ""));
                emit("batchdone", ev);
                ok(out);
            } else if (path.startsWith("/history")) {
                if (!peerTokenOk(q.get("token"))) { err(out, 403); return; }
                JSONObject o = new JSONObject();
                JSONArray peers = Store.peers();
                JSONObject threads = new JSONObject();
                for (int i = 0; i < peers.length(); i++) {
                    JSONObject pr = peers.getJSONObject(i);
                    threads.put(pr.getString("id"), Store.thread(pr.getString("id")));
                }
                o.put("threads", threads);
                o.put("deviceName", Store.deviceName());
                json(out, o);
            } else {
                err(out, 404);
            }
            out.flush();
        } catch (Exception ignored) {
        } finally {
            try { s.close(); } catch (Exception ignored) {}
        }
    }

    private boolean peerTokenOk(String token) {
        if (token == null) return false;
        JSONArray a = Store.peers();
        for (int i = 0; i < a.length(); i++)
            if (token.equals(a.optJSONObject(i) != null ? a.optJSONObject(i).optString("token") : "")) return true;
        return token.equals(Store.token());
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') sb.append((char) c);
        }
        return sb.length() == 0 && c == -1 ? null : sb.toString();
    }

    private static Map<String, String> query(String path) {
        Map<String, String> m = new HashMap<>();
        int i = path.indexOf('?');
        if (i < 0) return m;
        for (String kv : path.substring(i + 1).split("&")) {
            int e = kv.indexOf('=');
            if (e > 0) m.put(kv.substring(0, e), kv.substring(e + 1));
        }
        return m;
    }

    private static byte[] readBody(InputStream in, int len) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long remaining = len;
        while (remaining > 0) {
            int n = in.read(buf, 0, (int) Math.min(buf.length, remaining));
            if (n < 0) break;
            b.write(buf, 0, n);
            remaining -= n;
        }
        return b.toByteArray();
    }

    private static void json(OutputStream out, JSONObject o) throws IOException {
        byte[] b = o.toString().getBytes("UTF-8");
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + b.length + "\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
        out.write(b);
        out.flush();
    }

    private static void ok(OutputStream out) throws IOException {
        out.write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes("UTF-8"));
        out.flush();
    }

    private static void err(OutputStream out, int code) throws IOException {
        out.write(("HTTP/1.1 " + code + " X\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
        out.flush();
    }

    // ---------- auto-send watcher ----------
    private void startAutosendWatcher() {
        Runnable loop = new Runnable() {
            @Override public void run() {
                try { autosendScan(); } catch (Exception ignored) {}
                main.postDelayed(this, 15000);
            }
        };
        main.postDelayed(loop, 15000);
    }

    private void autosendScan() {
        if (!Store.autosendOn()) return;
        String tree = Store.autosendFolder();
        String target = Store.autosendTarget();
        if (tree.isEmpty() || target.isEmpty()) return;
        JSONArray known;
        try { known = new JSONArray(Store.get("autosendSeen", "[]")); } catch (Exception e) { known = new JSONArray(); }
        JSONArray found = AutoSend.listFiles(this, Uri.parse(tree));
        JSONArray newKnown = new JSONArray();
        for (int i = 0; i < found.length(); i++) {
            JSONObject f = found.optJSONObject(i);
            if (f == null) continue;
            String key = f.optString("uri") + ":" + f.optString("size");
            boolean seen = false;
            for (int k = 0; k < known.length(); k++) if (key.equals(known.optString(k))) { seen = true; break; }
            newKnown.put(key);
            if (!seen) {
                // wait a beat for the file to finish copying, then send
                final JSONObject ff = f;
                main.postDelayed(() -> autoSendFile(ff, target), 4000);
            }
        }
        Store.set("autosendSeen", newKnown.toString());
    }

    private void autoSendFile(JSONObject f, String target) {
        pool.execute(() -> {
            try {
                Uri uri = Uri.parse(f.getString("uri"));
                String name = f.getString("name");
                long size = f.getLong("size");
                if (target.startsWith("dev:")) {
                    JSONObject peer = Store.peerById(target.substring(4));
                    if (peer != null) sendToPeer(peer, uri, name, size);
                } else if (target.startsWith("grp:")) {
                    JSONArray groups = Store.groups();
                    for (int i = 0; i < groups.length(); i++) {
                        JSONObject g = groups.optJSONObject(i);
                        if (g == null || !target.substring(4).equals(g.optString("id"))) continue;
                        JSONArray members = g.optJSONArray("members");
                        if (members == null) break;
                        for (int m = 0; m < members.length(); m++) {
                            JSONObject peer = Store.peerById(members.optString(m));
                            if (peer != null) sendToPeer(peer, uri, name, size);
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public static boolean sendToPeer(JSONObject peer, Uri uri, String name, long size) {
        boolean ok = Net.send(App.ctx(), peer.optString("ip"), peer.optInt("port", Store.PORT),
                peer.optString("token"), uri, name, size, null);
        JSONObject msg = new JSONObject();
        try {
            msg.put("kind", "file"); msg.put("name", name); msg.put("size", size);
            msg.put("me", true); msg.put("ts", System.currentTimeMillis());
            msg.put("state", ok ? "sent" : "failed");
            Store.addMsg(peer.optString("id"), msg);
        } catch (Exception ignored) {}
        return ok;
    }
}
