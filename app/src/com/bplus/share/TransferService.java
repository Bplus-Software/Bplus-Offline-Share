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
        public String id;public volatile boolean cancelled;public final java.util.Set<Socket> sockets=java.util.Collections.newSetFromMap(new ConcurrentHashMap<Socket,Boolean>());
        public JSONArray files = new JSONArray(); // [{i,name,size,uri,mime}]
        public Map<String, Uri> uris = new HashMap<>();
    }

    public static void cancelBatch(String id){Batch b=batches.get(id);if(b==null)return;b.cancelled=true;for(Socket socket:b.sockets)try{socket.close();}catch(Exception ignored){}batches.remove(id);}
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

    public static void fileProgress(String from){try{emit("file-progress",new JSONObject().put("from",from));}catch(Exception ignored){}}
    public static void changed(String from){try{emit("sync",new JSONObject().put("from",from));}catch(Exception ignored){}}
    public static void start(Context c) {
        if(!LocalNetwork.usable(c))return;
        Intent i = new Intent(c, TransferService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    public static TransferService get() { return self; }
    public static boolean ready(){TransferService s=self;return s!=null&&s.running&&s.server!=null&&!s.server.isClosed();}

    @Override public void onCreate() {
        super.onCreate();
        self = this;
        boolean network=LocalNetwork.usable(this);boolean bound=false;if(network)try{server=new ServerSocket(Store.PORT);bound=true;}catch(Exception ignored){}
        Notification n = buildNotification(getString(bound?R.string.notif_ready:R.string.app_name), getString(bound?R.string.notif_ready_sub:R.string.notif_network_wait));
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(1, n);
        if(!bound){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return;}
        startServer();
        startAutosendWatcher();
        Outbox.start();Unpair.start();LocalGroups.start();
    }

    private Notification buildNotification(String title, String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, App.CH)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title).setContentText(text)
                .setContentIntent(pi).setOngoing(true).build();
    }

    private void notifyEvent(String title,String text){Alerts.incoming(title,text);}

    @Override public int onStartCommand(Intent i, int f, int id) { if(!LocalNetwork.usable(this)){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}return START_NOT_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        running = false;
        main.removeCallbacksAndMessages(null);autoPool.shutdownNow();JSONArray ap=Store.peers();for(int n=0;n<ap.length();n++){JSONObject p=ap.optJSONObject(n);if(p!=null)Net.cancelAuto(p.optString("id"));}
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        Outbox.stop();
        pool.shutdownNow();stopForeground(STOP_FOREGROUND_REMOVE);self = null;
        super.onDestroy();
    }

    // ---------- server ----------
    private void startServer() {
        running = true;
        pool.execute(() -> {
            try {

                while (running) {
                    Socket s = server.accept();
                    pool.execute(() -> handle(s));
                }
            } catch (Exception ignored) {if(running)main.post(()->{stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();});}
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

            if(path.equals("/pairing/unpair")||path.equals("/pairing/unpair-ack")){if(!method.equals("POST")||len<0||len>4096){err(out,400);return;}try{JSONObject body=new JSONObject(new String(readBody(in,len),"UTF-8"));if(path.endsWith("-ack")){Unpair.acknowledged(body);json(out,new JSONObject().put("ok",true));}else json(out,Unpair.receive(body));}catch(Exception denied){err(out,403);}}
            else if(path.equals("/profile-details")){if(!method.equals("POST")||len<0||len>2000){err(out,400);return;}try{JSONObject b=new JSONObject(new String(readBody(in,len),"UTF-8"));if(!Store.token().equals(b.optString("targetToken")))throw new IOException();synchronized(Store.class){Recall.authenticate(Store.peerById(b.optString("from")),s.getInetAddress().getHostAddress(),b.optString("senderToken"));json(out,ContactFields.shared().put("id",Store.get("deviceId","")));}}catch(Exception denied){err(out,403);}}
            else if(path.split("\\?")[0].equals("/sync")){
                if(!method.equals("POST")||len<0||len>48000){err(out,400);return;}
                try{json(out,LanSync.handle(new JSONObject(new String(readBody(in,len),"UTF-8")),s.getInetAddress().getHostAddress()));}catch(Exception denied){err(out,403);}
            } else if(path.startsWith("/group/")){
                if(!method.equals("POST")){err(out,400);return;}
                String route=path.substring(7).split("\\?")[0];
                try{if(route.equals("file")||route.equals("file-deliver")){LocalGroups.receiveFile(route,q,s.getInetAddress().getHostAddress(),in,len);ok(out);}else{if(len<0||len>20000){err(out,400);return;}json(out,LocalGroups.handle(route,new JSONObject(new String(readBody(in,len),"UTF-8")),s.getInetAddress().getHostAddress()));}}catch(Exception denied){err(out,403);}
            } else if(path.startsWith("/nearby/")){
                if(!method.equals("POST")||len<0||len>48000){err(out,400);return;}
                try{JSONObject result=Nearby.handle(path,new JSONObject(new String(readBody(in,len),"UTF-8")),s.getInetAddress().getHostAddress());json(out,result);if(path.equals("/nearby/confirm")&&result.optString("status").equals("paired"))changed("");}catch(Exception denied){err(out,403);}
            } else if (path.startsWith("/ping")) {
                JSONObject o = new JSONObject();
                o.put("reachable",true);
                if(Nearby.knownAddress(s.getInetAddress().getHostAddress())){o.put("id",Store.get("deviceId",""));o.put("name",Store.deviceName());}
                json(out, o);
            } else if (path.startsWith("/pair")) {
                if(!q.getOrDefault("token", "").equals(Store.token())) { err(out,403); return; }
                if(len<0||len>48000){err(out,400);return;}
                byte[] body = readBody(in, len);
                JSONObject input=new JSONObject(new String(body,"UTF-8"));
                JSONObject peer=Profile.clean(input);peer.put("token",input.getString("token"));String epoch=input.optString("pairEpoch");if(epoch.matches("[a-f0-9]{64}"))peer.put("pairEpoch",epoch);
                peer.put("ip", s.getInetAddress().getHostAddress());
                peer.put("port", Store.PORT);
                peer.put("lastSeen", System.currentTimeMillis());
                Store.upsertPeer(peer);
                JSONObject res = Profile.local();if(peer.has("pairEpoch"))res.put("pairEpoch",peer.getString("pairEpoch"));
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
                JSONObject receiver=null;JSONArray paired=Store.peers();for(int k=0;k<paired.length();k++){JSONObject p=paired.optJSONObject(k);if(p!=null&&s.getInetAddress().getHostAddress().equals(p.optString("ip")))receiver=p;}
                if(receiver==null||!FileConsent.ask(receiver.optString("name"),f.optString("name"),size)){err(out,403);return;}

                String h = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " + size + "\r\nConnection: close\r\n\r\n";
                if(b.cancelled){err(out,409);return;}b.sockets.add(s);try{
                if(b.cancelled)throw new IOException("Cancelled");out.write(h.getBytes("UTF-8"));
                try(InputStream fin = getContentResolver().openInputStream(uri)){
                byte[] buf = new byte[65536];
                long done = 0; int n;
                long lastProgress=0;while ((n = fin.read(buf)) > 0) {
                    if(b.cancelled)throw new IOException("Cancelled");out.write(buf, 0, n);
                    done += n;
                    final long d = done;
                    long now=android.os.SystemClock.elapsedRealtime();if(now-lastProgress<200&&done<size)continue;lastProgress=now;JSONObject ev = new JSONObject();
                    ev.put("batch", b.id); ev.put("i", idx); ev.put("done", d); ev.put("size", size);
                    main.post(() -> emit("serve-progress", ev));
                }
                out.flush();}}finally{b.sockets.remove(s);}
            } else if(path.split("\\?")[0].equals("/recall")){
                if(!method.equals("POST")||len<0||len>2048){err(out,400);return;}
                try{JSONObject request=new JSONObject(new String(readBody(in,len),"UTF-8"));json(out,Recall.handle(request,s.getInetAddress().getHostAddress(),uri->eraseReceived(uri)));JSONObject ev=new JSONObject();ev.put("from",request.optString("from"));emit("msg",ev);}catch(Exception denied){err(out,403);}
            } else if (path.startsWith("/recv")) {
                if (!peerTokenOk(q.get("token")) || !Nearby.knownAddress(s.getInetAddress().getHostAddress())) { err(out, 403); return; }
                String from=q.getOrDefault("from", ""),wire=q.getOrDefault("wire",""),receiptKey=q.getOrDefault("key",""),job=q.getOrDefault("job","");boolean tracked=!wire.isEmpty();
                String name=URLDecoder.decode(q.getOrDefault("name","file"),"UTF-8");if(len<0){err(out,400);return;}boolean reserved=false;
                if(tracked){Recall.authenticate(Store.peerById(from),s.getInetAddress().getHostAddress(),q.get("senderToken"));if(!Recall.valid(wire)||!Recall.valid(receiptKey)){err(out,400);return;}}
                if(!job.isEmpty()){JSONObject bound=Store.peerById(from);if(bound==null||!Store.token().equals(q.get("token"))||!s.getInetAddress().getHostAddress().equals(bound.optString("ip"))){err(out,403);return;}String state;try{state=FileReceiveJobs.begin(from,job,name,len,wire,receiptKey);}catch(Exception invalid){err(out,400);return;}if(state.equals("busy")){err(out,409);return;}if(state.equals("duplicate")){byte[] discard=new byte[65536];long remaining=len;while(remaining>0){int n=in.read(discard,0,(int)Math.min(discard.length,remaining));if(n<0)throw new IOException("Incomplete duplicate");remaining-=n;}json(out,new JSONObject().put("duplicate",true));return;}reserved=true;}
                try{
                 if(tracked&&Recall.exists(from,wire,receiptKey)){byte[] skip=new byte[65536];int left=len,n;while(left>0){n=in.read(skip,0,Math.min(left,skip.length));if(n<0)throw new IOException();left-=n;}json(out,new JSONObject().put("duplicate",true));return;}
                 Save.Target saved=Save.create(this,name);try{byte[] buf=new byte[65536];long remaining=len;int n;OutputStream target=new BufferedOutputStream(saved.out,131072);while(remaining>0){n=in.read(buf,0,(int)Math.min(buf.length,remaining));if(n<0)throw new IOException("Incomplete file");target.write(buf,0,n);remaining-=n;}target.flush();saved.complete();}catch(Exception e){saved.abort();throw e;}
                 JSONObject msg=new JSONObject().put("kind","file").put("name",name).put("size",len).put("me",false).put("ts",System.currentTimeMillis()).put("state","received").put("uri",saved.uri.toString()).put("mime",Save.mime(name));if(tracked)msg.put("wireId",wire);if(!job.isEmpty()){msg.put("job",job);Store.receiveFileJobAtomic(from,job,wire,receiptKey,msg,FileReceiveJobs.receipt(name,len,wire,receiptKey));}else{if(tracked)Recall.received(from,wire,receiptKey,"file",saved.uri.toString());if(!from.isEmpty())Store.addMsg(from,msg);}
                 JSONObject senderPeer=Store.peerById(from);Alerts.message(senderPeer==null?getString(R.string.unknown_sender):senderPeer.optString("name"),msg);JSONObject ev=new JSONObject().put("from",from).put("name",name);emit("recv-file",ev);json(out,new JSONObject().put("duplicate",false));
                }finally{if(reserved)FileReceiveJobs.end(from,job);}
            } else if (path.startsWith("/msg")) {
                if (!peerTokenOk(q.get("token")) || !Nearby.knownAddress(s.getInetAddress().getHostAddress())) { err(out, 403); return; }
                byte[] body = readBody(in, len);
                JSONObject m = new JSONObject(new String(body, "UTF-8"));
                if (m.optString("text", "").isEmpty()) { ok(out); return; }
                String from = m.optString("from", ""),wire=m.optString("wire"),receiptKey=m.optString("key");boolean tracked=!wire.isEmpty();if(tracked){Recall.authenticate(Store.peerById(from),s.getInetAddress().getHostAddress(),m.optString("senderToken"));if(!Recall.valid(wire)||!Recall.valid(receiptKey)){err(out,400);return;}if(Recall.exists(from,wire,receiptKey)){ok(out);return;}Recall.received(from,wire,receiptKey,"text","");}
                JSONObject msg = new JSONObject();
                msg.put("kind", "text"); msg.put("body", m.optString("text", ""));if(tracked)msg.put("wireId",wire);
                msg.put("me", false); msg.put("ts", System.currentTimeMillis()); msg.put("state", "received");
                if (!from.isEmpty()) Store.addMsg(from, msg);
                JSONObject peer = Store.peerById(from);
                String pname = peer != null ? peer.optString("name") : "B+ Offline Share";
                Alerts.message(pname,msg);
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
                if (!peerTokenOk(q.get("token")) || !Nearby.knownAddress(s.getInetAddress().getHostAddress())) { err(out, 403); return; }
                JSONObject o = new JSONObject();
                JSONArray peers = Store.peers();
                JSONObject threads = new JSONObject();
                for (int i = 0; i < peers.length(); i++) {
                    JSONObject pr = peers.getJSONObject(i);
                    JSONArray portable=new JSONArray(Store.thread(pr.getString("id")).toString());
                    for(int j=0;j<portable.length();j++){JSONObject item=portable.optJSONObject(j);if(item!=null){item.remove("uri");item.remove("wireId");item.remove("recallKey");}}
                    threads.put(pr.getString("id"), portable);
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
            if(sb.length()>16384)throw new IOException("HTTP line too long");
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

    private final java.util.concurrent.ExecutorService autoPool=java.util.concurrent.Executors.newSingleThreadExecutor();
    private void autosendScan(){JSONArray peers=Store.peers();for(int p=0;p<peers.length();p++){JSONObject peer=peers.optJSONObject(p);if(peer==null)continue;String id=peer.optString("id"),key=DeviceSettingsActivity.key(id);if(!Store.get(key+"on","0").equals("1"))continue;String folder=Store.get(key+"folder",""),epoch=Store.get(key+"epoch","");if(folder.isEmpty())continue;try{JSONArray files=AutoSend.listFilesChecked(this,Uri.parse(folder));for(int i=0;i<files.length();i++){JSONObject f=files.optJSONObject(i);if(f!=null&&AutoLedger.claim(id+"|"+folder,f))main.postDelayed(()->autoSendFile(f,id,folder,epoch),4000);}}catch(Exception ignored){}}}
    private void autoSendFile(JSONObject f,String id,String folder,String epoch){autoPool.execute(()->{boolean success=false;try{String key=DeviceSettingsActivity.key(id);JSONObject peer=Store.peerById(id);if(peer!=null&&epoch.equals(Store.get(key+"epoch",""))&&Store.get(key+"on","0").equals("1")&&folder.equals(Store.get(key+"folder","")))success=sendOriginal(peer,Uri.parse(f.getString("uri")),f.getString("name"),f.getLong("size"),true,id,()->Store.peerById(id)!=null&&epoch.equals(Store.get(key+"epoch",""))&&Store.get(key+"on","0").equals("1")&&folder.equals(Store.get(key+"folder","")));}catch(Exception ignored){}finally{AutoLedger.result(id+"|"+folder,f.optString("uri"),success);}});}

    private boolean eraseReceived(String raw){if(!Store.get("ownedReceived_"+raw,"").equals("1"))return false;try{Uri uri=Uri.parse(raw);boolean ok=getContentResolver().delete(uri,null,null)>0;if(!ok){try(android.database.Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){ok=c!=null&&!c.moveToFirst();}}if(ok)Recall.retireUri(raw);return ok;}catch(Exception e){return false;}}

    public static boolean sendTextToPeer(JSONObject peer,String text){try{Outbox.enqueue(peer.optString("id"),text);return true;}catch(Exception e){return false;}}
    public static boolean sendToPeer(JSONObject peer, Uri uri, String name, long size) {return sendOriginal(peer,uri,name,size,false);}
    static boolean sendRecordedVoice(JSONObject peer,Uri uri,String name,long size){return sendOriginal(peer,uri,name,size,true);}
    private static boolean sendOriginal(JSONObject peer,Uri uri,String name,long size,boolean micRelease){return sendOriginal(peer,uri,name,size,micRelease,"",()->true);}
    private static boolean sendOriginal(JSONObject peer,Uri uri,String name,long size,boolean micRelease,String autoId,java.util.function.BooleanSupplier allowed){
        return FileJobs.start(peer,uri,name,size,micRelease,autoId,allowed);
    }
}
