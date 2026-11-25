package com.bplus.share;
import org.json.*;
/** Contact fields only enter the separately authenticated paired-profile endpoint. */
public final class ContactFields{
 public static JSONObject shared()throws Exception{return new JSONObject().put("email",Store.get("shareProfileEmail","0").equals("1")?Store.get("profileEmail",""):"").put("phone",Store.get("shareProfilePhone","0").equals("1")?Store.get("profilePhone",""):"");}
}
