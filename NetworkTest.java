package com.bplus.share;
import org.json.*;import com.sun.net.httpserver.*;import java.net.*;import java.util.*;import java.io.*;import java.lang.reflect.*;
public class NetworkTest {
 static int tries,stored;static String wire;static boolean read;
 static void due()throws Exception{Field f=Outbox.class.getDeclaredField("due");f.setAccessible(true);((Map)f.get(null)).clear();}
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);}
 public static void main(String[] args)throws Exception{
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/sync",e->{try{JSONObject req=new JSONObject(new String(e.getRequestBody().readAllBytes(),"UTF-8"));JSONObject res=new JSONObject().put("v",2).put("profile",new JSONObject().put("id","remote").put("name","Changed remote")).put("capabilities",new JSONArray().put("durable-text"));String op=req.getString("op");if(op.equals("send")){tries++;if(wire==null){wire=req.getString("wire");stored++;}else if(!wire.equals(req.getString("wire")))throw new AssertionError("wire changed");if(tries==1){e.close();return;}res.put("wire",wire).put("status","delivered");}if(op.equals("status"))res.put("items",new JSONArray().put(new JSONObject().put("wire",wire).put("status",read?"read":"delivered")));byte[] b=res.toString().getBytes("UTF-8");e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);}catch(Exception ex){throw new RuntimeException(ex);}finally{e.close();}});server.start();
  try{Store.peer=new JSONObject().put("id","remote").put("ip","127.0.0.1").put("port",server.getAddress().getPort()).put("token","remote-token");
   JSONObject m=Outbox.enqueue("remote","lost ack synthetic test");Outbox.stop();Thread.sleep(100);due();Outbox.sweep();
   ok(stored==1,"lost ACK stores only one remote message");due();Outbox.sweep();
   ok(tries>=2&&stored==1,"automatic retry reuses stable wire and does not duplicate remote message");
   ok(Store.thread("remote").getJSONObject(0).getString("state").equals("delivered"),"ACK establishes delivered, never read");
   read=true;due();Outbox.sweep();ok(Store.thread("remote").getJSONObject(0).getString("state").equals("read"),"status poll advances to read");
   Store.peer=null;int before=tries;due();Outbox.sweep();ok(tries==before,"removed peer gets no further queue requests");
   System.out.println("Actual HTTP client/worker exercised against local synthetic server. Android disk/lifecycle/phone networks unverified.");
  }finally{Outbox.stop();server.stop(0);}
 }
}
