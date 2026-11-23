from pathlib import Path
import subprocess,http.server,threading,json,hashlib
r=Path.home()/'bplus-drop';t=Path('/tmp/upgrader1112');p=t/'com/bplus/share';p.mkdir(parents=True,exist_ok=True)
old=subprocess.check_output(['git','show','a05a40a1c787f8081f52c5e89b4acd09b869a024:app/src/com/bplus/share/UpdateHttp.java'],cwd=r/'share-repo',text=True);(p/'UpdateHttp.java').write_text(old)
apk=(r/'BplusShare.apk').read_bytes();sha=hashlib.sha256(apk).hexdigest()
class H(http.server.BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def do_GET(self):
  if self.path=='/redirect':self.send_response(302);self.send_header('Location',f'http://localhost:{self.server.server_port}/asset');self.end_headers();return
  b=json.dumps({'tag_name':'v1.1.13','draft':False,'prerelease':False,'assets':[{'name':'BplusShare-1.1.13.apk','size':len(apk),'digest':'sha256:'+sha}]}).encode() if self.path=='/api' else apk
  self.send_response(200);self.send_header('Content-Length',str(len(b)));self.end_headers();self.wfile.write(b)
s=http.server.ThreadingHTTPServer(('127.0.0.1',0),H);threading.Thread(target=s.serve_forever,daemon=True).start()
(t/'Upgrade1112Test.java').write_text('''import com.bplus.share.UpdateHttp;import java.io.*;public class Upgrade1112Test{public static void main(String[] a)throws Exception{if(!UpdateHttp.newer("v1.1.13","1.1.12"))throw new Exception("Version");if(!UpdateHttp.text(a[0]+"/api").contains("BplusShare-1.1.13.apk"))throw new Exception("Metadata");File f=new File("/tmp/candidate110.apk");f.delete();UpdateHttp.download(a[0]+"/redirect",f,Long.parseLong(a[1]),a[2],(d,t,n)->{});if(!UpdateHttp.sha(f).equals(a[2]))throw new Exception("Digest");System.out.println("PASS exact released 1.1.12 downloader: 1.1.13 version recognition, metadata, redirected candidate APK and SHA-256. Local HTTP harness, not live release or Android installer.");}}''')
j=Path.home()/'jdk17/bin';subprocess.run([str(j/'javac'),'-d',str(t),str(p/'UpdateHttp.java'),str(t/'Upgrade1112Test.java')],check=True)
x=subprocess.run([str(j/'java'),'-cp',str(t),'Upgrade1112Test',f'http://127.0.0.1:{s.server_port}',str(len(apk)),sha],capture_output=True,text=True,check=True,timeout=25);print(x.stdout);(r/'tests/upgrade1112-result.txt').write_text(x.stdout);s.shutdown()
