package com.bplus.share;
import org.json.*;import java.io.*;import java.security.*;
/** Bounded avatar field. A missing field from an older peer never clears a saved image. */
public final class PhotoSync{
 public static String hash(String photo)throws Exception{byte[] b=MessageDigest.getInstance("SHA-256").digest(photo.getBytes("UTF-8"));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(java.util.Locale.ROOT,"%02x",x&255));return s.toString();}
 public static void validate(JSONObject p)throws Exception{if(!p.has("photo"))return;String photo=p.optString("photo");if(photo.length()>40000||(!photo.isEmpty()&&!photo.matches("[A-Za-z0-9+/]*={0,2}")))throw new IOException("Invalid photo encoding");if(p.has("photoHash")&&!hash(photo).equals(p.optString("photoHash")))throw new IOException("Photo hash differs");}
}
