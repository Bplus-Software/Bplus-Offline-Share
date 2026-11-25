package com.bplus.share;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

public class Net {

    private static final ThreadLocal<String> error = new ThreadLocal<>();private static final ThreadLocal<Integer> httpStatus=new ThreadLocal<>();public static int lastHttp(){Integer c=httpStatus.get();return c==null?0:c;}
    public static String lastError(){String e=error.get();return e==null?"No response":e;}
    private static void failed(Exception e){error.set(e.getClass().getSimpleName()+": "+(e instanceof java.net.SocketTimeoutException?"Connection timed out":e instanceof java.net.ConnectException?"Connection refused or unreachable":e instanceof java.net.NoRouteToHostException?"No route to device":"Network or response error"));}

    public interface Progress { void onProgress(long done, long total); }

    public static String localIp(Context c) {
        try {
            android.net.ConnectivityManager cm=(android.net.ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
            for(android.net.Network network:cm.getAllNetworks()){
                android.net.NetworkCapabilities cap=cm.getNetworkCapabilities(network);
                if(cap==null || !cap.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI))continue;
                android.net.LinkProperties links=cm.getLinkProperties(network);
                if(links!=null)for(android.net.LinkAddress link:links.getLinkAddresses()){
                    InetAddress a=link.getAddress();if(a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress())return a.getHostAddress();
                }
            }
        }catch(Exception ignored){}

        try {
            WifiManager wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();
            if (ip != 0)
                return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
        } catch (Exception ignored) {}
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni=en.nextElement();
                String name=ni.getName().toLowerCase(java.util.Locale.ROOT);
                if(!ni.isUp() || !(name.startsWith("wlan") || name.startsWith("wifi") || name.startsWith("ap") || name.startsWith("swlan") || name.startsWith("softap"))) continue;
                Enumeration<InetAddress> addrs=ni.getInetAddresses();
                while(addrs.hasMoreElements()) { InetAddress a=addrs.nextElement(); if(!a.isLoopbackAddress() && !a.isLinkLocalAddress() && a instanceof Inet4Address) return a.getHostAddress(); }
            }
        } catch(Exception ignored) {}
        return null;
    }

    private static HttpURLConnection open(String method, String url) throws IOException {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setRequestMethod(method);
        h.setConnectTimeout(5000);
        h.setReadTimeout(60000);
        h.setRequestProperty("Connection", "close");
        return h;
    }

    public static JSONObject contactProfile(JSONObject peer)throws Exception{HttpURLConnection h=open("POST","http://"+peer.getString("ip")+":"+peer.optInt("port",Store.PORT)+"/profile-details");try{h.setReadTimeout(6000);h.setDoOutput(true);JSONObject b=new JSONObject().put("from",Store.get("deviceId","")).put("senderToken",Store.token()).put("targetToken",peer.getString("token"));byte[] bytes=b.toString().getBytes("UTF-8");h.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=h.getOutputStream()){out.write(bytes);}if(h.getResponseCode()!=200)throw new IOException("Profile unavailable");ByteArrayOutputStream buffer=new ByteArrayOutputStream();try(InputStream in=h.getInputStream()){byte[] chunk=new byte[512];int count;while((count=in.read(chunk))>0){if(buffer.size()+count>2000)throw new IOException("Profile too large");buffer.write(chunk,0,count);}}String raw=buffer.toString("UTF-8");JSONObject result=new JSONObject(raw);if(!peer.getString("id").equals(result.optString("id")))throw new IOException("Wrong peer");return result;}finally{h.disconnect();}}

    public static JSONObject ping(String ip,int port,String token){
        error.remove();java.net.HttpURLConnection h=null;
        try{h=open("GET","http://"+ip+":"+port+"/ping?token="+token);h.setReadTimeout(8000);int code=h.getResponseCode();if(code!=200){error.set("HTTP "+code);return null;}return new JSONObject(readAll(h.getInputStream()));}catch(Exception e){failed(e);return null;}finally{if(h!=null)h.disconnect();}
    }

    public static JSONObject pair(String ip, int port, String qrToken) {
        return pair(ip,port,qrToken,"");
    }
    public static JSONObject pair(String ip,int port,String qrToken,String epoch){
        error.remove();
        try {
            JSONObject body = Profile.local();if(epoch.matches("[a-f0-9]{64}"))body.put("pairEpoch",epoch);
            body.put("token", Store.token());
            HttpURLConnection h = open("POST", "http://" + ip + ":" + port + "/pair?token=" + qrToken);
            h.setDoOutput(true);
            h.setRequestProperty("Content-Type", "application/json");
            byte[] b = body.toString().getBytes("UTF-8");
            h.setFixedLengthStreamingMode(b.length);
            h.getOutputStream().write(b);
            h.getOutputStream().close();
            h.setReadTimeout(10000);
            if (h.getResponseCode() != 200) {error.set("HTTP "+h.getResponseCode());return null;}
            return new JSONObject(readAll(h.getInputStream()));
        } catch (Exception e) { failed(e); return null; }
    }

    public static JSONArray manifest(String ip, int port, String token, String batch) {
        error.remove();
        try {
            HttpURLConnection h = open("GET", "http://" + ip + ":" + port + "/manifest?token=" + token + "&batch=" + batch);
            h.setReadTimeout(10000);
            if (h.getResponseCode() != 200) {error.set("HTTP "+h.getResponseCode());return null;}
            return new JSONObject(readAll(h.getInputStream())).getJSONArray("files");
        } catch (Exception e) { failed(e); return null; }
    }

    private static final ThreadLocal<String> receivedUri=new ThreadLocal<>();
    public static String receivedUri(){return receivedUri.get();}

    // pull a file from a sender's batch
    public static boolean download(String ip, int port, String token, String batch, int idx,
                                   String name, long size, Context c, Progress pr) {
        receivedUri.remove();
        Save.Target target=null;
        try {
            HttpURLConnection h = open("GET", "http://" + ip + ":" + port + "/file?token=" + token + "&batch=" + batch + "&i=" + idx);
            if (h.getResponseCode() != 200) return false;
            InputStream in = new BufferedInputStream(h.getInputStream());
            target=Save.create(c,name);
            OutputStream out=target.out;
            byte[] buf = new byte[65536];
            long done = 0; int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                done += n;
                if (pr != null) pr.onProgress(done, size);
            }
            in.close();
            if(done!=size)throw new IOException("Incomplete transfer");
            target.complete();receivedUri.set(target.uri.toString());
            return true;
        } catch (Exception e) { if(target!=null)target.abort();return false; }
    }

    // push a file to a peer's inbox
    public static boolean send(Context c, String ip, int port, String token, Uri uri, String name, long size, Progress pr) {
        return send(c,ip,port,token,uri,name,size,pr,"","");}
    private static final java.util.concurrent.ConcurrentHashMap<String,HttpURLConnection> autoConnections=new java.util.concurrent.ConcurrentHashMap<>();
    public static void cancelAuto(String id){HttpURLConnection h=autoConnections.remove(id);if(h!=null)new Thread(h::disconnect).start();}
    public static boolean send(Context c,String ip,int port,String token,Uri uri,String name,long size,Progress pr,String wire,String key){return sendGuarded(c,ip,port,token,uri,name,size,pr,wire,key,"",()->true);}
    public static boolean sendGuarded(Context c,String ip,int port,String token,Uri uri,String name,long size,Progress pr,String wire,String key,String autoId,java.util.function.BooleanSupplier allowed){
        return sendJob(c,ip,port,token,uri,name,size,pr,wire,key,autoId,allowed,"");
    }
    public static boolean sendJob(Context c,String ip,int port,String token,Uri uri,String name,long size,Progress pr,String wire,String key,String autoId,java.util.function.BooleanSupplier allowed,String job){
        error.remove();httpStatus.remove();TransferTiming timing=TransferTiming.begin();HttpURLConnection h=null;
        try{
            if(!allowed.getAsBoolean())throw new IOException("Automatic send stopped");
            h=open("POST","http://"+ip+":"+port+"/recv?token="+token+"&name="+java.net.URLEncoder.encode(name,"UTF-8")+"&from="+Store.get("deviceId","")+"&wire="+wire+"&key="+key+"&senderToken="+Store.token()+"&job="+job);
            if(!autoId.isEmpty())autoConnections.put(autoId,h);
            h.setDoOutput(true);h.setRequestProperty("Content-Type","application/octet-stream");h.setFixedLengthStreamingMode(size);
            if(!allowed.getAsBoolean())throw new IOException("Automatic send stopped");
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new BufferedOutputStream(h.getOutputStream())){timing.opened();byte[] buf=new byte[65536];long done=0;int n;while(true){long r=System.nanoTime();n=in.read(buf);long w=System.nanoTime();if(n<=0)break;if(!allowed.getAsBoolean())throw new IOException("Automatic send stopped");out.write(buf,0,n);long callback=System.nanoTime();done+=n;if(pr!=null)pr.onProgress(done,size);timing.chunk(n,w-r,callback-w,System.nanoTime()-callback);}if(done!=size)throw new IOException("Source size changed");if(!allowed.getAsBoolean())throw new IOException("Automatic send stopped");out.flush();if(!allowed.getAsBoolean())throw new IOException("Automatic send stopped");int code=h.getResponseCode();httpStatus.set(code);timing.ack(code);boolean ok=code==200;if(!ok)error.set("HTTP "+code);return ok;}
        }catch(Exception e){failed(e);if(h!=null&&allowed.getAsBoolean())try{int status=h.getResponseCode();httpStatus.set(status);if(status>=400)error.set("HTTP "+status);}catch(Exception ignored){}return false;}finally{if(h!=null){if(!autoId.isEmpty())autoConnections.remove(autoId,h);h.disconnect();}}
    }

    public static boolean sendMsg(String ip,int port,String token,String text){
        return sendMsg(ip,port,token,text,"","");}
    public static boolean sendMsg(String ip,int port,String token,String text,String wire,String key){
        error.remove();HttpURLConnection h=null;
        try{
            JSONObject body=new JSONObject();body.put("text",text);body.put("from",Store.get("deviceId",""));body.put("wire",wire);body.put("key",key);body.put("senderToken",Store.token());
            h=open("POST","http://"+ip+":"+port+"/msg?token="+token);h.setReadTimeout(8000);h.setDoOutput(true);h.setRequestProperty("Content-Type","application/json");byte[] bytes=body.toString().getBytes("UTF-8");h.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=h.getOutputStream()){out.write(bytes);}int code=h.getResponseCode();if(code!=200){error.set("HTTP "+code);return false;}return true;
        }catch(Exception e){failed(e);return false;}finally{if(h!=null)h.disconnect();}
    }

    public static JSONObject history(String ip, int port, String token) {
        try {
            HttpURLConnection h = open("GET", "http://" + ip + ":" + port + "/history?token=" + token);
            if (h.getResponseCode() != 200) return null;
            return new JSONObject(readAll(h.getInputStream()));
        } catch (Exception e) { return null; }
    }

    static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return b.toString("UTF-8");
    }
}
