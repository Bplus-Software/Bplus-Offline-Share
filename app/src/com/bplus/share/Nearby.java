package com.bplus.share;
import org.json.*;import java.io.*;import java.net.*;
public final class Nearby {
 public static final NearbyGate gate=new NearbyGate();
 public static JSONObject call(String ip,String path,JSONObject data)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL("http://"+ip+":"+Store.PORT+path).openConnection();c.setConnectTimeout(4000);c.setReadTimeout(5000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");byte[] bytes=data.toString().getBytes("UTF-8");c.setFixedLengthStreamingMode(bytes.length);try{try(OutputStream out=c.getOutputStream()){out.write(bytes);}int code=c.getResponseCode();if(code!=200)throw new IOException("HTTP "+code);return new JSONObject(Net.readAll(c.getInputStream()));}finally{c.disconnect();}
 }
 public static synchronized JSONObject handle(String path,JSONObject data,String ip)throws Exception{
  long now=System.currentTimeMillis();if(!gate.visible)throw new IOException("Nearby is not open");
  JSONObject out=new JSONObject();
  if(path.equals("/nearby/profile"))return Profile.local();
  if(path.equals("/nearby/request")){
   JSONObject profile=Profile.clean(data.getJSONObject("profile"));if(Store.get("blocked_"+profile.getString("id"),"0").equals("1"))throw new IOException("Blocked");
   NearbyGate.Request r=gate.request(profile,ip,data.getString("secret"),now);out.put("request",r.id);out.put("status",r.status);return out;
  }
  NearbyGate.Request r=gate.get(data.getString("request"),data.getString("secret"),ip,now);
  if(path.equals("/nearby/cancel")){gate.cancel(r.id,r.secret,ip,now);out.put("status","cancelled");return out;}
  if(path.equals("/nearby/status")){out.put("status",r.status);if(r.status.equals("accepted")){out.put("profile",Profile.local());out.put("token",Store.token());}return out;}
  if(path.equals("/nearby/confirm")){
   if(!r.status.equals("accepted")&&!r.status.equals("paired"))throw new IOException("Request not accepted");String token=data.getString("token");if(!token.matches("[a-zA-Z0-9]{16,64}"))throw new IOException("Invalid token");JSONObject peer=new JSONObject(r.profile.toString());peer.put("ip",ip);peer.put("port",Store.PORT);peer.put("token",token);Store.upsertPeer(peer);r.status="paired";out.put("status","paired");return out;
  }
  throw new IOException("Unknown endpoint");
 }
 public static boolean knownAddress(String ip){org.json.JSONArray peers=Store.peers();for(int i=0;i<peers.length();i++){JSONObject p=peers.optJSONObject(i);if(p!=null&&ip.equals(p.optString("ip")))return true;}return false;}
}
