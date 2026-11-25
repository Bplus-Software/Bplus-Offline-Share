package com.bplus.share;
import android.app.Activity;import org.json.JSONObject;
/** Diagnostic copy is offered only inside a real failed-transfer error dialog. */
public final class TransferError{
 public static boolean eligible(JSONObject m){return m!=null&&"failed".equals(m.optString("state"));}
 public static void show(Activity a,JSONObject m){if(!eligible(m))return;String error=m.optString("error",a.getString(R.string.old_failure));String details=error+(m.optJSONObject("timing")==null?"":"\n"+m.optJSONObject("timing").toString());new AppDialog(a).setTitle(R.string.file_failed_actions).setMessage(error).setNeutralButton(R.string.copy_details,(d,n)->PairUi.copy(a,details)).setPositiveButton(android.R.string.ok,null).show();}
}
