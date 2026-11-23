from pathlib import Path
import subprocess,xml.etree.ElementTree as E
r=Path.home()/'bplus-drop';t=Path('/tmp/voice1111');t.mkdir(exist_ok=True)
(t/'Tests.java').write_text('''import com.bplus.share.VoiceRules;public class Tests{public static void main(String[]a){if(!VoiceRules.cancel(100,10,80)||!VoiceRules.cancel(10,100,80)||VoiceRules.cancel(10,20,80))throw new RuntimeException("slide");if(VoiceRules.send(true,1000,100)||VoiceRules.send(false,200,100)||VoiceRules.send(false,300000,100)||VoiceRules.send(false,1000,0)||!VoiceRules.send(false,1000,100))throw new RuntimeException("send guard");if(!VoiceRules.voice("audio/mp4")||VoiceRules.voice("image/jpeg"))throw new RuntimeException("audio");System.out.println("PASS voice slide both directions/short movement, cancel/short/limit/empty guards, valid release, audio MIME routing. Pure rules only.");}}''')
j=Path.home()/'jdk17/bin';subprocess.run([str(j/'javac'),'-d',str(t),str(r/'app/src/com/bplus/share/VoiceRules.java'),str(t/'Tests.java')],check=True);subprocess.run([str(j/'java'),'-cp',str(t),'Tests'],check=True)
for name in ['ChatActivity','GroupChatActivity']:
 s=(r/'app/src/com/bplus/share'/f'{name}.java').read_text();assert 'new VoiceRecorder' in s and 'voice.cancel()' in s and 'VoicePlayer.release' in s
s=(r/'app/src/com/bplus/share/VoiceRecorder.java').read_text();assert 'requestPermissions' in s and 'MediaRecorder.AudioEncoder.AAC' in s and 'ACTION_CANCEL' in s and 'voice_limit' in s
s=(r/'app/src/com/bplus/share/VoicePlayer.java').read_text();assert 'seekTo' in s and 'pause()' in s and 'getDuration()' in s
keys=lambda f:{x.attrib['name'] for x in E.parse(f).getroot()};en=keys(r/'app/res/values/strings.xml');assert en==keys(r/'app/res/values-ar/strings.xml');print('PASS voice recording/player/lifecycle wiring and',len(en),'EN/AR keys. No Android audio/UI test.')
