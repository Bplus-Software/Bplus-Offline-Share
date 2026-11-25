package com.bplus.share;
import org.json.*;import java.io.*;import java.net.*;
public final class Nearby {
 public static final NearbyGate gate=new NearbyGate();
 private static final java.util.concurrent.ScheduledExecutorService profileDeadline=java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
 public static JSONObject profile(String ip)throws Exception{
  final HttpURLConnection c=(HttpURLConnection)new URL("http://"+ip+":"+Store.PORT+"/nearby/profile").openConnection();long started=(System.nanoTime()/1000000L);java.util.concurrent.ScheduledFuture<?> limit=profileDeadline.schedule(c::disconnect,8,java.util.concurrent.TimeUnit.SECONDS);
  try{c.setConnectTimeout(2500);c.setReadTimeout(4000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");byte[] body="{}".getBytes("UTF-8");c.setFixedLengthStreamingMode(body.length);try(OutputStream o=c.getOutputStream()){o.write(body);}int code=c.getResponseCode();if(code!=200)throw new IOException("Profile HTTP "+code);ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] bytes=new byte[2048];int n;while((n=in.read(bytes))!=-1){if((System.nanoTime()/1000000L)-started>=8000)throw new java.net.SocketTimeoutException("Profile deadline");if(out.size()+n>48000)throw new IOException("Profile too large");out.write(bytes,0,n);}}return new JSONObject(out.toString("UTF-8"));}finally{limit.cancel(false);c.disconnect();}
 }
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
  if(path.equals("/nearby/status")){out.put("status",r.status);if((r.status.equals("accepted")||r.status.equals("paired"))){out.put("profile",Profile.local());out.put("token",Store.token());JSONObject saved=Store.peerById(r.profile.optString("id"));if(saved!=null&&saved.has("pairEpoch"))out.put("pairEpoch",saved.getString("pairEpoch"));}return out;}
  if(path.equals("/nearby/confirm")){
   if(!r.status.equals("accepted")&&!r.status.equals("paired"))throw new IOException("Request not accepted");String token=data.getString("token");if(!token.matches("[a-zA-Z0-9]{16,64}"))throw new IOException("Invalid token");JSONObject peer=new JSONObject(r.profile.toString());peer.put("ip",ip);peer.put("port",Store.PORT);peer.put("token",token);String epoch=data.optString("pairEpoch");if(!epoch.isEmpty()){if(!epoch.equals(UnpairWire.nearby(r.id,r.secret)))throw new IOException("Wrong pair epoch");peer.put("pairEpoch",epoch);}Store.upsertPeer(peer);r.status="paired";out.put("status","paired");if(peer.has("pairEpoch"))out.put("pairEpoch",peer.getString("pairEpoch"));return out;
  }
  throw new IOException("Unknown endpoint");
 }
 public static boolean knownAddress(String ip){org.json.JSONArray peers=Store.peers();for(int i=0;i<peers.length();i++){JSONObject p=peers.optJSONObject(i);if(p!=null&&ip.equals(p.optString("ip")))return true;}return false;}
}
