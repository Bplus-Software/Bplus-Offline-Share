package com.bplus.share;
import java.util.*;
/** Pure rules used by the Android share intent builder. */
public final class ShareRules{
 public static String mime(List<String> types){if(types.isEmpty())return "*/*";String first=valid(types.get(0)),top=first.split("/")[0];boolean same=true,common=true;for(String raw:types){String t=valid(raw);if(!t.equals(first))same=false;if(!t.split("/")[0].equals(top))common=false;}return same?first:common&&!top.equals("*")?top+"/*":"*/*";}
 static String valid(String s){return s!=null&&s.matches("[A-Za-z0-9!#$&^_.+*-]+/[A-Za-z0-9!#$&^_.+*-]+")?s:"application/octet-stream";}
}
