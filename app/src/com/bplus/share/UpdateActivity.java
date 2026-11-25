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
 private static final String API=UpdateAsset.API;
 private static final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static volatile boolean busy;
 private static volatile String phase="idle",detail="",version="",assetUrl="",digest="";
 private static volatile long size,bytes;
 private static volatile int attempt,expectedCode;
 private static volatile boolean installRequested;
 private static volatile UpdateActivity visible;
 private LinearLayout box;
 private TextView status,info,primary;
 private ProgressBar progress;
 private static volatile UpdateHttp.Control transfer;private final Handler clock=new Handler(Looper.getMainLooper());private TextView cancelTransfer;private final Runnable tick=new Runnable(){public void run(){if(visible!=UpdateActivity.this)return;render();if(busy)clock.postDelayed(this,1000);}};private boolean permissionPending;private Switch tests;

 private File apk(){return new File(getFilesDir(),"update.apk");}
 @Override protected void onCreate(Bundle b){super.onCreate(b);Ui.applyLocale(this);Ui.applyTheme(this);if(b!=null)permissionPending=b.getBoolean("permissionPending",false);
  LinearLayout root=Ui.col(this);root.setBackgroundColor(Ui.bg);root.addView(Ui.topBar(this,getString(R.string.updater_title),true,null));
  ScrollView scroll=new ScrollView(this);box=Ui.col(this);Ui.pad(box,24,24,24,24,this);scroll.addView(box);root.addView(scroll,Ui.lpW1());setContentView(root);
  box.addView(Ui.t(this,getString(R.string.app_name),24,Ui.text,App.cairoB));box.addView(Ui.t(this,getString(R.string.installed_version,AppVersion.display(this)),13,Ui.dim,App.cairoSB));
  tests=new Switch(this);tests.setText(R.string.include_test_versions);tests.setTextColor(Ui.text);tests.setTypeface(App.cairo);tests.setChecked(Store.get("includeTestVersions","1").equals("1"));box.addView(tests,Ui.lp(-1,Ui.dp(this,52)));box.addView(Ui.t(this,getString(R.string.include_test_versions_note),12,Ui.dim,App.cairo));tests.setOnCheckedChangeListener((button,on)->{if(busy)return;Store.set("includeTestVersions",on?"1":"0");installRequested=false;phase="idle";version="";assetUrl="";expectedCode=0;bytes=0;check();});
  Switch auto=new Switch(this);auto.setText(R.string.auto_update_check);auto.setTextColor(Ui.text);auto.setTypeface(App.cairo);auto.setChecked(Store.get("autoUpdateCheck","0").equals("1"));auto.setMinHeight(Ui.dp(this,52));box.addView(auto,Ui.lp(-1,-2));box.addView(Ui.t(this,getString(R.string.auto_update_check_note),12,Ui.dim,App.cairo));auto.setOnCheckedChangeListener((button,on)->Store.set("autoUpdateCheck",on?"1":"0"));
  LinearLayout card=Ui.col(this);card.setBackground(Ui.roundBorder(Ui.card,Ui.border,16,this));Ui.pad(card,18,18,18,18,this);LinearLayout.LayoutParams cardLp=Ui.lp(-1,-2);cardLp.topMargin=Ui.dp(this,22);box.addView(card,cardLp);
  status=Ui.t(this,"",19,Ui.text,App.cairoB);Ui.pad(status,0,30,0,14,this);card.addView(status);
  progress=Ui.hbar(this,10);progress.setMax(1000);card.addView(progress,Ui.lp(-1,Ui.dp(this,10)));
  info=Ui.t(this,"",13,Ui.dim,App.cairoSB);Ui.pad(info,0,20,0,20,this);info.setTextIsSelectable(true);card.addView(info);
  primary=Ui.cta(this,getString(R.string.check_updates));primary.setGravity(Gravity.CENTER);card.addView(primary,Ui.lp(-1,-2));primary.setOnClickListener(v->{if(busy)return;if(phase.equals("found")||phase.equals("download_failed"))download();else if(phase.equals("ready"))install();else check();});
  cancelTransfer=Ui.ghost(this,getString(R.string.update_cancel_download));LinearLayout.LayoutParams cancelLp=Ui.lp(-1,-2);cancelLp.topMargin=Ui.dp(this,12);card.addView(cancelTransfer,cancelLp);cancelTransfer.setOnClickListener(v->{UpdateHttp.Control c=transfer;if(c!=null)c.cancel();});
  TextView releases=Ui.ghost(this,getString(R.string.update_open_releases));releases.setBackgroundColor(android.graphics.Color.TRANSPARENT);Ui.pad(releases,12,16,12,16,this);LinearLayout.LayoutParams releaseLp=Ui.lp(-1,-2);releaseLp.topMargin=Ui.dp(this,16);box.addView(releases,releaseLp);releases.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/"+UpdateAsset.REPO+"/releases"))));
  TextView copy=Ui.ghost(this,getString(R.string.copy_details));copy.setBackgroundColor(android.graphics.Color.TRANSPARENT);Ui.pad(copy,12,16,12,16,this);LinearLayout.LayoutParams copyLp=Ui.lp(-1,-2);copyLp.topMargin=Ui.dp(this,8);if(DeveloperMode.enabled())box.addView(copy,copyLp);copy.setOnClickListener(v->PairUi.copy(this,getString(R.string.app_name)+" "+AppVersion.display(this)+"\nUpdater: "+phase+"\n"+detail+"\nRelease: "+version+"\nBytes: "+bytes+" / "+size+"\nAPI: "+API+transferDetails()));
  TextView note=Ui.t(this,getString(R.string.updater_note),12,Ui.dim,App.cairoSB);Ui.pad(note,0,24,0,0,this);box.addView(note);visible=this;render();if(phase.equals("idle"))check();
 }
 @Override protected void onResume(){super.onResume();visible=this;clock.post(tick);render();continueInstall();if(permissionPending){permissionPending=false;if(getPackageManager().canRequestPackageInstalls())install();else{detail=getString(R.string.allow_install_help);render();}}}
 @Override protected void onSaveInstanceState(Bundle b){b.putBoolean("permissionPending",permissionPending);super.onSaveInstanceState(b);}
 @Override protected void onPause(){clock.removeCallbacks(tick);if(visible==this)visible=null;super.onPause();}
 private static void refresh(){UpdateActivity a=visible;if(a!=null)a.runOnUiThread(()->{a.render();a.continueInstall();});}
 private void continueInstall(){if(!busy&&phase.equals("ready")&&installRequested&&visible==this){installRequested=false;install();}}
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
  UpdateHttp.Control c=transfer;if(busy&&phase.equals("downloading")&&c!=null){label=c.stage.equals("connecting")?R.string.update_connecting:c.stage.equals("waiting")?R.string.update_waiting_first:R.string.updater_downloading;}
  cancelTransfer.setVisibility(busy&&phase.equals("downloading")?android.view.View.VISIBLE:android.view.View.GONE);
  status.setText(getString(label)+(version.isEmpty()?"":" · "+version));
  progress.setProgress((int)Math.min(1000,1000*bytes/Math.max(1,size)));
  String downloadInfo=(phase.equals("downloading")||phase.equals("download_failed"))?((100*bytes/Math.max(1,size))+"% · "+Ui.fmtSize(bytes)+" / "+Ui.fmtSize(size)+" · "+getString(R.string.attempt_number,attempt)+"\n"):"";
  info.setText(downloadInfo+detail+((busy&&c!=null)?"\n"+getString(R.string.update_elapsed,(System.currentTimeMillis()-c.started)/1000)+"\n"+c.host:""));
  tests.setEnabled(!busy);
  primary.setEnabled(!busy);primary.setAlpha(busy?.5f:1f);
  primary.setText(getString(busy?(phase.equals("checking")?R.string.checking:phase.equals("verifying")?R.string.updater_verifying:R.string.updater_downloading):phase.equals("ready")?R.string.install_update:(phase.equals("found")?R.string.download_update:(phase.equals("download_failed")?R.string.resume_download:R.string.check_updates))));
 }
 private void check(){
  busy=true;phase="checking";detail="";version="";render();
  worker.execute(()->{try{
   String installed=AppVersion.installed(this);
   UpdateSource.Result source=UpdateSource.load(Store.get("includeTestVersions","1").equals("1"));JSONObject release=ReleaseChoice.choose(source.releases,Store.get("includeTestVersions","1").equals("1"),installed,AppVersion.code(this));if(release==null){if(source.feed)throw new IOException(getString(R.string.update_feed_no_new));phase="current";detail=getString(R.string.installed_version,AppVersion.display(this));return;}
   version=release.getString("tag_name");
   if(!ReleaseChoice.newer(release,installed,AppVersion.code(this))){phase="current";detail=getString(R.string.installed_version,AppVersion.display(this));return;}
   JSONObject asset=UpdateAsset.exact(release);expectedCode=ReleaseChoice.buildCode(release);
   assetUrl=asset.getString("browser_download_url");size=asset.getLong("size");digest=asset.getString("digest");
   String key=version+"|"+assetUrl+"|"+size+"|"+digest;
   if(!Store.get("updateAsset","").equals(key)){if(apk().exists()&&!apk().delete())throw new IOException("Cannot discard old update");Store.set("updateAsset",key);}
   bytes=apk().length();phase="found";detail=getString(R.string.release_size,Ui.fmtSize(size))+"\nGitHub · "+UpdateAsset.REPO+"";if(source.feed)detail+="\n"+getString(R.string.update_feed_used);if(source.cached)detail+="\n"+getString(R.string.update_cached);
  }catch(Exception e){phase="check_failed";detail=reason(e);}finally{busy=false;refresh();}});
 }
 private void download(){installRequested=true;busy=true;phase="downloading";transfer=new UpdateHttp.Control();detail=getString(R.string.keep_updater_open);render();clock.post(tick);
  worker.execute(()->{try{
   final long[] last={0};
   UpdateHttp.download(assetUrl,apk(),size,digest,(done,total,tryNo)->{bytes=done;attempt=tryNo;long now=System.currentTimeMillis();if(now-last[0]>180||done==total){last[0]=now;refresh();}},transfer);
   phase="verifying";detail=getString(R.string.verifying_signature);refresh();try{verifyApk();}catch(Exception invalid){apk().delete();bytes=0;throw invalid;}phase="ready";detail=getString(R.string.installer_help);
  }catch(Exception e){phase="download_failed";detail=reason(e);}finally{busy=false;refresh();}});
 }
 @SuppressWarnings("deprecation") private void verifyApk()throws Exception{
  PackageManager pm=getPackageManager();PackageInfo installed=pm.getPackageInfo(getPackageName(),Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES);
  PackageInfo downloaded=pm.getPackageArchiveInfo(apk().getAbsolutePath(),Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES);
  if(downloaded==null||!getPackageName().equals(downloaded.packageName))throw new IOException("Downloaded APK has the wrong package");
  if(downloaded.versionCode<=installed.versionCode||downloaded.versionCode!=expectedCode||!("v"+downloaded.versionName).equals(version))throw new IOException("Downloaded APK version differs from the selected release");
  if(apk().length()!=size||!UpdateHttp.sha(apk()).equalsIgnoreCase(digest.substring(7)))throw new IOException("Downloaded APK integrity differs");
  android.content.pm.Signature[] local=Build.VERSION.SDK_INT>=28?(installed.signingInfo==null?null:installed.signingInfo.getApkContentsSigners()):installed.signatures;
  android.content.pm.Signature[] remote=Build.VERSION.SDK_INT>=28?(downloaded.signingInfo==null?null:downloaded.signingInfo.getApkContentsSigners()):downloaded.signatures;
  if(local==null||remote==null||local.length==0||remote.length==0||!new java.util.HashSet<android.content.pm.Signature>(java.util.Arrays.asList(local)).equals(new java.util.HashSet<android.content.pm.Signature>(java.util.Arrays.asList(remote))))throw new IOException("Signing certificate does not match installed app");
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
 private static String transferDetails(){UpdateHttp.Control c=transfer;return c==null?"":"\nNetwork stage: "+c.stage+"\nHost: "+c.host+"\nHTTP: "+c.code+"\nElapsed seconds: "+((System.currentTimeMillis()-c.started)/1000)+"\nLast byte age seconds: "+((System.currentTimeMillis()-c.lastByte)/1000)+"\nCancelled: "+c.cancelled();}
 private String reason(Exception e){if(e instanceof UpdateHttp.HttpFailure){UpdateHttp.HttpFailure h=(UpdateHttp.HttpFailure)e;if(h.status==403||h.status==429)return h.resetAt>System.currentTimeMillis()?getString(R.string.update_rate_until,java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new java.util.Date(h.resetAt))):getString(R.string.update_rate_later);}return e.getClass().getSimpleName()+": "+(e.getMessage()==null?"No details available":e.getMessage());}
}
