package com.bplus.share;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.concurrent.*;

/** Shared by the Android updater and the JVM fault-injection tests. */
public final class UpdateHttp {
 public interface Progress { void onProgress(long done,long total,int attempt); }
 public static int connectMs=8000, readMs=10000, requestMs=90000;
 private static final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r);t.setDaemon(true);return t;});
 private static final class Response implements AutoCloseable {
  HttpURLConnection c; ScheduledFuture<?> timeout;
  Response(HttpURLConnection c){this.c=c;timeout=timer.schedule(c::disconnect,requestMs,TimeUnit.MILLISECONDS);}
  public void close(){timeout.cancel(false);c.disconnect();}
 }
 private static Response open(String address,long offset) throws IOException {
  URL url=new URL(address);
  for(int redirects=0;redirects<7;redirects++){
   if(!url.getProtocol().equals("https")&&!url.getProtocol().equals("http"))throw new IOException("Unsupported URL protocol");
   HttpURLConnection c=(HttpURLConnection)url.openConnection();
   c.setConnectTimeout(connectMs);c.setReadTimeout(readMs);c.setInstanceFollowRedirects(false);
   c.setRequestProperty("User-Agent","BplusShare-Updater/1.1.3");c.setRequestProperty("Accept-Encoding","identity");
   if(offset>0)c.setRequestProperty("Range","bytes="+offset+"-");
   Response r=new Response(c);
   try{
    int code=c.getResponseCode();
    if(code==301||code==302||code==303||code==307||code==308){
     String location=c.getHeaderField("Location");if(location==null)throw new IOException("Redirect missing Location");
     URL next=new URL(url,location);
     if(url.getProtocol().equals("https")&&!next.getProtocol().equals("https"))throw new IOException("Insecure redirect rejected");
     r.close();url=next;continue;
    }
    return r;
   }catch(IOException e){r.close();throw e;}
  }
  throw new IOException("Too many redirects");
 }
 public static String text(String url) throws IOException {
  try(Response r=open(url,0)){
   int code=r.c.getResponseCode();if(code!=200)throw new IOException("HTTP "+code);
   ByteArrayOutputStream out=new ByteArrayOutputStream();
   try(InputStream in=r.c.getInputStream()){
    byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2097152)throw new IOException("API response too large");}
   }
   return out.toString("UTF-8");
  }
 }
 public static void download(String url,File file,long expected,String digest,Progress progress)throws IOException{
  if(expected<=0)throw new IOException("Missing APK size");
  if(file.length()>expected&&!file.delete())throw new IOException("Cannot reset partial download");
  IOException last=null;
  for(int attempt=1;attempt<=3;attempt++){
   long offset=file.length();
   try{
    if(offset<expected){
     progress.onProgress(offset,expected,attempt);
     try(Response r=open(url,offset)){
      int code=r.c.getResponseCode();
      if(code!=200&&code!=206)throw new IOException("HTTP "+code);
      boolean append=code==206&&offset>0;
      if(code==206){String range=r.c.getHeaderField("Content-Range");if(range==null||!range.startsWith("bytes "+offset+"-")||!range.endsWith("/"+expected))throw new IOException("Invalid resume Content-Range");}
      if(!append)offset=0;
      long length=r.c.getContentLengthLong();if(length>=0&&length!=expected-offset)throw new IOException("APK length differs from release metadata");
      try(InputStream in=r.c.getInputStream();FileOutputStream out=new FileOutputStream(file,append)){
       byte[] b=new byte[65536];int n;long done=offset;
       while((n=in.read(b))!=-1){if(done+n>expected)throw new IOException("APK larger than expected");out.write(b,0,n);done+=n;progress.onProgress(done,expected,attempt);}
       out.getFD().sync();if(done!=expected)throw new IOException("Download interrupted at "+done+" / "+expected+" bytes");
      }
     }
    }
    if(digest!=null&&!digest.isEmpty()&&!sha(file).equalsIgnoreCase(digest.replace("sha256:",""))){file.delete();throw new IOException("SHA-256 mismatch; download discarded");}
    return;
   }catch(IOException e){last=e;progress.onProgress(file.length(),expected,attempt);if(e.getMessage()!=null&&e.getMessage().startsWith("HTTP 4"))break;}
  }
  throw new IOException("Download failed after retries: "+(last==null?"unknown":last.getClass().getSimpleName()+": "+last.getMessage()),last);
 }
 public static String sha(File f)throws IOException{
  try{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format("%02x",b&255));return s.toString();}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
 }
 public static boolean newer(String remote,String local)throws IOException{
  try{String[] a=remote.replaceFirst("^[vV]","").split("\\."),b=local.split("\\.");for(int i=0;i<Math.max(a.length,b.length);i++){int x=i<a.length?Integer.parseInt(a[i]):0,y=i<b.length?Integer.parseInt(b[i]):0;if(x!=y)return x>y;}return false;}catch(Exception e){throw new IOException("Unrecognized release version");}
 }
}
