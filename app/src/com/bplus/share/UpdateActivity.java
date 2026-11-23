package com.bplus.share;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;
import org.json.*;

public class UpdateActivity extends Activity {
 private static final String API="https://api.github.com/repos/basem-sadoun/Bplus-Share/releases?per_page=100";
 private static final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static volatile boolean busy;
 private static volatile String phase="idle",detail="",version="",assetUrl="",digest="";
 private static volatile long size,bytes;
 private static volatile int attempt;
 private static volatile UpdateActivity visible;
 private LinearLayout box;
 private TextView status,info,primary;
 private ProgressBar progress;
 private boolean permissionPending;
 private static String current(Activity a){try{return a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;}catch(Exception e){return "1.1.12";}}
 private File apk(){return new File(getFilesDir(),"update.apk");}
 @Override protected void onCreate(Bundle b){super.onCreate(b);Ui.applyLocale(this);Ui.applyTheme(this);
  LinearLayout root=Ui.col(this);root.setBackgroundColor(Ui.bg);root.addView(Ui.topBar(this,getString(R.string.updater_title),true,null));
  ScrollView scroll=new ScrollView(this);box=Ui.col(this);Ui.pad(box,24,24,24,24,this);scroll.addView(box);root.addView(scroll,Ui.lpW1());setContentView(root);
  box.addView(Ui.t(this,"B+ Share",24,Ui.text,App.cairoB));box.addView(Ui.t(this,getString(R.string.installed_version,current(this)),13,Ui.dim,App.cairoSB));
  status=Ui.t(this,"",19,Ui.text,App.cairoB);Ui.pad(status,0,30,0,14,this);box.addView(status);
  progress=Ui.hbar(this,10);progress.setMax(1000);box.addView(progress,Ui.lp(-1,Ui.dp(this,10)));
  info=Ui.t(this,"",13,Ui.dim,App.cairoSB);Ui.pad(info,0,20,0,20,this);info.setTextIsSelectable(true);box.addView(info);
  primary=Ui.cta(this,getString(R.string.check_updates));primary.setGravity(Gravity.CENTER);box.addView(primary,Ui.lp(-1,-2));primary.setOnClickListener(v->{if(busy)return;if(phase.equals("found")||phase.equals("download_failed"))download();else if(phase.equals("ready"))install();else check();});
  TextView copy=Ui.ghost(this,getString(R.string.copy_details));Ui.pad(copy,12,16,12,16,this);box.addView(copy);copy.setOnClickListener(v->PairUi.copy(this,"B+ Share "+current(this)+"\nUpdater: "+phase+"\n"+detail+"\nRelease: "+version+"\nBytes: "+bytes+" / "+size+"\nAPI: "+API));
  TextView note=Ui.t(this,getString(R.string.updater_note),12,Ui.dim,App.cairoSB);Ui.pad(note,0,24,0,0,this);box.addView(note);visible=this;render();
 }
 @Override protected void onResume(){super.onResume();visible=this;render();if(permissionPending){permissionPending=false;if(getPackageManager().canRequestPackageInstalls())install();else{detail=getString(R.string.allow_install_help);render();}}}
 @Override protected void onPause(){if(visible==this)visible=null;super.onPause();}
 private static void refresh(){UpdateActivity a=visible;if(a!=null)a.runOnUiThread(a::render);}
 private void render(){
  if(isFinishing())return;
  int label=R.string.updater_idle;
  if(phase.equals("checking"))label=R.string.checking;
  else if(phase.equals("current"))label=R.string.updater_current;
  else if(phase.equals("found"))label=R.string.updater_found;
  else if(phase.equals("downloading"))label=R.string.updater_downloading;
  else if(phase.equals("verifying"))label=R.string.updater_verifying;
  else if(phase.equals("ready"))label=R.string.updater_ready;
  else if(phase.endsWith("failed"))label=R.string.updater_failed;
  status.setText(getString(label)+(version.isEmpty()?"":" · "+version));
  progress.setProgress((int)Math.min(1000,1000*bytes/Math.max(1,size)));
  String downloadInfo=(phase.equals("downloading")||phase.equals("download_failed"))?((100*bytes/Math.max(1,size))+"% · "+Ui.fmtSize(bytes)+" / "+Ui.fmtSize(size)+" · "+getString(R.string.attempt_number,attempt)+"\n"):"";
  info.setText(downloadInfo+detail);
  primary.setEnabled(!busy);primary.setAlpha(busy?.5f:1f);
  primary.setText(getString(phase.equals("ready")?R.string.install_update:(phase.equals("found")?R.string.download_update:(phase.equals("download_failed")?R.string.resume_download:R.string.check_updates))));
 }
 private void check(){
  busy=true;phase="checking";detail="";version="";render();
  worker.execute(()->{try{
   JSONObject release=ReleaseChoice.choose(new JSONArray(UpdateHttp.text(API)),Store.get("includeTestVersions","1").equals("1"),current(this));if(release==null){phase="current";detail=getString(R.string.installed_version,current(this));return;}
   version=release.getString("tag_name");
   if(!UpdateHttp.newer(version,current(this))){phase="current";detail=getString(R.string.installed_version,current(this));return;}
   JSONArray assets=release.getJSONArray("assets");JSONObject asset=null;
   for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);if(a.optString("name").endsWith(".apk")){if(asset!=null)throw new IOException("More than one APK; choose the release in GitHub");asset=a;}}
   if(asset==null)throw new IOException("Release has no APK asset");
   assetUrl=asset.getString("browser_download_url");
   java.net.URL u=new java.net.URL(assetUrl);
   if(!u.getProtocol().equals("https")||!u.getHost().equals("github.com")||!u.getPath().startsWith("/basem-sadoun/Bplus-Share/releases/download/"))throw new IOException("Unexpected release asset location");
   if(!asset.optString("state","uploaded").equals("uploaded"))throw new IOException("APK is not ready on GitHub");
   size=asset.getLong("size");if(size<=0)throw new IOException("Missing APK size");
   digest=asset.optString("digest","");if(digest.equals("null"))digest="";
   String key=version+"|"+assetUrl+"|"+size+"|"+digest;
   if(!Store.get("updateAsset","").equals(key)){if(apk().exists()&&!apk().delete())throw new IOException("Cannot discard old update");Store.set("updateAsset",key);}
   bytes=apk().length();phase="found";detail=getString(R.string.release_size,Ui.fmtSize(size))+"\nGitHub · basem-sadoun/Bplus-Share";
  }catch(Exception e){phase="check_failed";detail=reason(e);}finally{busy=false;refresh();}});
 }
 private void download(){busy=true;phase="downloading";detail=getString(R.string.keep_updater_open);render();
  worker.execute(()->{try{
   final long[] last={0};
   UpdateHttp.download(assetUrl,apk(),size,digest,(done,total,tryNo)->{bytes=done;attempt=tryNo;long now=System.currentTimeMillis();if(now-last[0]>180||done==total){last[0]=now;refresh();}});
   phase="verifying";detail=getString(R.string.verifying_signature);refresh();try{verifyApk();}catch(Exception invalid){apk().delete();bytes=0;throw invalid;}phase="ready";detail=getString(R.string.installer_help);
  }catch(Exception e){phase="download_failed";detail=reason(e);}finally{busy=false;refresh();}});
 }
 @SuppressWarnings("deprecation") private void verifyApk()throws Exception{
  PackageManager pm=getPackageManager();PackageInfo installed=pm.getPackageInfo(getPackageName(),PackageManager.GET_SIGNATURES);
  PackageInfo downloaded=pm.getPackageArchiveInfo(apk().getAbsolutePath(),PackageManager.GET_SIGNATURES);
  if(downloaded==null||!getPackageName().equals(downloaded.packageName))throw new IOException("Downloaded APK has the wrong package");
  if(downloaded.versionCode<=installed.versionCode)throw new IOException("Downloaded APK is not newer");
  if(!java.util.Arrays.equals(installed.signatures,downloaded.signatures))throw new IOException("Signing certificate does not match installed app");
 }
 private void install(){try{
  verifyApk();
  if(!getPackageManager().canRequestPackageInstalls()){
   detail=getString(R.string.allow_install_help);render();permissionPending=true;
   startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));return;
  }
  Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.parse("content://"+getPackageName()+".updates/update.apk"),"application/vnd.android.package-archive");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);
  detail=getString(R.string.installer_help);render();
 }catch(Exception e){detail=reason(e);render();}}
 private static String reason(Exception e){return e.getClass().getSimpleName()+": "+(e.getMessage()==null?"No details available":e.getMessage());}
}
