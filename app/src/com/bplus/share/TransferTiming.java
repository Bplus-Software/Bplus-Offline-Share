package com.bplus.share;
import org.json.*;import android.os.SystemClock;
public final class TransferTiming{
 private static final ThreadLocal<TransferTiming> current=new ThreadLocal<>();private final long start=SystemClock.elapsedRealtime();private long opened,first,last,ack,readNs,writeNs,callbackNs,bytes;private int http;
 public static TransferTiming begin(){TransferTiming t=new TransferTiming();current.set(t);return t;}public static JSONObject latest(){TransferTiming t=current.get();return t==null?new JSONObject():t.json();}
 public void opened(){opened=SystemClock.elapsedRealtime()-start;}public void chunk(long n,long read,long write,long callback){long at=SystemClock.elapsedRealtime()-start;if(bytes==0)first=at;last=at;bytes+=n;readNs+=read;writeNs+=write;callbackNs+=callback;}public void ack(int code){ack=SystemClock.elapsedRealtime()-start;http=code;}
 public JSONObject json(){JSONObject out=new JSONObject();try{out.put("bytes",bytes).put("connectMs",opened).put("firstByteMs",first).put("lastByteMs",last).put("ackMs",ack).put("http",http).put("sourceReadMs",readNs/1000000).put("socketWriteMs",writeNs/1000000).put("progressCallbackMs",callbackNs/1000000);}catch(Exception ignored){}return out;}
}
