package com.bplus.share;
import android.app.*;import android.content.*;import android.media.*;import android.os.*;
public final class Alerts{
 static final String CHANNEL="bplus_messages_v1";
 public static void setup(Context c){if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"Messages and received files",NotificationManager.IMPORTANCE_DEFAULT);ch.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());ch.enableVibration(true);c.getSystemService(NotificationManager.class).createNotificationChannel(ch);}}
 public static void incoming(String title,String body){try{Context c=App.ctx();setup(c);Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,1,open,PendingIntent.FLAG_IMMUTABLE);Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(body).setContentIntent(pi).setAutoCancel(true).build();c.getSystemService(NotificationManager.class).notify((int)(System.currentTimeMillis()%1000000)+2,n);}catch(Exception ignored){}}
}
