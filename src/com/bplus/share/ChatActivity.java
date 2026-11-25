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
    private FileSelection selection;private VoiceRecorder voice;private CameraCapture capture;
    private JSONObject locating;
    private final java.util.Set<String> retrying=java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
    private final java.util.Map<String,TextView> progressLabels=new java.util.HashMap<>();private final java.util.Map<String,android.widget.ProgressBar> progressBars=new java.util.HashMap<>();
    private String peerId;
    private android.widget.ImageView peerAvatar;private String shownPhoto="";private JSONObject peer;private TextView peerTitle;private boolean resumed;private String drawn="";
    private final android.os.Handler syncUi=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable syncTick=new Runnable(){public void run(){if(!resumed)return;JSONObject p=Store.listedPeer(peerId);if(p==null){finish();return;}if(peerAvatar!=null&&!shownPhoto.equals(p.optString("photo"))){shownPhoto=p.optString("photo");android.widget.ImageView fresh=Profile.avatar(ChatActivity.this,p,40);peerAvatar.setImageDrawable(fresh.getDrawable());}if(peerTitle!=null)peerTitle.setText(p.optString("name")+(p.optBoolean("unpaired")?" · "+getString(R.string.not_paired):""));String now=Store.thread(peerId).toString();if(!now.equals(drawn)&&!selection.active()){preserveSelectionScroll=true;renderThread();preserveSelectionScroll=false;}markVisibleRead();syncUi.postDelayed(this,1000);}};
    private void markVisibleRead(){if(!resumed||!hasWindowFocus()||msgs==null||scroll==null)return;android.graphics.Rect viewport=new android.graphics.Rect();if(!scroll.getGlobalVisibleRect(viewport))return;for(int i=0;i<msgs.getChildCount();i++){View row=msgs.getChildAt(i);if(!(row.getTag() instanceof JSONObject))continue;JSONObject m=(JSONObject)row.getTag();android.graphics.Rect bounds=new android.graphics.Rect();if(!m.optBoolean("me")&&row.getGlobalVisibleRect(bounds)&&android.graphics.Rect.intersects(viewport,bounds))LanSync.read(peerId,m.optString("wireId"));}}

    private LinearLayout msgs;
    private ScrollView scroll;
    private final java.util.concurrent.ExecutorService exec = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.applyLocale(this);
        Ui.applyTheme(this);
        peerId = getIntent().getStringExtra("peer");
        capture=new CameraCapture(this,(uri,name,size)->exec.execute(()->{JSONObject current=Store.peerById(peerId);if(current!=null)TransferService.sendToPeer(current,uri,name,size);runOnUiThread(this::renderThread);}),b);
        peer = Store.listedPeer(peerId);
        if(peer==null){finish();return;}selection=new FileSelection(this,peerId,this::refreshSelection);

        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.bg);
        String name = peer != null ? peer.optString("name") : "";
        LinearLayout head = Ui.row(this);
        Ui.pad(head, 8, 10, 8, 10, this);
        TextView back = Ui.t(this, "‹", 26, Ui.dim, App.cairoB);
        back.setGravity(Gravity.CENTER);
        head.addView(back, Ui.lp(Ui.dp(this, 38), Ui.dp(this, 38)));
        android.widget.ImageView avatar=Profile.avatar(this,peer,40);peerAvatar=avatar;shownPhoto=peer.optString("photo");head.addView(avatar);avatar.setContentDescription(getString(R.string.profile_details));avatar.setOnClickListener(v->ProfileCard.show(this,peerId));
        back.setOnClickListener(v -> finish());
        LinearLayout meta = Ui.col(this);
        Ui.pad(meta, 12, 0, 12, 0, this);
        peerTitle=Ui.t(this,name,16,Ui.text,App.cairoB);peerTitle.setSingleLine(true);peerTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);meta.addView(peerTitle);
        if(!peer.optString("username").isEmpty()){TextView username=Ui.t(this,"@"+peer.optString("username"),11,Ui.dim,App.cairoSB);username.setSingleLine(true);username.setEllipsize(android.text.TextUtils.TruncateAt.END);meta.addView(username);}
        Store.set("read_"+peerId,String.valueOf(System.currentTimeMillis()));
        TextView status = Ui.t(this, "", 11, Ui.green, App.cairoB);
        meta.addView(status);
        head.addView(meta, Ui.lpw(1));
        TextView diagnose=Ui.icon(this,"ⓘ",getString(R.string.connection_details));head.addView(diagnose);diagnose.setContentDescription(getString(R.string.connection_details));diagnose.setOnClickListener(v->diagnostics());
        status.setOnClickListener(v->diagnostics());
        if(!peer.optBoolean("unpaired")){TextView settings=Ui.icon(this,"⚙",getString(R.string.device_settings));settings.setContentDescription(getString(R.string.device_settings));settings.setOnClickListener(v->DeviceSettingsActivity.open(this,peerId));head.addView(settings);}
        TextView chatMenu=Ui.icon(this,"⋮",getString(R.string.chat_options));chatMenu.setContentDescription(getString(R.string.chat_options));head.addView(chatMenu);chatMenu.setOnClickListener(v->ChatActions.show(this,peerId,()->{selection.back();refreshSelection();}));
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
        attach.setOnClickListener(v -> new AppDialog(this).setTitle(R.string.capture_attach).setItems(new String[]{getString(R.string.capture_photo),getString(R.string.capture_video),getString(R.string.capture_file)},(d,n)->{if(n==2)pickFile();else capture.launch(n==1);}).show());
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
        TextView send = new ComposerButton(this);
        send.setGravity(Gravity.CENTER);
        send.setBackground(Ui.round(Ui.accent, 24, this));
        bar.addView(send, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            input.setText("");
            sendText(text);
        });
        TextView emoji=Ui.t(this,"☺",22,Ui.accent,App.cairoB);emoji.setGravity(Gravity.CENTER);LinearLayout.LayoutParams emojiLp=Ui.lp(Ui.dp(this,44),Ui.dp(this,44));emojiLp.setMarginEnd(Ui.dp(this,12));bar.addView(emoji,0,emojiLp);
        emoji.setContentDescription(getString(R.string.emoji_title));
        emoji.setOnClickListener(v->EmojiPicker.show(this,input));
        voice=new VoiceRecorder(this,send,input,(uri,voiceName,size)->exec.execute(()->{TransferService.sendRecordedVoice(peer,uri,voiceName,size);runOnUiThread(this::renderThread);}));
        send.setText("🎙");input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){send.setText(s.length()==0?"🎙":"➤");send.setContentDescription(getString(s.length()==0?R.string.voice_hold:R.string.send_now));}public void afterTextChanged(android.text.Editable e){}});
        root.addView(bar);
        setContentView(root);

        renderThread();
        TransferService.listen("chat", (type, data) -> {
            if (!peerId.equals(data.optString("from"))) return;
            runOnUiThread(()->{if(resumed){if(type.equals("file-progress"))refreshFileProgress();else renderThread();}});
        });
        if (getIntent().getBooleanExtra("attach", false)) pickFile();
    }

    @Override public void onBackPressed(){if(EmojiPicker.dismiss(this))return;if(selection!=null&&selection.back())return;super.onBackPressed();}
    @Override protected void onDestroy() {if(voice!=null)voice.cancel();VoicePlayer.release(this);EmojiPicker.dismiss(this);
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

    @Override protected void onResume(){super.onResume();resumed=true;if(msgs!=null)renderThread();syncUi.removeCallbacks(syncTick);syncUi.post(syncTick);Outbox.kick();}
    @Override protected void onPause(){resumed=false;syncUi.removeCallbacks(syncTick);if(voice!=null)voice.cancel();VoicePlayer.release(this);super.onPause();}
    private void renderThread() {
        VoicePlayer.release(this);
        msgs.removeAllViews();progressLabels.clear();progressBars.clear();
        JSONArray t = Store.thread(peerId);drawn=t.toString();
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.US);
        for (int i = 0; i < t.length(); i++) {
            JSONObject m = FileJobs.display(t.optJSONObject(i));
            if (m == null) continue;
            boolean me = m.optBoolean("me");
            String kind = m.optString("kind", "text");
            View bub;
            if(kind.equals("text")){
                TextView text=Ui.t(this,m.optString("body"),14,me?Color.WHITE:Ui.text,App.cairoSB);
                ChatLinks.apply(this,text,()->selection.active());text.setLineSpacing(0,1.25f);text.setMaxWidth(Ui.dp(this,280));text.setBackground(Ui.round(me?Ui.accent:Ui.card,16,this));Ui.pad(text,13,9,13,9,this);if(me&&m.optString("state").equals("failed"))text.setOnClickListener(v->retryMessage(m));text.setOnLongClickListener(v->{selection.toggle(m);return true;});bub=text;
            }else{
                bub=FileCards.card(this,m,()->selection.toggle(m),()->fileMenu(m));


                if(!selection.active()&&!Attachments.state(this,m).equals("ok"))bub.setOnClickListener(v->locate(m));
            }
            String time = tf.format(new Date(m.optLong("ts")));
            String state=m.optString("state");
            if(me)time+=" · "+(state.equals("read")?"✓✓ "+getString(R.string.status_read):state.equals("delivered")?"✓✓ "+getString(R.string.status_delivered):state.equals("queued")||state.equals("sending")?"◷ "+getString(R.string.status_queued):state.equals("canceled")?getString(R.string.status_canceled):state.equals("failed")?getString(kind.equals("text")?R.string.failed_retry:R.string.file_failed_actions):"✓ "+getString(R.string.sent));
            TextView stamp = Ui.t(this,time,10,state.equals("read")?0xff58a4ff:Ui.dim,App.cairoSB);
            LinearLayout cell = selection.row(m);cell.setTag(m);cell.setGravity(me?Gravity.END:Gravity.START);if(selection.has(m))cell.setBackground(Ui.round(0x3058a4ff,12,this));if(selection.active())cell.setOnClickListener(v->selection.toggle(m));
            LinearLayout.LayoutParams bp=Ui.lp(-2,-2);bp.gravity=me?Gravity.END:Gravity.START;cell.addView(kind.equals("file")?selection.decorate(bub,m):bub,bp);if(kind.equals("file")&&!Attachments.state(this,m).equals("ok")){TextView fix=Ui.t(this,Attachments.explanation(this,Attachments.state(this,m))+"\n"+getString(R.string.locate_file),12,Ui.accent,App.cairoSB);fix.setMaxWidth(Ui.dp(this,280));fix.setOnClickListener(v->locate(m));cell.addView(fix);}cell.addView(stamp);if(m.optBoolean("recallFailed"))cell.addView(Ui.t(this,getString(R.string.recall_pending_label),11,0xffff8585,App.cairoSB));
            if(me && state.equals("failed") && kind.equals("text")){
                String reason=m.optString("error",getString(R.string.old_failure));TextView failure=Ui.t(this,reason+"\n"+getString(R.string.retry_details),11,Ui.dim,App.cairoSB);failure.setMaxWidth(Ui.dp(this,280));failure.setOnClickListener(v->retryMessage(m));cell.addView(failure);
            }
            if(me&&kind.equals("file")&&!m.optString("job").isEmpty()){
             String phase=m.optString("phase"),job=m.optString("job");if(state.equals("sending")){android.widget.ProgressBar bar=Ui.hbar(this,6);bar.setProgress((int)Math.min(1000,m.optLong("done")*1000/Math.max(1,m.optLong("size"))));cell.addView(bar,Ui.lp(Ui.dp(this,260),Ui.dp(this,6)));progressBars.put(job,bar);String label=phase.equals("cancelling")?getString(R.string.file_cancelling):phase.equals("confirming")?getString(R.string.file_confirming):phase.equals("retrying")?getString(R.string.file_retrying):phase.equals("connecting")?getString(R.string.file_connecting):getString(R.string.sending);TextView progressLabel=Ui.t(this,label+" · "+(m.optLong("done")*100/Math.max(1,m.optLong("size")))+"% · "+Ui.fmtSize(m.optLong("speed"))+"/s",12,Ui.accent,App.cairoSB);cell.addView(progressLabel);progressLabels.put(job,progressLabel);TextView stop=Ui.ghost(this,getString(R.string.cancel));cell.addView(stop);stop.setOnClickListener(v->FileJobs.cancel(job));}
             if(state.equals("failed")||state.equals("canceled")||state.equals("sending")&&!FileJobs.active(job)){TextView retry=Ui.ghost(this,getString(R.string.retry_file));cell.addView(retry);retry.setOnClickListener(v->exec.execute(()->FileJobs.retry(peerId,m)));if(state.equals("failed")){TextView error=Ui.t(this,m.optString("error"),11,Ui.dim,App.cairoSB);cell.addView(error);error.setOnClickListener(v->TransferError.show(this,m));}if(DeveloperMode.enabled()&&m.optJSONObject("timing")!=null){TextView copy=Ui.ghost(this,getString(R.string.copy_details));cell.addView(copy);copy.setOnClickListener(v->PairUi.copy(this,m.optJSONObject("timing").toString()));}}
            }
            Ui.pad(cell,0,0,0,12,this);
            LinearLayout.LayoutParams rowLp=Ui.lp(-1,-2);rowLp.bottomMargin=Ui.dp(this,8);msgs.addView(cell,rowLp);
        }
        if(!preserveSelectionScroll)scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void refreshFileProgress(){for(java.util.Map.Entry<String,TextView> entry:progressLabels.entrySet()){try{JSONObject m=FileJobs.display(new JSONObject().put("job",entry.getKey()));String phase=m.optString("phase");String label=getString(phase.equals("cancelling")?R.string.file_cancelling:phase.equals("confirming")?R.string.file_confirming:phase.equals("retrying")?R.string.file_retrying:phase.equals("connecting")?R.string.file_connecting:R.string.sending);entry.getValue().setText(label+" · "+(m.optLong("done")*100/Math.max(1,m.optLong("size")))+"% · "+Ui.fmtSize(m.optLong("speed"))+"/s");android.widget.ProgressBar bar=progressBars.get(entry.getKey());if(bar!=null)bar.setProgress((int)Math.min(1000,m.optLong("done")*1000/Math.max(1,m.optLong("size"))));}catch(Exception ignored){}}}

    private void refreshSelection(){if(msgs==null)return;int y=scroll.getScrollY();java.util.HashSet<String> live=new java.util.HashSet<>();JSONArray saved=Store.thread(peerId);for(int j=0;j<saved.length();j++){JSONObject item=saved.optJSONObject(j);if(item!=null)live.add(FileSelection.key(item));}for(int i=msgs.getChildCount()-1;i>=0;i--){View row=msgs.getChildAt(i);if(row.getTag() instanceof JSONObject&&!live.contains(FileSelection.key((JSONObject)row.getTag())))msgs.removeViewAt(i);}for(int i=0;i<msgs.getChildCount();i++){View row=msgs.getChildAt(i);Object tag=row.getTag();if(tag instanceof JSONObject){JSONObject m=(JSONObject)tag;{row.setBackground(selection.has(m)?Ui.round(0x3058a4ff,12,this):null);row.setOnClickListener(selection.active()?v->selection.toggle(m):null);}}invalidateSelection(row);}scroll.post(()->scroll.scrollTo(0,y));}
    private void invalidateSelection(View v){v.invalidate();if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)invalidateSelection(g.getChildAt(i));}}
    private boolean preserveSelectionScroll;
    private void sendText(String text){try{Outbox.enqueue(peerId,text);renderThread();}catch(Exception e){new AppDialog(this).setMessage(e.getMessage()).setPositiveButton(android.R.string.ok,null).show();}}
    private void deliver(JSONObject m){Outbox.kick();}
    private void retryMessage(JSONObject m){
        if(retrying.contains(m.optString("id")))return;
        final JSONObject[] selected={m};
        new AppDialog(this).setTitle(getString(R.string.not_delivered_title)).setMessage(m.optString("error",getString(R.string.old_failure))+"\n\n"+getString(R.string.retry_duplicate_note)).setNegativeButton(getString(R.string.cancel),null).setNeutralButton(getString(R.string.connection_details),(d,n)->diagnostics()).setPositiveButton(getString(R.string.try_again),(d,n)->{
            synchronized(Store.class){JSONArray thread=Store.thread(peerId);for(int i=0;i<thread.length();i++){JSONObject item=thread.optJSONObject(i);if(same(item,m)&&item.optString("body").equals(m.optString("body"))){try{if(item.optString("id").isEmpty())item.put("id",java.util.UUID.randomUUID().toString());item.put("state","sending");selected[0]=item;}catch(Exception ignored){}break;}}Store.saveThread(peerId,thread);}renderThread();deliver(selected[0]);
        }).show();
    }
    private void diagnostics(){
        JSONObject current=Store.peerById(peerId);String local=Net.localIp(this);String ip=current==null?"?":current.optString("ip");int port=current==null?Store.PORT:current.optInt("port",Store.PORT);
        TextView details=Ui.t(this,getString(R.string.checking),13,Ui.text,App.cairoSB);Ui.pad(details,20,20,20,20,this);details.setTextIsSelectable(true);
        android.app.AlertDialog dialog=new AppDialog(this).setTitle(getString(R.string.connection_details)).setView(details).setPositiveButton(android.R.string.ok,null).setNeutralButton(getString(R.string.open_wifi),(d,n)->PairUi.wifi(this)).setNegativeButton(getString(R.string.copy_details),(d,n)->PairUi.copy(this,details.getText().toString())).create();dialog.show();
        exec.execute(()->{boolean ok=current!=null&&Net.ping(ip,port,current.optString("token"))!=null;String reason=current==null?getString(R.string.peer_missing):ok?getString(R.string.device_responded):Net.lastError();String text=getString(R.string.app_name)+" "+AppVersion.display(this)+"\n"+getString(R.string.target_address)+": "+ip+":"+port+"\n"+getString(R.string.local_address)+": "+(local==null?getString(R.string.no_lan):local)+"\n"+getString(R.string.failure_reason)+": "+reason+"\n\n"+getString(R.string.diagnostic_note);runOnUiThread(()->{if(dialog.isShowing())details.setText(text);});});
    }

    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);if(capture!=null)capture.save(b);}
    @Override public void onRequestPermissionsResult(int q,String[] p,int[] r){super.onRequestPermissionsResult(q,p,r);if(capture!=null)capture.permission(q,r);}
    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_PICK);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);if(capture!=null&&capture.result(req,res,data))return;if(selection!=null&&selection.activityResult(req,res,data))return;
        if(req==62 && res==RESULT_OK && data!=null && data.getData()!=null && locating!=null){
            Uri u=data.getData();if(!Attachments.readable(this,u)){android.widget.Toast.makeText(this,R.string.attach_permission,1).show();return;}try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            JSONObject selected=locating;locating=null;
            new AppDialog(this).setMessage(getString(R.string.locate_confirm,selected.optString("name"))+"\n"+u.getLastPathSegment()+"\n"+getString(R.string.locate_safe_note)).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(android.R.string.ok,(d,n)->{
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
                try { getContentResolver().takePersistableUriPermission(u, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION)); } catch (Exception ignored) {}
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
        String[] options={getString(R.string.open_file),getString(R.string.forward_file),getString(R.string.remove_chat),getString(R.string.locate_file),getString(R.string.share_file),getString(R.string.retry_file),getString(R.string.delete_local_files),getString(R.string.delete_everyone),getString(R.string.open_folder)};
        new AppDialog(this).setTitle(m.optString("name")).setItems(options,(d,n)->{
            if(n==8){Attachments.folder(this,m);return;}if(n==7){java.util.ArrayList<JSONObject> chosen=new java.util.ArrayList<>();chosen.add(m);RecallAction.show(this,peerId,chosen,this::renderThread);return;}if(n==6){selection.deleteLocal(m);return;}if(n==0){if(FileCards.uri(m)==null)locate(m);else FileCards.open(this,m);}
            else if(n==1)FileCards.forward(this,m);
            else if(n==3)locate(m);
            else if(n==5){
                if(m.optString("job").matches("[a-f0-9]{24}")&&m.optBoolean("me")){exec.execute(()->FileJobs.retry(peerId,m));return;}
                if(!FileCards.available(this,m)){locate(m);return;}
                new AppDialog(this).setTitle(getString(R.string.retry_file)).setMessage(m.optString("error",getString(R.string.old_failure))+"\n\n"+getString(R.string.forward_confirm,m.optString("name"),peer.optString("name"))).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.send_now),(retryDialog,retryWhich)->exec.execute(()->{JSONObject current=Store.peerById(peerId);boolean ok=current!=null&&TransferService.sendToPeer(current,FileCards.uri(m),m.optString("name"),m.optLong("size"));runOnUiThread(()->{renderThread();android.widget.Toast.makeText(this,getString(ok?R.string.sent:R.string.file_failed_actions),android.widget.Toast.LENGTH_LONG).show();});})).show();
            }
            else if(n==4){if(FileCards.uri(m)==null)locate(m);else FileCards.share(this,m);}
            else new AppDialog(this).setTitle(getString(R.string.remove_chat)).setMessage(getString(R.string.remove_chat_help)).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.remove_message),(confirm,which)->{JSONArray old=Store.thread(peerId),kept=new JSONArray();boolean removed=false;for(int j=0;j<old.length();j++){JSONObject item=old.optJSONObject(j);if(!removed&&same(item,m)){removed=true;continue;}kept.put(item);}Store.saveThread(peerId,kept);renderThread();}).show();
        }).show();
    }
}
