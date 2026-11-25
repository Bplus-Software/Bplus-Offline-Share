package com.bplus.share;import org.json.*;import java.io.*;import android.net.Uri;
class Nearby{static int calls;static boolean fail;static JSONObject call(String ip,String route,JSONObject b)throws Exception{calls++;if(fail)throw new IOException("HTTP 403");return new JSONObject().put("status","deleted").put("actor","member");}}
class TransferService{static void changed(String s){}}
class Profile{static JSONObject local(){return new JSONObject().put("name","host-name");}}
class App{static android.content.Context ctx(){return new android.content.Context();}}
class Alerts{static void message(String s,JSONObject m){}}
class FileConsent{static boolean ask(String a,String b,long c){return true;}}
class R{static class string{static int group_deleted_host=1;}}
class Save{static String mime(String n){return "file";}static class Target{java.io.OutputStream out=new java.io.ByteArrayOutputStream();Uri uri=Uri.parse("content://saved");void complete(){}void abort(){}}static Target create(Object c,String n){return new Target();}}
