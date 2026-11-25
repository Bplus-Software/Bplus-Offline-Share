from pathlib import Path
import subprocess
r=Path.home()/'bplus-drop';t=Path('/tmp/chat116');t.mkdir(exist_ok=True)
(t/'Tests.java').write_text('''import com.bplus.share.*;import org.json.*;import java.util.*;
public class Tests{static int n;static void ok(boolean b,String s)throws Exception{if(!b)throw new Exception(s);n++;System.out.println("PASS "+s);}public static void main(String[]a)throws Exception{
JSONObject f1=new JSONObject().put("id","file1").put("kind","file").put("uri","content://one");JSONObject f2=new JSONObject().put("id","file2").put("kind","file").put("uri","content://two");JSONObject text=new JSONObject().put("id","text").put("body","hello");JSONArray old=new JSONArray().put(text).put(f1).put(f2);
Set<String> selected=new HashSet<>();selected.add("file1");Set<String> confirmed=new HashSet<>(selected);selected.clear();selected.add("file2");JSONArray kept=ChatRules.removeSelected(old,confirmed);
ok(kept.length()==2&&kept.getJSONObject(1)==f2,"confirmed selection snapshot removes original exact file only");ok(kept.getJSONObject(0)==text,"selected removal keeps text");ok(old.length()==3&&f1.optString("uri").equals("content://one"),"source records and URI metadata not mutated");ok(ChatRules.clear(old,true).length()==1&&ChatRules.clear(old,true).getJSONObject(0)==text,"clear file cards retains text");ok(ChatRules.clear(old,false).length()==0,"clear conversation removes local records");ok(ChatRules.removeSelected(old,new HashSet<>()).length()==3,"empty selection no-op");JSONObject legacy=new JSONObject().put("id","").put("uri","content://old").put("name","old.pdf").put("ts",10);ok(ChatRules.key(legacy).equals("content://old|old.pdf|10"),"empty legacy IDs use stable metadata fallback");Set<String> bad=new HashSet<>();bad.add("text");ok(ChatRules.removeSelected(old,bad).length()==3,"file-only selected removal cannot erase text ID collision");ok(ChatRules.clear(new JSONArray(),true).length()==0,"empty conversation safe");System.out.println(n+" pure record-rule assertions. No Android UI/storage runtime claim.");}}
''')
j=Path.home()/'jdk17/bin';subprocess.run([str(j/'javac'),'-cp','/tmp/org-json.jar','-d',str(t),str(r/'app/src/com/bplus/share/ChatRules.java'),str(t/'Tests.java')],check=True)
x=subprocess.run([str(j/'java'),'-cp',str(t)+':/tmp/org-json.jar','Tests'],capture_output=True,text=True,check=True);print(x.stdout);(r/'tests/chat116-results.txt').write_text(x.stdout)
# Source wiring checks are not runtime assertions.
for name in ['ChatActivity','GroupChatActivity']:
 s=(r/'app/src/com/bplus/share'/f'{name}.java').read_text();assert 'selection.row(m)' in s and 'rows.removeViewAt(i)' in s if name=='GroupChatActivity' else 'selection.row(m)' in s and 'msgs.removeViewAt(i)' in s
assert not any('new AlertDialog.Builder' in p.read_text() or 'new android.app.AlertDialog.Builder' in p.read_text() for p in (r/'app/src').rglob('*.java'))
print('PASS source wiring: row-level mark, deleted-row reconciliation, shared app dialog builder')
