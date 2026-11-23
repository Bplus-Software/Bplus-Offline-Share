package com.bplus.share;
import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
public final class FileCards {
 private static final ExecutorService thumbnails=Executors.newSingleThreadExecutor();
 public static Uri uri(JSONObject m){String s=m.optString("uri");return s.isEmpty()?null:Uri.parse(s);}
 public static boolean available(Activity a,JSONObject m){Uri u=uri(m);if(u==null)return false;try(android.content.res.AssetFileDescriptor fd=a.getContentResolver().openAssetFileDescriptor(u,"r")){return fd!=null;}catch(Exception e){return false;}}
 public static View card(Activity a,JSONObject m,Runnable menu){
  boolean me=m.optBoolean("me");int ink=me?Color.WHITE:Ui.text;
  LinearLayout card=Ui.col(a);card.setBackground(Ui.round(me?Ui.accent:Ui.card,16,a));Ui.pad(card,13,12,13,10,a);
  String mime=m.optString("mime",Save.mime(m.optString("name")));if(mime.isEmpty()||mime.equals("null"))mime=Save.mime(m.optString("name"));
  if(mime.startsWith("video/"))card.addView(Ui.t(a,"▶  "+a.getString(R.string.play_video),25,ink,App.cairoB));
  if(mime.startsWith("image/")&&uri(m)!=null){ImageView image=new ImageView(a);image.setScaleType(ImageView.ScaleType.CENTER_CROP);card.addView(image,Ui.lp(Ui.dp(a,238),Ui.dp(a,145)));Uri u=uri(m);thumbnails.execute(()->{try{BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;try(InputStream in=a.getContentResolver().openInputStream(u)){BitmapFactory.decodeStream(in,null,opt);}if(opt.outWidth<=0)return;opt.inSampleSize=1;while(opt.outWidth/opt.inSampleSize>600||opt.outHeight/opt.inSampleSize>600)opt.inSampleSize*=2;opt.inJustDecodeBounds=false;Bitmap bitmap;try(InputStream in=a.getContentResolver().openInputStream(u)){bitmap=BitmapFactory.decodeStream(in,null,opt);}a.runOnUiThread(()->{if(!a.isDestroyed())image.setImageBitmap(bitmap);});}catch(Exception ignored){}});}
  LinearLayout row=Ui.row(a);TextView type=Ui.t(a,extension(m.optString("name")),11,ink,App.cairoB);Ui.pad(type,7,10,7,10,a);row.addView(type);TextView name=Ui.t(a,m.optString("name"),13.5f,ink,App.cairoB);name.setMaxWidth(Ui.dp(a,215));Ui.pad(name,9,6,4,6,a);row.addView(name);card.addView(row);
  card.addView(Ui.t(a,Ui.fmtSize(m.optLong("size"))+" · "+a.getString(uri(m)==null?R.string.file_unavailable:R.string.tap_to_open),11,me?0xffd6e4ff:Ui.dim,App.cairoSB));
  card.setOnClickListener(v->open(a,m));card.setOnLongClickListener(v->{menu.run();return true;});return card;
 }
 private static String extension(String name){int dot=name.lastIndexOf('.');String ext=dot<0?"FILE":name.substring(dot+1).toUpperCase(Locale.ROOT);return ext.length()>5?"FILE":ext;}
 public static void open(Activity a,JSONObject m){
  if(!available(a,m)){new AlertDialog.Builder(a).setMessage(a.getString(R.string.file_unavailable_help)).setPositiveButton(android.R.string.ok,null).show();return;}
  try{Uri u=uri(m);Intent i=new Intent(Intent.ACTION_VIEW);String mime=a.getContentResolver().getType(u);if(mime==null||mime.equals("*/*"))mime=m.optString("mime",Save.mime(m.optString("name")));if(mime.startsWith("video/")){Intent player=new Intent(a,VideoActivity.class);player.putExtra("uri",u.toString());player.putExtra("name",m.optString("name"));a.startActivity(player);return;}i.setDataAndType(u,mime);i.setClipData(ClipData.newRawUri("attachment",u));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);a.startActivity(i);}catch(ActivityNotFoundException e){Toast.makeText(a,a.getString(R.string.no_file_app),Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(a,a.getString(R.string.file_open_failed)+" "+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
 }
 public static void share(Activity a,JSONObject m){
  if(!available(a,m)){open(a,m);return;}
  try{Uri u=uri(m);String mime=a.getContentResolver().getType(u);if(mime==null||mime.equals("*/*"))mime=m.optString("mime",Save.mime(m.optString("name")));if(mime.isEmpty()||mime.equals("null"))mime=Save.mime(m.optString("name"));Intent send=new Intent(Intent.ACTION_SEND);send.setType(mime);send.putExtra(Intent.EXTRA_STREAM,u);send.setClipData(ClipData.newRawUri("attachment",u));send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);Intent chooser=Intent.createChooser(send,a.getString(R.string.share_file));chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);a.startActivity(chooser);}catch(Exception e){Toast.makeText(a,a.getString(R.string.file_open_failed)+" "+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
 }
 public static void forward(Activity a,JSONObject m){
  if(!available(a,m)){open(a,m);return;}
  ArrayList<String> labels=new ArrayList<>();ArrayList<ArrayList<JSONObject>> targets=new ArrayList<>();JSONArray peers=Store.peers();
  for(int i=0;i<peers.length();i++){JSONObject p=peers.optJSONObject(i);if(p==null)continue;labels.add(p.optString("name"));ArrayList<JSONObject> list=new ArrayList<>();list.add(p);targets.add(list);}
  JSONArray groups=Store.groups();for(int i=0;i<groups.length();i++){JSONObject g=groups.optJSONObject(i);if(g==null)continue;ArrayList<JSONObject> list=new ArrayList<>();JSONArray members=g.optJSONArray("members");if(members!=null)for(int j=0;j<members.length();j++){JSONObject p=Store.peerById(members.optString(j));if(p!=null)list.add(p);}if(list.isEmpty())continue;labels.add("▦ "+g.optString("name")+" ("+list.size()+")");targets.add(list);}
  if(labels.isEmpty()){Toast.makeText(a,a.getString(R.string.no_chats),Toast.LENGTH_LONG).show();return;}
  final int[] selected={0};new AlertDialog.Builder(a).setTitle(a.getString(R.string.forward_file)).setSingleChoiceItems(labels.toArray(new String[0]),0,(d,n)->selected[0]=n).setNegativeButton(a.getString(R.string.cancel),null).setPositiveButton(a.getString(R.string.next), (d,n)->{
   final String label=labels.get(selected[0]);final ArrayList<JSONObject> recipients=targets.get(selected[0]);
   new AlertDialog.Builder(a).setTitle(a.getString(R.string.forward_file)).setMessage(a.getString(R.string.forward_confirm,m.optString("name"),label)).setNegativeButton(a.getString(R.string.cancel),null).setPositiveButton(a.getString(R.string.send_now),(d2,n2)->{
    Toast.makeText(a,a.getString(R.string.sending),Toast.LENGTH_SHORT).show();Executors.newSingleThreadExecutor().execute(()->{int ok=0;for(JSONObject recipient:recipients)if(TransferService.sendToPeer(recipient,uri(m),m.optString("name"),m.optLong("size")))ok++;final int sent=ok;a.runOnUiThread(()->Toast.makeText(a,a.getString(R.string.forward_result,sent,recipients.size()),Toast.LENGTH_LONG).show());});
   }).show();
  }).show();
 }
}
