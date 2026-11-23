package com.bplus.share;
import android.app.*;import android.content.*;import org.json.*;
/** Foreground app-open check only. Never downloads or installs automatically. */
public final class AutoUpdates{
 private static boolean busy;private static long checked;
 public static synchronized void check(Activity a){if(busy||!Store.get("autoUpdateCheck","0").equals("1")||System.currentTimeMillis()-checked<21600000)return;busy=true;checked=System.currentTimeMillis();new Thread(()->{try{String current=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;JSONObject release=ReleaseChoice.choose(new JSONArray(UpdateHttp.text("https://api.github.com/repos/basem-sadoun/Bplus-Share/releases?per_page=100")),Store.get("includeTestVersions","1").equals("1"),current);if(release!=null){String version=release.optString("tag_name");a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed()&&a.hasWindowFocus())new AppDialog(a).setTitle(R.string.updater_found).setMessage(version).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.check_updates,(d,n)->a.startActivity(new Intent(a,UpdateActivity.class))).show();});}}catch(Exception ignored){}finally{synchronized(AutoUpdates.class){busy=false;}}}).start();}
}
