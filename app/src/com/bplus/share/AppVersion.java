package com.bplus.share;
import android.content.Context;import android.content.pm.PackageInfo;
/** Installed package is the sole version source. Unknown is never a made-up release number. */
public final class AppVersion{
 public static String installed(Context c)throws java.io.IOException{try{PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),0);if(p.versionName==null||p.versionName.trim().isEmpty())throw new Exception();return p.versionName;}catch(Exception e){throw new java.io.IOException("Cannot read installed app version",e);}}
 public static String display(Context c){try{return installed(c);}catch(Exception e){return c.getString(R.string.version_unknown);}}
}
