import com.bplus.share.*;import java.net.*;import java.io.*;import java.util.*;import java.nio.file.*;
public class TestOrgTransport118{
 static int mode=0,calls=0;static byte[] data="test301".getBytes();static List<String> paths=new ArrayList<>();
 static class C extends HttpURLConnection{C(URL u){super(u);calls++;paths.add(u.toString());}public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}public int getResponseCode(){if(mode==1)return 403;if(mode==2)return url.getPath().contains("Bplus-Software")?404:200;return url.getPath().contains("basem-sadoun")?301:200;}public String getHeaderField(String k){if(k.equals("Location"))return url.getHost().equals("api.github.com")?"https://api.github.com/repos/Bplus-Software/Bplus-Offline-Share/releases?per_page=100":"https://github.com/Bplus-Software/Bplus-Offline-Share/releases/download/v1.0.18/BplusShare-1.0.18.apk";return null;}public long getContentLengthLong(){return data.length;}public InputStream getInputStream(){return new ByteArrayInputStream(data);}}
 static void ok(boolean b){if(!b)throw new AssertionError(paths.toString());}
 public static void main(String[] a)throws Exception{URL.setURLStreamHandlerFactory(p->p.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){return new C(u);}}:null);
 ok(UpdateHttp.text("https://api.github.com/repos/basem-sadoun/Bplus-Offline-Share/releases?per_page=100").equals("test301"));ok(calls==2);
 File f=new File("/tmp/org118redirect.apk");Files.write(f.toPath(),data);String sha=UpdateHttp.sha(f);f.delete();calls=0;UpdateHttp.download("https://github.com/basem-sadoun/Bplus-Offline-Share/releases/download/v1.0.18/BplusShare-1.0.18.apk",f,data.length,"sha256:"+sha,(x,y,z)->{});ok(calls==2&&Arrays.equals(Files.readAllBytes(f.toPath()),data));
 mode=1;calls=0;try{UpdateAsset.fetch("/releases?per_page=100");throw new AssertionError();}catch(UpdateHttp.HttpFailure e){ok(e.status==403&&calls==1);}
 mode=2;calls=0;ok(UpdateAsset.fetch("/releases?per_page=100").equals("test301")&&calls==2);
 System.out.println("PASS mock actual API301 and APK301 transport;404 fallback;403 does not fan out to aliases");
 }
}
