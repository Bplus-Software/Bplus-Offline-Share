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

    public static synchronized void saveLocalProfile(String name,String email,String phone,boolean showEmail,boolean showPhone)throws java.io.IOException{if(!p.edit().putString("profileName",name).putString("profileEmail",email).putString("profilePhone",phone).putString("shareProfileEmail",showEmail?"1":"0").putString("shareProfilePhone",showPhone?"1":"0").commit())throw new java.io.IOException("Profile save failed");}

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

    public static synchronized void savePeers(JSONArray a) { if(!p.edit().putString("peers",a.toString()).commit())throw new IllegalStateException("Cannot save paired devices"); }

    public static JSONObject peerById(String id) {
        if(id==null||id.isEmpty())return null;
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
        try{Unpair.paired(peer);}catch(Exception e){throw new IllegalStateException(e);}JSONArray archived=unpaired(),keep=new JSONArray();for(int i=0;i<archived.length();i++){JSONObject old=archived.optJSONObject(i);if(old!=null&&!peer.optString("id").equals(old.optString("id")))keep.put(old);}if(!p.edit().putString("peers",a.toString()).putString("unpairedPeers",keep.toString()).putString(Unpair.tombSlot(peer.optString("id")),"").commit())throw new IllegalStateException("Cannot save paired device");
    }

    public static JSONArray unpaired(){try{return new JSONArray(get("unpairedPeers","[]"));}catch(Exception e){return new JSONArray();}}
    public static JSONArray listedPeers(){JSONArray all=peers(),old=unpaired();for(int i=0;i<old.length();i++){JSONObject p=old.optJSONObject(i);if(p!=null&&peerById(p.optString("id"))==null)all.put(p);}return all;}
    public static JSONObject listedPeer(String id){JSONObject p=peerById(id);if(p!=null)return p;JSONArray old=unpaired();for(int i=0;i<old.length();i++){JSONObject o=old.optJSONObject(i);if(o!=null&&id.equals(o.optString("id")))return o;}return null;}
    public static synchronized void removePeer(String id) {removePeerInternal(id,true,null);Unpair.kick();}
    public static synchronized void applyRemoteUnpair(String id,JSONObject tomb){removePeerInternal(id,false,tomb);TransferService.changed(id);}
    private static synchronized void removePeerInternal(String id,boolean notify,JSONObject tomb) {try{if(notify)tomb=Unpair.prepare(id);}catch(Exception e){throw new IllegalStateException(e);}FileJobs.invalidate(id);set("autoPeer_"+id+"_on","0");Net.cancelAuto(id);
        JSONObject previous=peerById(id);JSONArray archive=unpaired();if(previous!=null){try{JSONObject label=new JSONObject();for(String field:new String[]{"id","name","username","device","photo"})label.put(field,previous.optString(field));label.put("unpaired",true);archive.put(label);}catch(Exception e){throw new IllegalStateException(e);}}
        JSONArray a = peers(); JSONArray out = new JSONArray();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && !id.equals(o.optString("id"))) out.put(o);
        }
        JSONArray t=thread(id);for(int i=0;i<t.length();i++){JSONObject m=t.optJSONObject(i);if(m!=null&&m.optBoolean("me")&&(m.optString("state").equals("queued")||m.optString("state").equals("sending")||m.optString("state").equals("failed"))&&m.optString("kind").equals("text")){try{m.put("state","canceled");}catch(Exception ignored){}}}
        android.content.SharedPreferences.Editor edit=p.edit().putString("thread_"+id,t.toString()).putString("peers",out.toString()).putString("unpairedPeers",archive.toString());try{if(tomb!=null){edit.putString(Unpair.tombSlot(id),tomb.toString());Unpair.index(id,edit);}}catch(Exception e){throw new IllegalStateException(e);}if(!edit.commit())throw new IllegalStateException("Cannot remove paired device");
    }

    /** Full local removal. Keep receipt/dedup and saved-file ownership ledgers. Never delete bytes. */
    public static synchronized void forgetPeer(String id){FileJobs.invalidate(id);
        if(id==null||id.isEmpty())throw new IllegalArgumentException("Missing device");
        JSONArray active=new JSONArray(),archive=new JSONArray();
        for(int i=0;i<peers().length();i++){JSONObject o=peers().optJSONObject(i);if(o!=null&&!id.equals(o.optString("id")))active.put(o);}
        JSONArray old=unpaired();for(int i=0;i<old.length();i++){JSONObject o=old.optJSONObject(i);if(o!=null&&!id.equals(o.optString("id")))archive.put(o);}
        if(!p.edit().putString("peers",active.toString()).putString("unpairedPeers",archive.toString())
            .putString("autoPeer_"+id+"_on","0").putString("autoPeer_"+id+"_epoch",UUID.randomUUID().toString())
            .putString("forgotten_"+id,Long.toString(System.currentTimeMillis()))
            .remove("thread_"+id).remove("read_"+id).remove("lastChatActionSnapshot_"+id)
            .remove("autoPeer_"+id+"_folder").commit())throw new IllegalStateException("Cannot remove device and chat");
        Net.cancelAuto(id);
    }
    private static JSONArray visibleMessages(String id,JSONArray messages){
        String raw=get("forgotten_"+id,"");if(raw.isEmpty())return messages;
        JSONArray keep=new JSONArray();if(listedPeer(id)==null)return keep;
        long cutoff;try{cutoff=Long.parseLong(raw);}catch(Exception e){return keep;}
        for(int i=0;i<messages.length();i++){JSONObject m=messages.optJSONObject(i);if(m!=null&&m.optLong("ts")>cutoff)keep.put(m);}return keep;
    }

    public static synchronized void durable(String k,String v)throws java.io.IOException{if(!p.edit().putString(k,v).commit())throw new java.io.IOException("Local save failed");}
    public static synchronized void receiveFileJobAtomic(String from,String job,String wire,String key,JSONObject msg,JSONObject metadata)throws Exception{if(peerById(from)==null)throw new java.io.IOException("Device no longer paired");JSONArray t=thread(from);msg.put("id",UUID.randomUUID().toString());t.put(msg);SharedPreferences.Editor edit=p.edit().putString("thread_"+from,t.toString()).putString(FileReceiveJobs.slot(from,job),metadata.toString());if(!wire.isEmpty()){JSONObject receipt=new JSONObject().put("key",key).put("kind","file").put("uri",msg.getString("uri")).put("status","received");edit.putString(Recall.ledger(from,wire),receipt.toString());}if(!edit.commit())throw new java.io.IOException("File receipt save failed");}
    public static synchronized void receiveAtomic(String from,String wire,JSONObject msg,JSONObject receipt)throws Exception{if(peerById(from)==null)throw new java.io.IOException("Device no longer paired");JSONArray t=thread(from);msg.put("id",UUID.randomUUID().toString());t.put(msg);if(!p.edit().putString("thread_"+from,t.toString()).putString(Recall.ledger(from,wire),receipt.toString()).commit())throw new java.io.IOException("Message save failed");}
    public static synchronized void refreshProfile(String id,JSONObject profile)throws Exception{JSONObject peer=peerById(id);if(peer==null)return;if(!id.equals(profile.optString("id")))throw new java.io.IOException("Wrong profile identity");boolean changed=false;for(String field:new String[]{"name","username","device"}){String value=profile.optString(field);if(value.length()>(field.equals("username")?40:80))throw new java.io.IOException("Profile too large");if(!peer.optString(field).equals(value)){peer.put(field,value);changed=true;}}if(profile.has("photo")){PhotoSync.validate(profile);Profile.clean(profile);String photo=profile.optString("photo");if(!peer.optString("photo").equals(photo)){peer.put("photo",photo);changed=true;}peer.put("photoHash",PhotoSync.hash(photo));}if(changed){upsertPeer(peer);TransferService.changed(id);}}
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
        if(!get("forgotten_"+peerId,"").isEmpty()&&peerById(peerId)==null)return;
        try{if(msg.optString("id").isEmpty())msg.put("id",UUID.randomUUID().toString());}catch(Exception ignored){}
        JSONArray t = thread(peerId);
        t.put(msg);
        set("thread_" + peerId, visibleMessages(peerId,t).toString());
    }

    public static synchronized void saveThread(String peerId, JSONArray t) { set("thread_" + peerId, visibleMessages(peerId,t).toString()); }

    // ---- auto-send ----
    public static boolean autosendOn() { return get("autosend", "0").equals("1"); }
    public static String autosendFolder() { return get("autosendFolder", ""); }
    public static String autosendTarget() { return get("autosendTarget", ""); } // "dev:<id>" or "grp:<id>"
}
