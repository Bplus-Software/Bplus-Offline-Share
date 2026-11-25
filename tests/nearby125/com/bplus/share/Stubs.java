package com.bplus.share;import org.json.*;import java.io.*;
class Store{static final int PORT=47200;static JSONObject saved;static String get(String k,String d){return d;}static String token(){return "receiverToken12345678";}static JSONObject peerById(String id){return saved!=null&&saved.optString("id").equals(id)?saved:null;}static JSONArray peers(){return new JSONArray();}static void upsertPeer(JSONObject p)throws Exception{if(p.optString("pairEpoch").equals("old"))throw new IOException();saved=p;}}
class Profile{static JSONObject clean(JSONObject p){return new JSONObject(p.toString());}static JSONObject local(){return new JSONObject().put("id","receiver").put("name","Receiver");}}
class Unpair{static JSONObject load(String id){return new JSONObject().put("exists",true);}}
class Net{static String readAll(InputStream i){return "";}}
