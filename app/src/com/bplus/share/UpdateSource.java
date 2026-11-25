package com.bplus.share;
import org.json.*;import java.io.*;
/** Shared manual/automatic source, persistent five-minute cache and API backoff. */
public final class UpdateSource{
 public static final class Result{public JSONArray releases;public boolean feed,cached;Result(JSONArray a,boolean f,boolean c){releases=a;feed=f;cached=c;}}
 private static long number(String key){try{return Long.parseLong(Store.get(key,"0"));}catch(Exception e){return 0;}}
 public static synchronized Result load(boolean tests)throws Exception{
  String key="updateCheck_org118_"+(tests?"test":"stable");long now=System.currentTimeMillis(),at=number(key+"_at");String cache=Store.get(key+"_json","");if(!cache.isEmpty()&&at<=now&&now-at<300000)return new Result(new JSONArray(cache),Store.get(key+"_feed","0").equals("1"),true);
  long until=number("updateApiBlockedUntil");JSONArray all;boolean feed=false;
  try{if(until>now)throw new UpdateHttp.HttpFailure(429,until);all=new JSONArray(UpdateAsset.fetch("/releases?per_page=100"));}
  catch(UpdateHttp.HttpFailure limit){if(limit.status!=403&&limit.status!=429)throw limit;if(until<=now)Store.set("updateApiBlockedUntil",Long.toString(Math.max(now+300000,limit.resetAt)));if(!tests)throw limit;all=UpdateFeed.parse(UpdateAsset.fetch("/releases.atom"));if(all.length()==0)throw new IOException("Fallback release metadata could not be verified");feed=true;}
  Store.set(key+"_json",all.toString());Store.set(key+"_at",Long.toString(now));Store.set(key+"_feed",feed?"1":"0");return new Result(all,feed,false);
 }
}
