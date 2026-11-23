from pathlib import Path
import subprocess,http.server,threading,hashlib,json,tempfile
root=Path.home()/'bplus-drop';test=root/'tests';test.mkdir(exist_ok=True)
old=subprocess.check_output(['git','show','a078633:app/src/com/bplus/share/UpdateHttp.java'],cwd=root/'share-repo',text=True)
p=Path('/tmp/updater101/com/bplus/share');p.mkdir(parents=True,exist_ok=True);(p/'UpdateHttp.java').write_text(old)
apk=(root/'BplusShare-1.0.2.apk').read_bytes();sha=hashlib.sha256(apk).hexdigest()
class H(http.server.BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def do_GET(self):
  if self.path=='/redirect':self.send_response(302);self.send_header('Location',f'http://localhost:{self.server.server_port}/asset');self.end_headers();return
  if self.path=='/api':
   b=json.dumps({'tag_name':'v1.0.2','draft':False,'prerelease':False,'assets':[{'name':'BplusShare-1.0.2.apk','size':len(apk),'digest':'sha256:'+sha,'state':'uploaded','browser_download_url':'https://github.com/basem-sadoun/Bplus-Share/releases/download/v1.0.2/BplusShare-1.0.2.apk'}]}).encode()
  else:b=apk
  self.send_response(200);self.send_header('Content-Length',str(len(b)));self.end_headers();self.wfile.write(b)
s=http.server.ThreadingHTTPServer(('127.0.0.1',0),H);threading.Thread(target=s.serve_forever,daemon=True).start()
java='''import com.bplus.share.UpdateHttp;import java.io.*;public class Upgrade101Test{public static void main(String[] a)throws Exception{if(!UpdateHttp.newer("v1.0.2","1.0.1"))throw new Exception("Version");String api=UpdateHttp.text(a[0]+"/api");if(!api.contains("BplusShare-1.0.2.apk"))throw new Exception("Asset");File f=new File("/tmp/candidate-102.apk");f.delete();UpdateHttp.download(a[0]+"/redirect",f,Long.parseLong(a[1]),a[2],(d,t,n)->{});if(!UpdateHttp.sha(f).equals(a[2]))throw new Exception("Digest");System.out.println("PASS exact 1.0.1 downloader: candidate 1.0.2 version, metadata fetch, redirected real APK download and SHA-256");}}'''
(test/'Upgrade101Test.java').write_text(java)
subprocess.run([str(Path.home()/'jdk17/bin/javac'),'-d','/tmp/upgrader101',str(p/'UpdateHttp.java'),str(test/'Upgrade101Test.java')],check=True)
r=subprocess.run([str(Path.home()/'jdk17/bin/java'),'-cp','/tmp/upgrader101','Upgrade101Test',f'http://127.0.0.1:{s.server_port}',str(len(apk)),sha],capture_output=True,text=True,timeout=20)
print(r.stdout);print(r.stderr);assert r.returncode==0
(test/'upgrade101-result.txt').write_text(r.stdout+'Signer and Android versionCode verified separately with apksigner/aapt2. Android installer/runtime not tested.\n')
s.shutdown()
