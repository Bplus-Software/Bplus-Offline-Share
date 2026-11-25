package com.bplus.share;
import org.json.*;import java.io.*;import java.util.*;
/** Local, expiring request gate. No profile endpoint returns pairing credentials. */
public final class NearbyGate {
 public static final class Request {public String id,secret,ip;public volatile String status="pending";public JSONObject profile;public long expires;}
 private final Map<String,Request> requests=new HashMap<>();private final Map<String,Long> recent=new HashMap<>();private final Set<String> blocked=new HashSet<>();public volatile boolean visible;
 public synchronized void visible(boolean on){visible=on;if(!on)requests.clear();}
 public synchronized Request request(JSONObject profile,String ip,String secret,long now)throws Exception{
  if(!visible)throw new IOException("Nearby screen closed");String peer=profile.getString("id");requests.values().removeIf(r->r.expires<=now);recent.entrySet().removeIf(e->now-e.getValue()>60000);if(blocked.contains(peer))throw new IOException("Request blocked");if(secret.length()!=32)throw new IOException("Invalid request secret");
  for(Request r:requests.values())if(r.ip.equals(ip)&&r.expires>now&&(r.status.equals("pending")||r.status.equals("accepted")))throw new IOException("Request already pending");
  Long last=recent.get(ip);if(last!=null&&now-last<30000)throw new IOException("Please wait before sending another request");if(requests.size()>=32){Iterator<Request> it=requests.values().iterator();while(it.hasNext())if(it.next().expires<now)it.remove();if(requests.size()>=32)throw new IOException("Too many requests");}
  Request r=new Request();r.id=UUID.randomUUID().toString();r.secret=secret;r.profile=profile;r.ip=ip;r.expires=now+60000;requests.put(r.id,r);recent.put(ip,now);return r;
 }
 public synchronized Request get(String id,String secret,String ip,long now)throws IOException{Request r=requests.get(id);if(!visible||r==null||!r.secret.equals(secret)||!r.ip.equals(ip)||r.expires<now)throw new IOException("Request expired or unavailable");return r;}
 public synchronized void cancel(String id,String secret,String ip,long now)throws IOException{Request r=get(id,secret,ip,now);if(!r.status.equals("paired"))r.status="cancelled";}
 public synchronized void decide(String id,String state,long now)throws IOException{if(!state.equals("accepted")&&!state.equals("declined")&&!state.equals("blocked"))throw new IOException("Invalid decision");Request r=requests.get(id);if(!visible||r==null||r.expires<now||!(r.status.equals("pending")||r.status.equals("accepted")))throw new IOException("Request unavailable");r.status=state;if(state.equals("accepted"))r.expires=now+60000;if(state.equals("blocked"))blocked.add(r.profile.optString("id"));}
 public synchronized ArrayList<Request> pending(long now){ArrayList<Request> out=new ArrayList<>();if(visible)for(Request r:requests.values())if(r.expires>now&&r.status.equals("pending"))out.add(r);return out;}
 public synchronized void block(String id){blocked.add(id);}
}
