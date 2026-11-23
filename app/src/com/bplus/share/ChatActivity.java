package com.bplus.share;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ChatActivity extends Activity {
    private static final int REQ_PICK = 61;
    private FileSelection selection;
    private JSONObject locating;
    private final java.util.Set<String> retrying=java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
    private String peerId;
    private JSONObject peer;
    private LinearLayout msgs;
    private ScrollView scroll;
    private final java.util.concurrent.ExecutorService exec = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.applyLocale(this);
        Ui.applyTheme(this);
        peerId = getIntent().getStringExtra("peer");
        peer = Store.peerById(peerId);
        if(peer==null){finish();return;}selection=new FileSelection(this,peerId,this::renderThread);

        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.bg);
        String name = peer != null ? peer.optString("name") : "";
        LinearLayout head = Ui.row(this);
        Ui.pad(head, 14, 10, 14, 10, this);
        TextView back = Ui.t(this, "‹", 26, Ui.dim, App.cairoB);
        back.setGravity(Gravity.CENTER);
        head.addView(back, Ui.lp(Ui.dp(this, 38), Ui.dp(this, 38)));
        head.addView(Profile.avatar(this,peer,40));
        back.setOnClickListener(v -> finish());
        LinearLayout meta = Ui.col(this);
        Ui.pad(meta, 12, 0, 12, 0, this);
        meta.addView(Ui.t(this, name, 16, Ui.text, App.cairoB));
        if(!peer.optString("username").isEmpty())meta.addView(Ui.t(this,"@"+peer.optString("username"),11,Ui.dim,App.cairoSB));
        Store.set("read_"+peerId,String.valueOf(System.currentTimeMillis()));
        TextView status = Ui.t(this, "", 11, Ui.green, App.cairoB);
        meta.addView(status);
        head.addView(meta, Ui.lpw(1));
        TextView diagnose=Ui.ghost(this,"ⓘ");Ui.pad(diagnose,12,8,12,8,this);head.addView(diagnose);diagnose.setContentDescription(getString(R.string.connection_details));diagnose.setOnClickListener(v->diagnostics());
        status.setOnClickListener(v->diagnostics());
        head.setBackgroundColor(Ui.bar);
        root.addView(head);
        checkOnline(status);

        msgs = Ui.col(this);
        Ui.pad(msgs, 16, 8, 16, 8, this);
        scroll = new ScrollView(this);
        scroll.addView(msgs);
        root.addView(scroll, Ui.lpW1());

        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Ui.bar);
        Ui.pad(bar, 14, 12, 14, 20, this);
        TextView attach = Ui.t(this, "＋", 19, Ui.dim, App.cairoB);
        attach.setGravity(Gravity.CENTER);
        attach.setBackground(Ui.round(Ui.field, 20, this));
        bar.addView(attach, Ui.lp(Ui.dp(this, 40), Ui.dp(this, 40)));
        attach.setOnClickListener(v -> pickFile());
        EditText input = new EditText(this);
        input.setHint(getString(R.string.message_hint));
        input.setHintTextColor(Color.rgb(94,104,120));
        input.setTextColor(Ui.text);
        input.setTypeface(App.cairoSB);
        input.setTextSize(13.5f);
        input.setBackground(Ui.round(Ui.field, 22, this));
        Ui.pad(input, 16, 10, 16, 10, this);
        LinearLayout.LayoutParams ilp = Ui.lpw(1);
        ilp.setMarginStart(Ui.dp(this, 10));
        ilp.setMarginEnd(Ui.dp(this, 10));
        bar.addView(input, ilp);
        TextView send = Ui.t(this, "➤", 16, Color.WHITE, App.cairoB);
        send.setGravity(Gravity.CENTER);
        send.setBackground(Ui.round(Ui.accent, 20, this));
        bar.addView(send, Ui.lp(Ui.dp(this, 40), Ui.dp(this, 40)));
        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            input.setText("");
            sendText(text);
        });
        TextView emoji=Ui.t(this,"☺",22,Ui.accent,App.cairoB);emoji.setGravity(Gravity.CENTER);bar.addView(emoji,0,Ui.lp(Ui.dp(this,36),Ui.dp(this,40)));
        emoji.setContentDescription(getString(R.string.emoji_title));
        emoji.setOnClickListener(v->{String[] choices={"👍","😊","❤️","😂","🙏"};new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.emoji_title)).setItems(choices,(d,n)->{int start=Math.max(0,input.getSelectionStart());input.getText().insert(start,choices[n]);input.requestFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(input,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}).setNegativeButton(getString(R.string.cancel),null).show();});
        root.addView(bar);
        setContentView(root);

        renderThread();
        TransferService.listen("chat", (type, data) -> {
            if (!peerId.equals(data.optString("from"))) return;
            runOnUiThread(this::renderThread);
        });
        if (getIntent().getBooleanExtra("attach", false)) pickFile();
    }

    @Override public void onBackPressed(){if(selection!=null&&selection.back())return;super.onBackPressed();}
    @Override protected void onDestroy() {
        TransferService.unlisten("chat");
        super.onDestroy();
    }

    private void checkOnline(TextView status) {
        if (peer == null) return;
        exec.execute(() -> {
            boolean on = Net.ping(peer.optString("ip"), peer.optInt("port", Store.PORT), peer.optString("token")) != null;
            runOnUiThread(() -> {
                status.setText("● " + getString(on ? R.string.online_wifi : R.string.not_nearby));
                status.setTextColor(on ? Ui.green : Ui.dim);
            });
        });
    }

    private void renderThread() {
        msgs.removeAllViews();
        JSONArray t = Store.thread(peerId);
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.US);
        for (int i = 0; i < t.length(); i++) {
            JSONObject m = t.optJSONObject(i);
            if (m == null) continue;
            boolean me = m.optBoolean("me");
            String kind = m.optString("kind", "text");
            View bub;
            if(kind.equals("text")){
                TextView text=Ui.t(this,m.optString("body"),14,me?Color.WHITE:Ui.text,App.cairoSB);
                text.setLineSpacing(0,1.25f);text.setMaxWidth(Ui.dp(this,280));text.setBackground(Ui.round(me?Ui.accent:Ui.card,16,this));Ui.pad(text,13,9,13,9,this);if(me&&m.optString("state").equals("failed"))text.setOnClickListener(v->retryMessage(m));bub=text;
            }else{
                bub=FileCards.card(this,m,()->selection.toggle(m));
                if(selection.active())bub.setOnClickListener(v->selection.toggle(m));

                if(!selection.active()&&!Attachments.state(this,m).equals("ok"))bub.setOnClickListener(v->locate(m));
            }
            String time = tf.format(new Date(m.optLong("ts")));
            String state=m.optString("state");
            if(me)time+=" · "+getString(state.equals("failed")?(kind.equals("text")?R.string.failed_retry:R.string.file_failed_actions):state.equals("sending")?R.string.sending:R.string.sent);
            TextView stamp = Ui.t(this,time,10,Ui.dim,App.cairoSB);
            LinearLayout cell = Ui.col(this);cell.setGravity(me?Gravity.END:Gravity.START);
            LinearLayout.LayoutParams bp=Ui.lp(-2,-2);bp.gravity=me?Gravity.END:Gravity.START;cell.addView(kind.equals("file")?selection.decorate(bub,m):bub,bp);if(kind.equals("file")&&!Attachments.state(this,m).equals("ok")){TextView fix=Ui.t(this,Attachments.explanation(this,Attachments.state(this,m))+"\n"+getString(R.string.locate_file),12,Ui.accent,App.cairoSB);fix.setMaxWidth(Ui.dp(this,280));fix.setOnClickListener(v->locate(m));cell.addView(fix);}cell.addView(stamp);
            if(me && state.equals("failed") && kind.equals("text")){
                String reason=m.optString("error",getString(R.string.old_failure));TextView failure=Ui.t(this,reason+"\n"+getString(R.string.retry_details),11,Ui.dim,App.cairoSB);failure.setMaxWidth(Ui.dp(this,280));failure.setOnClickListener(v->retryMessage(m));cell.addView(failure);
            }
            Ui.pad(cell,0,0,0,12,this);
            msgs.addView(cell);
        }
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void sendText(String text){
        JSONObject m=new JSONObject();try{m.put("id",java.util.UUID.randomUUID().toString());m.put("kind","text");m.put("body",text);m.put("me",true);m.put("ts",System.currentTimeMillis());m.put("state","sending");Store.addMsg(peerId,m);}catch(Exception ignored){}renderThread();deliver(m);
    }
    private void deliver(JSONObject m){
        String id=m.optString("id");if(!retrying.add(id))return;
        exec.execute(()->{JSONObject current=Store.peerById(peerId);boolean ok=current!=null&&Net.sendMsg(current.optString("ip"),current.optInt("port",Store.PORT),current.optString("token"),m.optString("body"));String why=ok?"":current==null?getString(R.string.peer_missing):Net.lastError();
            synchronized(Store.class){JSONArray thread=Store.thread(peerId);for(int i=0;i<thread.length();i++){JSONObject item=thread.optJSONObject(i);if(item!=null&&id.equals(item.optString("id"))){try{item.put("state",ok?"sent":"failed");item.put("error",why);}catch(Exception ignored){}break;}}Store.saveThread(peerId,thread);}retrying.remove(id);runOnUiThread(this::renderThread);
        });
    }
    private void retryMessage(JSONObject m){
        if(retrying.contains(m.optString("id")))return;
        final JSONObject[] selected={m};
        new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.not_delivered_title)).setMessage(m.optString("error",getString(R.string.old_failure))+"\n\n"+getString(R.string.retry_duplicate_note)).setNegativeButton(getString(R.string.cancel),null).setNeutralButton(getString(R.string.connection_details),(d,n)->diagnostics()).setPositiveButton(getString(R.string.try_again),(d,n)->{
            synchronized(Store.class){JSONArray thread=Store.thread(peerId);for(int i=0;i<thread.length();i++){JSONObject item=thread.optJSONObject(i);if(same(item,m)&&item.optString("body").equals(m.optString("body"))){try{if(item.optString("id").isEmpty())item.put("id",java.util.UUID.randomUUID().toString());item.put("state","sending");selected[0]=item;}catch(Exception ignored){}break;}}Store.saveThread(peerId,thread);}renderThread();deliver(selected[0]);
        }).show();
    }
    private void diagnostics(){
        JSONObject current=Store.peerById(peerId);String local=Net.localIp(this);String ip=current==null?"?":current.optString("ip");int port=current==null?Store.PORT:current.optInt("port",Store.PORT);
        TextView details=Ui.t(this,getString(R.string.checking),13,Ui.text,App.cairoSB);Ui.pad(details,20,20,20,20,this);details.setTextIsSelectable(true);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.connection_details)).setView(details).setPositiveButton(android.R.string.ok,null).setNeutralButton(getString(R.string.open_wifi),(d,n)->PairUi.wifi(this)).setNegativeButton(getString(R.string.copy_details),(d,n)->PairUi.copy(this,details.getText().toString())).create();dialog.show();
        exec.execute(()->{boolean ok=current!=null&&Net.ping(ip,port,current.optString("token"))!=null;String reason=current==null?getString(R.string.peer_missing):ok?getString(R.string.device_responded):Net.lastError();String text="B+ Share 1.1.3\n"+getString(R.string.target_address)+": "+ip+":"+port+"\n"+getString(R.string.local_address)+": "+(local==null?getString(R.string.no_lan):local)+"\n"+getString(R.string.failure_reason)+": "+reason+"\n\n"+getString(R.string.diagnostic_note);runOnUiThread(()->{if(dialog.isShowing())details.setText(text);});});
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_PICK);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if(req==62 && res==RESULT_OK && data!=null && data.getData()!=null && locating!=null){
            Uri u=data.getData();if(!Attachments.readable(this,u)){android.widget.Toast.makeText(this,R.string.attach_permission,1).show();return;}try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            JSONObject selected=locating;locating=null;
            new android.app.AlertDialog.Builder(this).setMessage(getString(R.string.locate_confirm,selected.optString("name"))+"\n"+u.getLastPathSegment()+"\n"+getString(R.string.locate_safe_note)).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(android.R.string.ok,(d,n)->{
                Attachments.snapshot();JSONArray thread=Store.thread(peerId);for(int j=0;j<thread.length();j++){JSONObject item=thread.optJSONObject(j);if(same(item,selected)){try{item.put("uri",u.toString());item.put("mime",getContentResolver().getType(u));}catch(Exception ignored){}break;}}Store.saveThread(peerId,thread);renderThread();
            }).show();return;
        }
        if (req != REQ_PICK || res != RESULT_OK || data == null || peer == null) return;
        java.util.ArrayList<Uri> uris = new java.util.ArrayList<>();
        if (data.getClipData() != null)
            for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        exec.execute(() -> {
            for (Uri u : uris) {
                try { getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
                String name = "file"; long size = 0;
                try (Cursor c = getContentResolver().query(u, null, null, null, null)) {
                    if (c != null && c.moveToFirst()) {
                        int ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        int si = c.getColumnIndex(OpenableColumns.SIZE);
                        if (ni >= 0) name = c.getString(ni);
                        if (si >= 0) size = c.getLong(si);
                    }
                }
                boolean ok = TransferService.sendToPeer(peer, u, name, size);
                runOnUiThread(() -> {
                    renderThread();
                    if (!ok) android.widget.Toast.makeText(this, getString(R.string.no_device_nearby), android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private static boolean same(JSONObject a,JSONObject b){return a!=null && (b.optString("id").isEmpty() || a.optString("id").equals(b.optString("id"))) && a.optLong("ts")==b.optLong("ts") && a.optString("name").equals(b.optString("name")) && a.optBoolean("me")==b.optBoolean("me");}
    private void locate(JSONObject m){
        locating=m;Attachments.locate(this,m,62);
    }
    private void fileMenu(JSONObject m){
        String[] options={getString(R.string.open_file),getString(R.string.forward_file),getString(R.string.remove_chat),getString(R.string.locate_file),getString(R.string.share_file),getString(R.string.retry_file)};
        new android.app.AlertDialog.Builder(this).setTitle(m.optString("name")).setItems(options,(d,n)->{
            if(n==0){if(FileCards.uri(m)==null)locate(m);else FileCards.open(this,m);}
            else if(n==1)FileCards.forward(this,m);
            else if(n==3)locate(m);
            else if(n==5){
                if(!FileCards.available(this,m)){locate(m);return;}
                new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.retry_file)).setMessage(m.optString("error",getString(R.string.old_failure))+"\n\n"+getString(R.string.forward_confirm,m.optString("name"),peer.optString("name"))).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.send_now),(retryDialog,retryWhich)->exec.execute(()->{JSONObject current=Store.peerById(peerId);boolean ok=current!=null&&TransferService.sendToPeer(current,FileCards.uri(m),m.optString("name"),m.optLong("size"));runOnUiThread(()->{renderThread();android.widget.Toast.makeText(this,getString(ok?R.string.sent:R.string.file_failed_actions),android.widget.Toast.LENGTH_LONG).show();});})).show();
            }
            else if(n==4){if(FileCards.uri(m)==null)locate(m);else FileCards.share(this,m);}
            else new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.remove_chat)).setMessage(getString(R.string.remove_chat_help)).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.remove_message),(confirm,which)->{JSONArray old=Store.thread(peerId),kept=new JSONArray();boolean removed=false;for(int j=0;j<old.length();j++){JSONObject item=old.optJSONObject(j);if(!removed&&same(item,m)){removed=true;continue;}kept.put(item);}Store.saveThread(peerId,kept);renderThread();}).show();
        }).show();
    }
}
