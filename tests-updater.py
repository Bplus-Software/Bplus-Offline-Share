import http.server, threading, socket, time, json, subprocess, hashlib
from pathlib import Path
root=Path.home()/'bplus-drop'
blob=bytes(range(256))*4096
counts={};ranges=[]
class Handler(http.server.BaseHTTPRequestHandler):
 def log_message(self,*a): pass
 def do_GET(self):
  path=self.path;counts[path]=counts.get(path,0)+1
  if path=='/api':
   data=json.dumps({'tag_name':'v1.0.2','assets':[{'name':'BplusShare.apk','size':len(blob),'browser_download_url':f'http://127.0.0.1:{self.server.server_port}/redirect'}]}).encode()
   self.send_response(200);self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data);return
  if path=='/redirect':
   self.send_response(302);self.send_header('Location',f'http://localhost:{self.server.server_port}/asset');self.end_headers();return
  if path in ('/stall','/stall-api'):
   self.send_response(200);self.send_header('Content-Length',str(len(blob)));self.end_headers();time.sleep(2);return
  if path=='/slow-api':
   self.send_response(200);self.end_headers()
   try:
    for x in range(50):self.wfile.write(b'x');self.wfile.flush();time.sleep(.1)
   except:pass
   return
  if path=='/missing':self.send_response(404);self.end_headers();return
  if path=='/loop':self.send_response(302);self.send_header('Location','/loop');self.end_headers();return
  offset=int(self.headers.get('Range','bytes=0-').split('=')[1].split('-')[0]);ranges.append((path,offset))
  if path=='/ignore':offset=0
  self.send_response(206 if offset else 200)
  if offset:self.send_header('Content-Range',f'bytes {offset}-{len(blob)-1}/{len(blob)}')
  self.send_header('Content-Length',str(len(blob)-offset));self.end_headers()
  if path=='/drop' and counts[path]==1:
   self.wfile.write(blob[:131072]);self.wfile.flush();self.connection.shutdown(socket.SHUT_RDWR);self.connection.close();return
  try:self.wfile.write(blob[offset:])
  except:pass
server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);threading.Thread(target=server.serve_forever,daemon=True).start()
java='''import com.bplus.share.UpdateHttp;import java.io.*;public class UpdateTests{
 static String base;static String digest;static File out=new File("/tmp/update-test.apk");
 static void download(String path,boolean partial)throws Exception{out.delete();if(partial){try(FileOutputStream o=new FileOutputStream(out)){o.write(new byte[131072]);}}UpdateHttp.download(base+path,out,1048576,digest,(d,t,a)->{});if(!UpdateHttp.sha(out).equals(digest))throw new Exception("Hash differs");System.out.println("PASS "+path);}
 static void failure(String path,boolean text)throws Exception{out.delete();long begin=System.currentTimeMillis();try{if(text)UpdateHttp.text(base+path);else UpdateHttp.download(base+path,out,1048576,digest,(d,t,a)->{});throw new RuntimeException("Expected failure "+path);}catch(IOException e){if(System.currentTimeMillis()-begin>4500)throw new RuntimeException("Unbounded request");System.out.println("PASS "+path+" visible reason: "+e.getMessage());}}
 public static void main(String[] a)throws Exception{base=a[0];digest=a[1];UpdateHttp.connectMs=300;UpdateHttp.readMs=300;UpdateHttp.requestMs=900;
 String api=UpdateHttp.text(base+"/api");if(!api.contains("v1.0.2")||!api.contains("/redirect"))throw new Exception("Fake API read failed");System.out.println("PASS fake GitHub API");
 download("/redirect",false);download("/drop",false);download("/ignore",true);failure("/stall",false);failure("/stall-api",true);failure("/slow-api",true);failure("/missing",false);failure("/missing",true);failure("/loop",true);
 if(!UpdateHttp.newer("v1.0.2","1.0.1")||UpdateHttp.newer("v1.0","1.0.1")||UpdateHttp.newer("v1.0.1","1.0.1"))throw new Exception("Version compare");System.out.println("PASS semantic version comparisons");
 out.delete();try{UpdateHttp.download(base+"/asset",out,1048576,"bad-digest",(d,t,n)->{});throw new RuntimeException("Hash mismatch accepted");}catch(IOException e){if(out.exists())throw new RuntimeException("Bad digest retained");System.out.println("PASS mismatched digest rejected");}
 out.delete();System.exit(0);
 }}'''
(root/'tests').mkdir(exist_ok=True);(root/'tests/UpdateTests.java').write_text(java)
subprocess.run([str(Path.home()/'jdk17/bin/javac'),'-d','/tmp/update-tests',str(root/'app/src/com/bplus/share/UpdateHttp.java'),str(root/'tests/UpdateTests.java')],check=True)
r=subprocess.run([str(Path.home()/'jdk17/bin/java'),'-cp','/tmp/update-tests','UpdateTests',f'http://127.0.0.1:{server.server_port}',hashlib.sha256(blob).hexdigest()],capture_output=True,text=True,timeout=25)
print(r.stdout);print(r.stderr)
assert r.returncode==0
assert ('/drop',131072) in ranges, ranges
assert counts['/missing']==2,counts
print('PASS Range request resumes at exact interrupted offset; HTTP 404 not retried')
(root/'tests/results.txt').write_text(r.stdout+'PASS Range offset confirmed; 404 no retry\n')
server.shutdown()
