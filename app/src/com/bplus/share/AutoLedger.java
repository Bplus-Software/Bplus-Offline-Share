package com.bplus.share;
import android.database.sqlite.*;import android.content.*;import android.database.Cursor;import org.json.*;
/** Durable local send ledger. A claimed URI is never automatically retried, even after a lost ACK. */
public final class AutoLedger extends SQLiteOpenHelper{
 private AutoLedger(){super(App.ctx(),"autosend-ledger.db",null,1);}
 public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE files(folder TEXT NOT NULL,uri TEXT NOT NULL,name TEXT,size INTEGER,state TEXT NOT NULL,updated INTEGER,PRIMARY KEY(folder,uri))");}public void onUpgrade(SQLiteDatabase db,int a,int b){}
 public static synchronized void baseline(String folder,JSONArray files)throws Exception{try(AutoLedger helper=new AutoLedger()){SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();try{for(int i=0;i<files.length();i++){JSONObject f=files.getJSONObject(i);ContentValues v=values(folder,f,"baseline");db.insertWithOnConflict("files",null,v,SQLiteDatabase.CONFLICT_IGNORE);}db.setTransactionSuccessful();}finally{db.endTransaction();}}}
 private static ContentValues values(String folder,JSONObject f,String state){ContentValues v=new ContentValues();v.put("folder",folder);v.put("uri",f.optString("uri"));v.put("name",f.optString("name"));v.put("size",f.optLong("size"));v.put("state",state);v.put("updated",System.currentTimeMillis());return v;}
 public static synchronized boolean claim(String folder,JSONObject file){try(AutoLedger h=new AutoLedger()){return h.getWritableDatabase().insertWithOnConflict("files",null,values(folder,file,"claimed"),SQLiteDatabase.CONFLICT_IGNORE)!=-1;}}
 public static synchronized void result(String folder,String uri,boolean success){try(AutoLedger h=new AutoLedger()){ContentValues v=new ContentValues();v.put("state",success?"sent":"failed-or-uncertain");v.put("updated",System.currentTimeMillis());h.getWritableDatabase().update("files",v,"folder=? AND uri=?",new String[]{folder,uri});}}
}
