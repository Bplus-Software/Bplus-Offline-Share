package com.bplus.share;
import org.json.*;import java.io.*;
/** Choose highest numeric release version, independent of publish order; APK versionCode guards installation. */
public final class ReleaseChoice{
 public static JSONObject choose(JSONArray releases,boolean tests,String installed)throws Exception{JSONObject best=null;String highest=installed;for(int i=0;i<releases.length();i++){JSONObject r=releases.optJSONObject(i);if(r==null||r.optBoolean("draft")||(!tests&&r.optBoolean("prerelease")))continue;String version=r.optString("tag_name");try{if(!UpdateHttp.newer(version,highest))continue;}catch(IOException e){continue;}JSONArray assets=r.optJSONArray("assets");int apk=0;if(assets!=null)for(int j=0;j<assets.length();j++){JSONObject a=assets.optJSONObject(j);if(a!=null&&a.optString("name").endsWith(".apk")&&a.optString("state","uploaded").equals("uploaded"))apk++;}if(apk!=1)continue;best=r;highest=version;}return best;}
}
