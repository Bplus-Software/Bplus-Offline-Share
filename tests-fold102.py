from pathlib import Path
import re,sqlite3,xml.etree.ElementTree as E
r=Path.home()/'bplus-drop';src=r/'app/src/com/bplus/share'
def read(n):return (src/n).read_text()
ledger=read('AutoLedger.java');sql=re.search(r'db.execSQL\("([^"]+)"\)',ledger).group(1);db=sqlite3.connect('/tmp/auto-ledger102.sqlite');db.execute('DROP TABLE IF EXISTS files');db.execute(sql)
def claim(uri,size=10,state='claimed'):
 cur=db.execute('INSERT OR IGNORE INTO files VALUES(?,?,?,?,?,?)',('folder',uri,uri,size,state,1));db.commit();return cur.rowcount==1
assert claim('old',state='baseline') and not claim('old')
assert claim('new') and not claim('new') and not claim('new',size=99)
db.close();db=sqlite3.connect('/tmp/auto-ledger102.sqlite');assert not claim('new')
assert db.execute('SELECT state FROM files WHERE uri=?',('old',)).fetchone()[0]=='baseline';db.close()
print('PASS actual SQLite schema: baseline skipped, new claimed once, resize skipped, restart does not repeat. Ledger algorithm exercised in Python SQLite, not Android SQLite API.')
s=read('TransferService.java');assert 'FileConsent.ask(peer.optString("name"),name,size)' in read('FileJobs.java') and 'FileConsent.ask(receiver.optString("name"),f.optString("name"),size)' in s
assert 'static boolean sendRecordedVoice' in s and 'sendOriginal(peer,uri,name,size,true)' in s
assert 'AutoLedger.claim(id+"|"+folder,f)' in s and 'AutoLedger.result(id+"|"+folder' in s
assert 'AutoLedger.baseline' in read('DeviceSettingsActivity.java') and 'Store.durable(key+"on","1")' in read('DeviceSettingsActivity.java')
assert 'FileConsent.ask' in read('LocalGroups.java') and 'share_confirm_external' in read('FileCards.java')
cap=read('CameraCapture.java');assert 'MediaStore.EXTRA_OUTPUT' in cap and 'FLAG_GRANT_WRITE_URI_PERMISSION' in cap and 'RESULT_OK' in cap and 'review();' in cap and 'revokeUriPermission' in cap and '100L*1024*1024' in cap and 'ACTION_VIDEO_CAPTURE' in cap and 'captureName' in cap
provider=read('CaptureProvider.java');assert 'getCanonicalFile' in provider and 'getPathSegments().size()!=1' in provider and 'f.isFile()' in provider
manifest=(r/'app/AndroidManifest.xml').read_text();assert 'versionCode="38" android:versionName="1.0.17"' in manifest and '.CaptureProvider' in manifest and 'com.bplus.share.captures' in manifest
voice=read('VoicePlayer.java');base=(r/'share-repo/app/src/com/bplus/share/VoicePlayer.java').read_text();assert 'cardBottom.getHeight()-trackAt[1]' in voice and 'clock.setTranslationY' in voice
for expr in ['Ui.dp(a,226),Ui.dp(a,50)','Ui.dp(a,32),Gravity.CENTER_VERTICAL','Ui.dp(a,15),Gravity.BOTTOM']:assert expr in voice and expr in base
assert 'Ui.pad(card,8,3,6,3,a)' in read('FileCards.java')
assert 'overCancel(e)' in read('VoiceRecorder.java') and 'cancelTarget' in read('VoiceRecorder.java') and 'sendRecordedVoice' in read('ChatActivity.java') and 'sendRecordedVoice' in read('GroupChatActivity.java')
print('PASS permission/output/camera review, fail-closed file gates/exemptions, actual-card measurement with original dimensions, cancel-target and mic-release source wiring. Android runtime/pixels unverified.')

assert 'configureAutosend' not in read('SettingsActivity.java')
assert 'Net.cancelAuto(id)' in read('DeviceSettingsActivity.java')
assert 'epoch.equals(Store.get(key+"epoch",""))' in read('TransferService.java')
assert 'Executors.newSingleThreadExecutor()' in read('TransferService.java')
net=read('Net.java');assert 'if(!allowed.getAsBoolean())throw new IOException("Automatic send stopped");out.write(buf,0,n);' in net and 'autoConnections.remove(id)' in net and 'new Thread(h::disconnect).start()' in net
print('PASS per-device default OFF/no global UI, unpair/disable cancels active HTTP, serialized queue and current epoch/folder/pairing guard. Source wiring, stop latency on phone unverified.')
