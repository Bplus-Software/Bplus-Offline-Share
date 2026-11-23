package com.bplus.share;
import org.json.*;import java.util.*;
/** Pure record-only rules: never reads or deletes file bytes. */
public final class ChatRules{
 public static String key(JSONObject m){String id=m.optString("id");return id.isEmpty()?m.optString("uri")+"|"+m.optString("name")+"|"+m.optLong("ts"):id;}
 public static JSONArray removeSelected(JSONArray old,Set<String> confirmed){JSONArray keep=new JSONArray();for(int i=0;i<old.length();i++){JSONObject m=old.optJSONObject(i);if(m!=null&&!(m.optString("kind").equals("file")&&confirmed.contains(key(m))))keep.put(m);}return keep;}
 public static JSONArray clear(JSONArray old,boolean filesOnly){JSONArray keep=new JSONArray();for(int i=0;i<old.length();i++){JSONObject m=old.optJSONObject(i);if(m!=null&&filesOnly&&!m.optString("kind").equals("file"))keep.put(m);}return keep;}
}
