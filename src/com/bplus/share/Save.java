package com.bplus.share;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import java.io.*;

public class Save {
 public static String mime(String name){int dot=name.lastIndexOf('.');String m=dot<0?null:MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(dot+1).toLowerCase(java.util.Locale.ROOT));return m==null?"application/octet-stream":m;}
 public static String folder(String name){String m=mime(name);return m.startsWith("video/")?"Videos":m.startsWith("image/")?"Photos":"Files";}
 public static class Target {
  public final Uri uri;public final OutputStream out;private final Context c;private final File file;public String name;
  Target(Context c,Uri u,OutputStream o,File f){this.c=c;uri=u;out=o;file=f;}
  public void complete()throws IOException{out.close();if(Build.VERSION.SDK_INT>=29){ContentValues v=new ContentValues();v.put(MediaStore.Downloads.IS_PENDING,0);c.getContentResolver().update(uri,v,null,null);}
   try{Store.set("ownedReceived_"+uri.toString(),"1");JSONObjectRecord.record(uri,name);}catch(Exception ignored){}
  }
  public void abort(){try{out.close();}catch(Exception ignored){}try{if(file!=null)file.delete();else c.getContentResolver().delete(uri,null,null);}catch(Exception ignored){}}
 }
 public static Target create(Context c,String input)throws IOException{
  String name=input.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);if(name.isEmpty()||name.equals(".")||name.equals(".."))name="file";
  try{
   if(Build.VERSION.SDK_INT>=29){ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,name);v.put(MediaStore.Downloads.MIME_TYPE,mime(name));v.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/B+ Share/"+folder(name));v.put(MediaStore.Downloads.IS_PENDING,1);Uri u=c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(u==null)throw new IOException("Cannot create download");OutputStream out=c.getContentResolver().openOutputStream(u);if(out==null){c.getContentResolver().delete(u,null,null);throw new IOException("Cannot open download");}Target t=new Target(c,u,out,null);t.name=name;return t;}
   File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"B+ Share/"+folder(name));if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create Downloads folder");File f=new File(dir,name);int i=1;while(f.exists())f=new File(dir,(i++)+"-"+name);Uri u=Uri.parse("content://"+c.getPackageName()+".files/received/"+folder(name)+"/"+Uri.encode(f.getName()));Target t=new Target(c,u,new FileOutputStream(f),f);t.name=name;return t;
  }catch(IOException e){throw e;}catch(Exception e){throw new IOException("Cannot save file: "+e.getClass().getSimpleName(),e);}
 }
 private static class JSONObjectRecord {static synchronized void record(Uri uri,String name)throws Exception{org.json.JSONArray a=new org.json.JSONArray(Store.get("library","[]"));org.json.JSONObject m=new org.json.JSONObject();m.put("kind","file");m.put("uri",uri.toString());m.put("name",name);m.put("mime",mime(name));m.put("me",false);long size=0;try(android.content.res.AssetFileDescriptor f=App.ctx().getContentResolver().openAssetFileDescriptor(uri,"r")){if(f!=null)size=Math.max(0,f.getLength());}m.put("size",size);a.put(m);Store.set("library",a.toString());}}
}
