package com.bplus.share;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import org.json.*;
public class ChatsActivity extends Activity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);Ui.applyLocale(this);Ui.applyTheme(this);}
 @Override protected void onResume(){super.onResume();LinearLayout root=Ui.col(this);root.setBackgroundColor(Ui.bg);root.addView(Ui.topBar(this,getString(R.string.chats_title),true,null));ScrollView sc=new ScrollView(this);LinearLayout box=Ui.col(this);Ui.pad(box,20,12,20,24,this);sc.addView(box);root.addView(sc,Ui.lpW1());JSONArray peers=Store.peers();if(peers.length()==0){box.addView(Ui.t(this,getString(R.string.no_chats),14,Ui.dim,App.cairoSB));TextView pair=Ui.cta(this,getString(R.string.pair_title));box.addView(pair);pair.setOnClickListener(v->startActivity(new Intent(this,DevicesActivity.class)));}for(int i=0;i<peers.length();i++){JSONObject peer=peers.optJSONObject(i);if(peer==null)continue;JSONArray thread=Store.thread(peer.optString("id"));JSONObject last=thread.optJSONObject(thread.length()-1);String preview=last==null?getString(R.string.start_chat):last.optString("kind").equals("file")?"📎 "+last.optString("name"):last.optString("body");LinearLayout row=Ui.setRow(this,peer.optString("name"),preview,null);box.addView(row);row.setOnClickListener(v->{Intent intent=new Intent(this,ChatActivity.class);intent.putExtra("peer",peer.optString("id"));startActivity(intent);});}setContentView(root);}
}
