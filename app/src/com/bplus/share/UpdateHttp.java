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
 public static final class Control implements AutoCloseable{
  public final long started=System.currentTimeMillis();public volatile long lastByte=started;public volatile String stage="connecting",host="";public volatile int code;private volatile boolean cancelled,expired;private volatile HttpURLConnection connection;private ScheduledFuture<?> guard;
  public Control(){guard=timer.scheduleAtFixedRate(()->{long now=System.currentTimeMillis();if(now-started>120000||now-lastByte>25000){expired=true;HttpURLConnection c=connection;if(c!=null)c.disconnect();}},1,1,TimeUnit.SECONDS);}
  public void cancel(){cancelled=true;HttpURLConnection c=connection;if(c!=null)timer.execute(c::disconnect);}
  public void check()throws IOException{if(cancelled)throw new InterruptedIOException("Download cancelled; partial file kept");if(expired||System.currentTimeMillis()-started>120000)throw new IOException("Download deadline or inactivity timeout; partial file kept");}
  public boolean cancelled(){return cancelled;}
  public void close(){if(guard!=null)guard.cancel(false);connection=null;}
 }
 private static final ThreadLocal<Control> active=new ThreadLocal<>();
 private static final class Response implements AutoCloseable {
  HttpURLConnection c; ScheduledFuture<?> timeout;
  Response(HttpURLConnection c){this.c=c;timeout=timer.schedule(c::disconnect,requestMs,TimeUnit.MILLISECONDS);}
  public void close(){timeout.cancel(false);c.disconnect();}
 }
 private static Response open(String address,long offset) throws IOException {
  URL url=new URL(address);Control control=active.get();
  for(int redirects=0;redirects<7;redirects++){
   if(!url.getProtocol().equals("https")||url.getUserInfo()!=null||url.getRef()!=null||!(url.getPort()==-1||url.getPort()==443))throw new IOException("HTTPS required for updates");
   String host=url.getHost().toLowerCase(java.util.Locale.ROOT);if(!(host.equals("api.github.com")||host.equals("github.com")||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com")))throw new IOException("Unexpected update host");
   if(control!=null){control.check();control.stage="connecting";control.host=url.getHost();control.code=0;}
   HttpURLConnection c=(HttpURLConnection)url.openConnection();if(control!=null)control.connection=c;
   c.setConnectTimeout(connectMs);c.setReadTimeout(readMs);c.setInstanceFollowRedirects(false);
   c.setRequestProperty("User-Agent","BplusOfflineShare-Updater");c.setRequestProperty("Accept-Encoding","identity");
   if(offset>0)c.setRequestProperty("Range","bytes="+offset+"-");
   Response r=new Response(c);
   try{
    int code=c.getResponseCode();if(control!=null){control.check();control.code=code;control.stage="waiting";}
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
 public static final class HttpFailure extends IOException{public final int status;public final long resetAt;public HttpFailure(int s,long reset){super("HTTP "+s);status=s;resetAt=reset;}}
 private static long reset(HttpURLConnection c){long at=0;try{at=Long.parseLong(c.getHeaderField("X-RateLimit-Reset"))*1000;}catch(Exception ignored){}String retry=c.getHeaderField("Retry-After");try{at=Math.max(at,System.currentTimeMillis()+Long.parseLong(retry)*1000);}catch(Exception ignored){try{at=Math.max(at,java.time.ZonedDateTime.parse(retry,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli());}catch(Exception ignored2){}}return at;}
 public static String text(String url) throws IOException {
  try(Response r=open(url,0)){
   int code=r.c.getResponseCode();if(code!=200)throw new HttpFailure(code,reset(r.c));
   ByteArrayOutputStream out=new ByteArrayOutputStream();
   try(InputStream in=r.c.getInputStream()){
    byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2097152)throw new IOException("API response too large");}
   }
   return out.toString("UTF-8");
  }
 }
 public static void download(String url,File file,long expected,String digest,Progress progress)throws IOException{try(Control c=new Control()){download(url,file,expected,digest,progress,c);}}
 public static void download(String url,File file,long expected,String digest,Progress progress,Control control)throws IOException{active.set(control);try{downloadBody(url,file,expected,digest,progress,control);}finally{active.remove();control.close();}}
 private static void downloadBody(String url,File file,long expected,String digest,Progress progress,Control control)throws IOException{
  if(expected<=0)throw new IOException("Missing APK size");
  if(file.length()>expected&&!file.delete())throw new IOException("Cannot reset partial download");
  IOException last=null;
  for(int attempt=1;attempt<=3;attempt++){
   control.check();long offset=file.length();
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
       while((n=in.read(b))!=-1){control.check();control.lastByte=System.currentTimeMillis();control.stage="downloading";if(done+n>expected)throw new IOException("APK larger than expected");out.write(b,0,n);done+=n;progress.onProgress(done,expected,attempt);}
       out.getFD().sync();if(done!=expected)throw new IOException("Download interrupted at "+done+" / "+expected+" bytes");
      }
     }
    }
    control.check();control.stage="verifying";if(digest!=null&&!digest.isEmpty()&&!sha(file).equalsIgnoreCase(digest.replace("sha256:",""))){file.delete();throw new IOException("SHA-256 mismatch; download discarded");}
    return;
   }catch(IOException e){control.check();last=e;progress.onProgress(file.length(),expected,attempt);if(e.getMessage()!=null&&e.getMessage().startsWith("HTTP 4"))break;}
  }
  throw new IOException("Download failed after retries: "+(last==null?"unknown":last.getClass().getSimpleName()+": "+last.getMessage()),last);
 }
 public static String sha(File f)throws IOException{
  try{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format("%02x",b&255));return s.toString();}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
 }
 public static boolean newer(String remote,String local)throws IOException{
  try{String[] a=remote.replaceFirst("^[vV]","").split("\\."),b=local.replaceFirst("^[vV]","").split("\\.");for(int i=0;i<Math.max(a.length,b.length);i++){int x=i<a.length?Integer.parseInt(a[i]):0,y=i<b.length?Integer.parseInt(b[i]):0;if(x!=y)return x>y;}return false;}catch(Exception e){throw new IOException("Unrecognized release version");}
 }
}
