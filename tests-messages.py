from pathlib import Path
import subprocess,threading,http.server,json,time
root=Path.home()/'bplus-drop';tmp=Path('/tmp/message-tests');(tmp/'com/bplus/share').mkdir(parents=True,exist_ok=True)
(tmp/'com/bplus/share/Store.java').write_text('package com.bplus.share; public class Store { public static String get(String k,String d){return "test-device";} public static String deviceName(){return "test";} public static String token(){return "test";} }')
(tmp/'com/bplus/share/Save.java').write_text('package com.bplus.share;public class Save{public static String mime(String n){return "application/octet-stream";}public static class Target{public android.net.Uri uri;public java.io.OutputStream out;public void abort(){}public void complete(){}}public static Target create(android.content.Context c,String n){return null;}}')
(tmp/'com/bplus/share/Profile.java').write_text('package com.bplus.share;import org.json.*;public class Profile{public static JSONObject local()throws Exception{return new JSONObject();}}')
java='''import com.bplus.share.Net;public class MessageTests{public static void main(String[] a)throws Exception{int p=Integer.parseInt(a[0]);if(!Net.sendMsg("127.0.0.1",p,"test-ok","hello"))throw new Exception(Net.lastError());System.out.println("PASS message delivered HTTP 200");if(Net.sendMsg("127.0.0.1",p,"test-forbidden","hello")||!Net.lastError().equals("HTTP 403"))throw new Exception("HTTP reason missing");System.out.println("PASS HTTP 403 preserved");if(Net.sendMsg("127.0.0.1",1,"none","hello")||!Net.lastError().contains("ConnectException"))throw new Exception("Connection reason missing");System.out.println("PASS refused connection reason");if(Net.ping("127.0.0.1",p,"x")==null)throw new Exception("Ping");System.out.println("PASS connectivity ping");long start=System.currentTimeMillis();if(Net.sendMsg("127.0.0.1",p,"test-stall","hello")||!Net.lastError().contains("SocketTimeoutException")||System.currentTimeMillis()-start>11000)throw new Exception("Timeout");System.out.println("PASS bounded stalled-message timeout with reason");}}'''
(tmp/'MessageTests.java').write_text(java)
class H(http.server.BaseHTTPRequestHandler):
 def log_message(self,*a):pass
 def do_GET(self):
  b=b'{"id":"peer","name":"Test"}';self.send_response(200);self.send_header('Content-Length',str(len(b)));self.end_headers();self.wfile.write(b)
 def do_POST(self):
  data=json.loads(self.rfile.read(int(self.headers['Content-Length'])));assert data['text']=='hello' and data['from']=='test-device'
  if 'stall' in self.path:time.sleep(10);return
  self.send_response(403 if 'forbidden' in self.path else 200);self.send_header('Content-Length','0');self.end_headers()
s=http.server.ThreadingHTTPServer(('127.0.0.1',0),H);threading.Thread(target=s.serve_forever,daemon=True).start()
android=Path.home()/'android-sdk/platforms/android-34/android.jar'
subprocess.run([str(Path.home()/'jdk17/bin/javac'),'-cp',str(android)+':/tmp/org-json.jar','-d',str(tmp),str(tmp/'com/bplus/share/Store.java'),str(tmp/'com/bplus/share/Save.java'),str(tmp/'com/bplus/share/Profile.java'),str(root/'app/src/com/bplus/share/Net.java'),str(tmp/'MessageTests.java')],check=True)
r=subprocess.run([str(Path.home()/'jdk17/bin/java'),'-cp',str(tmp)+':/tmp/org-json.jar:'+str(android),'MessageTests',str(s.server_port)],capture_output=True,text=True,timeout=18)
print(r.stdout);print(r.stderr);assert r.returncode==0
(root/'tests/message-network-results.txt').write_text(r.stdout+'JVM network tests, not Android UI or OS share-sheet tests.\n')
s.shutdown()
