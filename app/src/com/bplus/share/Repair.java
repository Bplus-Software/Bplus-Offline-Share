package com.bplus.share;
import android.app.*;import android.content.*;import org.json.*;
/** Same-device shortcut only. Never restores revoked pairing credentials. */
public final class Repair{
 public static final String TARGET="repairTarget",NAME="repairName";
 public static String target(Activity a){String id=a.getIntent().getStringExtra(TARGET);return id==null?"":id;}
 public static void open(Activity a,String id){JSONObject p=Store.listedPeer(id);if(p==null)return;if(Store.peerById(id)!=null)return;String name=p.optString("name");new AppDialog(a).setTitle(a.getString(R.string.repair_title,name)).setView(Ui.sub(a,a.getString(R.string.repair_note))).setItems(new String[]{a.getString(R.string.repair_nearby),a.getString(R.string.repair_qr)},(d,n)->a.startActivity(new Intent(a,n==0?NearbyActivity.class:ScanActivity.class).putExtra(TARGET,id).putExtra(NAME,name))).setNegativeButton(R.string.cancel,null).show();}
 public static boolean matches(String target,String actual){return RepairIdentity.matches(target,actual);}
 public static boolean fresh(String target,String epoch){return RepairIdentity.fresh(target,epoch);}
}
