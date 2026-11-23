package com.bplus.share;
import android.app.*;
import android.content.*;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;

public final class PairUi {
 public static void copy(Activity a,String details){((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("B+ Share",details));Toast.makeText(a,a.getString(R.string.details_copied),Toast.LENGTH_SHORT).show();}
 public static void wifi(Activity a){try{a.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));}catch(Exception e){Toast.makeText(a,e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}}
 public static boolean ensure(Activity a,Runnable retry){if(Net.localIp(a)!=null)return true;LinearLayout root=Ui.col(a);root.setBackgroundColor(Ui.bg);android.app.Dialog dialog=new android.app.Dialog(a);setup(a,root,()->{dialog.dismiss();retry.run();});dialog.setContentView(root);dialog.show();dialog.getWindow().setLayout(-1,-1);return false;}
 public static void setup(Activity a,LinearLayout root,Runnable retry){
  root.removeAllViews();root.addView(Ui.topBar(a,a.getString(R.string.pair_title),true,null));
  LinearLayout box=Ui.col(a);Ui.pad(box,24,24,24,24,a);ScrollView scroll=new ScrollView(a);scroll.addView(box);root.addView(scroll,Ui.lpW1());
  TextView icon=Ui.t(a,"⌁",60,Ui.accent,App.cairoB);icon.setGravity(Gravity.CENTER);box.addView(icon);
  box.addView(Ui.t(a,a.getString(R.string.connect_phones),23,Ui.text,App.cairoB));
  TextView intro=Ui.t(a,a.getString(R.string.lan_steps),14,Ui.dim,App.cairoSB);Ui.pad(intro,0,24,0,24,a);box.addView(intro);
  button(a,box,a.getString(R.string.open_wifi),()->wifi(a),true);
  button(a,box,a.getString(R.string.check_connection),()->{if(Net.localIp(a)==null)Toast.makeText(a,a.getString(R.string.no_lan),Toast.LENGTH_LONG).show();else retry.run();},false);
  TextView note=Ui.t(a,a.getString(R.string.lan_note),12,Ui.dim,App.cairoSB);Ui.pad(note,0,20,0,0,a);box.addView(note);
 }
 public static void failure(Activity a,LinearLayout root,String name,String ip,int port,String reason,Runnable retry){
  root.removeAllViews();root.addView(Ui.topBar(a,a.getString(R.string.pair_title),true,null));ScrollView sc=new ScrollView(a);LinearLayout box=Ui.col(a);Ui.pad(box,24,26,24,20,a);sc.addView(box);root.addView(sc,Ui.lpW1());
  box.addView(Ui.t(a,a.getString(R.string.peer_not_responding),22,Ui.text,App.cairoB));
  String details="B+ Share 1.1.0\n"+a.getString(R.string.target_device)+": "+name+"\n"+a.getString(R.string.target_address)+": "+ip+":"+port+"\n"+a.getString(R.string.local_address)+": "+Net.localIp(a)+"\n"+a.getString(R.string.failure_reason)+": "+reason;
  TextView d=Ui.t(a,details,13,Ui.dim,App.cairoSB);d.setTextIsSelectable(true);Ui.pad(d,16,18,16,18,a);d.setBackground(Ui.roundBorder(Ui.card,Ui.border,18,a));LinearLayout.LayoutParams lp=Ui.lp(-1,-2);lp.topMargin=Ui.dp(a,24);box.addView(d,lp);
  TextView help=Ui.t(a,a.getString(R.string.pair_help),13,Ui.dim,App.cairoSB);Ui.pad(help,0,20,0,20,a);box.addView(help);
  button(a,box,a.getString(R.string.try_again),retry,true);button(a,box,a.getString(R.string.open_wifi),()->wifi(a),false);button(a,box,a.getString(R.string.copy_details),()->copy(a,details),false);
 }
 private static void button(Activity a,LinearLayout box,String text,Runnable click,boolean primary){TextView b=primary?Ui.cta(a,text):Ui.ghost(a,text);b.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=Ui.lp(-1,-2);lp.topMargin=Ui.dp(a,12);box.addView(b,lp);b.setOnClickListener(v->click.run());}
}
