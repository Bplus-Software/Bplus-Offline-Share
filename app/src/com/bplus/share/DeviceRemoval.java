package com.bplus.share;
import android.app.Activity;
public final class DeviceRemoval{
 public static void menu(Activity a,String id,Runnable done){boolean paired=Store.peerById(id)!=null;String[] labels=paired?new String[]{a.getString(R.string.unpair_device),a.getString(R.string.remove_device_chat)}:new String[]{a.getString(R.string.remove_device_chat)};new AppDialog(a).setTitle(R.string.device_actions).setItems(labels,(d,n)->confirm(a,id,!paired||n==1,done)).show();}
 public static void show(Activity a,String id,Runnable done){menu(a,id,done);}
 public static void confirm(Activity a,String id,boolean erase,Runnable done){new AppDialog(a).setTitle(erase?R.string.remove_device_chat:R.string.unpair_device).setMessage(erase?R.string.remove_device_chat_note:R.string.unpair_device_note).setNegativeButton(R.string.cancel,null).setPositiveButton(erase?R.string.remove_device_chat:R.string.unpair_device,(d,n)->{try{if(erase)Store.forgetPeer(id);else {Store.removePeer(id);android.widget.Toast.makeText(a,R.string.unpair_remote_notice,android.widget.Toast.LENGTH_LONG).show();}done.run();}catch(Exception e){new AppDialog(a).setMessage(R.string.device_action_failed).setPositiveButton(android.R.string.ok,null).show();}}).show();}
}
