package com.bplus.share;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.Build;import android.provider.*;import org.json.*;import java.util.*;
/** Deletes only the exact URI explicitly confirmed by the user. No name/path matching. */
public final class FileDelete {
 public static final int REQUEST=819;private final Activity a;private final ArrayList<JSONObject> files;private final Runnable done;private final ArrayList<String> failures=new ArrayList<>();private final Set<String> seen=new HashSet<>();private int index,deleted;private Uri waiting;private JSONObject current;private boolean systemDeletes;
 public FileDelete(Activity a,ArrayList<JSONObject> files,Runnable done){this.a=a;this.files=files;this.done=done;}
 public void start(){next();}
 private void next(){while(index<files.size()){current=files.get(index++);Uri u=FileCards.uri(current);if(u==null){fail(R.string.delete_no_reference);continue;}if(!seen.add(u.toString()))continue;try{
 boolean ok;if(DocumentsContract.isDocumentUri(a,u))ok=DocumentsContract.deleteDocument(a.getContentResolver(),u);else ok=a.getContentResolver().delete(u,null,null)>0;
 if(ok){deleted++;continue;}if(mediaConsent(u))return;fail(R.string.delete_refused);
 }catch(SecurityException e){if(Build.VERSION.SDK_INT>=29&&e instanceof android.app.RecoverableSecurityException&&consent(((android.app.RecoverableSecurityException)e).getUserAction().getActionIntent().getIntentSender(),u,false))return;if(mediaConsent(u))return;fail(R.string.delete_read_only);
 }catch(Exception e){if(mediaConsent(u))return;fail(R.string.delete_refused);}}
 done.run();StringBuilder message=new StringBuilder(a.getString(R.string.delete_summary,deleted,failures.size()));for(String s:failures)message.append("\n\n").append(s);message.append("\n\n").append(a.getString(R.string.delete_records_kept));new AppDialog(a).setTitle(R.string.action_delete).setMessage(message.toString()).setPositiveButton(android.R.string.ok,null).show();
 }
 private boolean mediaConsent(Uri u){if(Build.VERSION.SDK_INT<30)return false;try{Uri media="media".equals(u.getAuthority())?u:MediaStore.getMediaUri(a,u);return media!=null&&"media".equals(media.getAuthority())&&consent(MediaStore.createDeleteRequest(a.getContentResolver(),java.util.Collections.singletonList(media)).getIntentSender(),media,true);}catch(Exception e){return false;}}
 private boolean consent(IntentSender sender,Uri u,boolean deletes){try{waiting=u;systemDeletes=deletes;a.startIntentSenderForResult(sender,REQUEST,null,0,0,0);return true;}catch(Exception e){waiting=null;return false;}}
 public void result(int result){if(waiting==null)return;Uri u=waiting;waiting=null;if(result!=Activity.RESULT_OK){fail(R.string.delete_cancelled);next();return;}if(systemDeletes){deleted++;next();return;}try{boolean ok=DocumentsContract.isDocumentUri(a,u)?DocumentsContract.deleteDocument(a.getContentResolver(),u):a.getContentResolver().delete(u,null,null)>0;if(ok)deleted++;else fail(R.string.delete_refused);}catch(Exception e){fail(R.string.delete_read_only);}next();}
 private void fail(int reason){failures.add(current.optString("name")+"\n"+a.getString(reason));}
}
