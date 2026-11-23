package com.bplus.share;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

public class Store {
    private static SharedPreferences p;
    public static final int PORT = 47200;

    public static void init(Context c) {
        p = c.getSharedPreferences("bplus_share", Context.MODE_PRIVATE);
        if (get("deviceId", "").isEmpty())
            set("deviceId", UUID.randomUUID().toString().substring(0, 8));
        if (get("deviceName", "").isEmpty())
            set("deviceName", android.os.Build.MODEL);
        if (get("sessionToken", "").isEmpty())
            set("sessionToken", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    }

    public static String get(String k, String def) { return p.getString(k, def); }
    public static void set(String k, String v) { p.edit().putString(k, v).apply(); }
    public static int getInt(String k, int def) { return p.getInt(k, def); }
    public static void setInt(String k, int v) { p.edit().putInt(k, v).apply(); }

    public static String deviceName() { return get("deviceName", android.os.Build.MODEL); }
    public static String token() { return get("sessionToken", ""); }

    // ---- paired devices ----
    public static JSONArray peers() {
        try { return new JSONArray(get("peers", "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    public static void savePeers(JSONArray a) { set("peers", a.toString()); }

    public static JSONObject peerById(String id) {
        JSONArray a = peers();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return o;
        }
        return null;
    }

    public static synchronized void upsertPeer(JSONObject peer) {
        JSONArray a = peers();
        boolean found = false;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && peer.optString("id").equals(o.optString("id"))) {
                try { a.put(i, peer); } catch (Exception ignored) {}
                found = true;
            }
        }
        if (!found) a.put(peer);
        savePeers(a);
    }

    public static synchronized void removePeer(String id) {
        JSONArray a = peers(); JSONArray out = new JSONArray();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && !id.equals(o.optString("id"))) out.put(o);
        }
        savePeers(out);
    }

    // ---- groups: [{id,name,members:[peerId]}] ----
    public static JSONArray groups() {
        try { return new JSONArray(get("groups", "[]")); } catch (Exception e) { return new JSONArray(); }
    }
    public static void saveGroups(JSONArray a) { set("groups", a.toString()); }

    // ---- chat threads: pref key "thread_<peerId>" -> [{kind,text|file,body,name,size,uri,me,ts,state}] ----
    public static JSONArray thread(String peerId) {
        try { return new JSONArray(get("thread_" + peerId, "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    public static synchronized void addMsg(String peerId, JSONObject msg) {
        try{if(msg.optString("id").isEmpty())msg.put("id",UUID.randomUUID().toString());}catch(Exception ignored){}
        JSONArray t = thread(peerId);
        t.put(msg);
        set("thread_" + peerId, t.toString());
    }

    public static void saveThread(String peerId, JSONArray t) { set("thread_" + peerId, t.toString()); }

    // ---- auto-send ----
    public static boolean autosendOn() { return get("autosend", "0").equals("1"); }
    public static String autosendFolder() { return get("autosendFolder", ""); }
    public static String autosendTarget() { return get("autosendTarget", ""); } // "dev:<id>" or "grp:<id>"
}
