package com.bplus.share;
import org.json.*;import java.io.*;import java.util.concurrent.*;
/** Auth happens before this ledger. A job is scoped to the verified sender. */
public final class FileReceiveJobs{
 private static final ConcurrentHashMap<String,Boolean> active=new ConcurrentHashMap<>();
 public static String slot(String from,String job){return "filejob_"+from+"_"+job;}
 public static String begin(String from,String job,String name,long size,String wire,String key)throws Exception{if(!job.matches("[a-f0-9]{24}")||(!wire.isEmpty()&&(!Recall.valid(wire)||!Recall.valid(key)))||size<0)throw new IOException("Invalid file job");String slot=slot(from,job);synchronized(Store.class){String raw=Store.get(slot,"");if(!raw.isEmpty()){JSONObject prior=new JSONObject(raw);if(!name.equals(prior.optString("name"))||size!=prior.optLong("size")||!wire.equals(prior.optString("wire"))||!key.equals(prior.optString("key")))throw new IOException("Job metadata mismatch");return "duplicate";}if(active.putIfAbsent(slot,true)!=null)return "busy";return "new";}}
 public static void end(String from,String job){active.remove(slot(from,job));}
 public static JSONObject receipt(String name,long size,String wire,String key)throws Exception{return new JSONObject().put("name",name).put("size",size).put("wire",wire).put("key",key);}
}
