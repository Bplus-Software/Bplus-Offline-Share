package com.bplus.share;
/** Shared-import bounds. Caller must still verify actual stream bytes, not trust metadata. */
public final class ImportRules{
 public static boolean count(int n){return n>=0&&n<=20;}
 public static boolean bytes(long file,long total){return file>=0&&total>=0&&file<=100L*1024*1024&&total<=200L*1024*1024;}
 public static boolean text(String s){return s!=null&&s.length()<=8000;}
 public static boolean source(String scheme,String authority,String app){return "content".equals(scheme)&&authority!=null&&authority.indexOf('@')<0&&!authority.equals(app)&&!authority.startsWith(app+".");}
}
