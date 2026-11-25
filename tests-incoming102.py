from pathlib import Path
import subprocess,xml.etree.ElementTree as E
r=Path.home()/'bplus-drop';t=Path('/tmp/incoming102');t.mkdir(exist_ok=True)
(t/'Test.java').write_text('''import com.bplus.share.IncomingText;public class Test{public static void main(String[]a){String[][] all={{"file","audio/mp4","voice-random.m4a","voice"},{"file","application/octet-stream","voice-foo.M4A","voice"},{"file","image/jpeg","secret.jpg","photo"},{"file","video/mp4","secret.mp4","video"},{"file","application/pdf","secret.pdf","file"},{"text","","raw text","text"},{"file","audio/ogg","unknown","voice"},{"file","","x.heic","photo"},{"file","","x.mov","video"}};for(String[]c:all)if(!IncomingText.type(c[0],c[1],c[2]).equals(c[3]))throw new AssertionError();System.out.println("PASS9 actual media classification cases, no filenames returned.");}}''')
j=Path.home()/'jdk17/bin';subprocess.run([str(j/'javac'),'-d',str(t),str(r/'app/src/com/bplus/share/IncomingText.java'),str(t/'Test.java')],check=True);subprocess.run([str(j/'java'),'-cp',str(t),'Test'],check=True)
langs=[]
for folder in ['values','values-ar']:
 a={x.attrib['name']:x.text for x in E.parse(r/'app/res'/folder/'strings.xml').getroot() if x.tag=='string'};langs.append(a)
assert set(langs[0])==set(langs[1]);assert langs[1]['incoming_voice']=='وصلتك رسالة صوتية' and langs[1]['preview_voice']=='رسالة صوتية'
src=r/'app/src/com/bplus/share';assert 'Alerts.preview(this,m,false)' in (src/'MainActivity.java').read_text();assert 'getString(R.string.notif_file, name)' not in (src/'TransferService.java').read_text();assert 'Alerts.message' in (src/'LanSync.java').read_text() and 'Alerts.message' in (src/'LocalGroups.java').read_text()
print('PASS shared notification/list classification routes, both locales and sender/title wiring. Actual Android notification pixels remain unverified.')

assert 'remove.setContentDescription(getString(R.string.remove_device))' in (src/'MainActivity.java').read_text() and 'DeviceRemoval.show(this,id,this::render)' in (src/'MainActivity.java').read_text()
print('PASS chat-list per-device visible menu and long-press remove route, existing confirmation reused. Actual Android layout unverified.')
