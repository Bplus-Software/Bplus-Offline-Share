from pathlib import Path
import xml.etree.ElementTree as E
r=Path('/home/sandbox/bplus-drop');d=r/'app/src/com/bplus/share'
for loc in ['values','values-ar']:
 vals=[s.attrib['name']for s in E.parse(r/'app/res'/loc/'strings.xml').getroot().findall('string')];assert len(vals)==len(set(vals))
s=(d/'ReceiveShareActivity.java').read_text();assert 'JSONArray groups=LocalGroups.all()'in s and 'group.optBoolean("deleted")||!group.optBoolean("joined")'in s and 'groupTarget(selected)'in s and 'LocalGroups.sendFile' in s and 'LocalGroups.sendText' in s
f=(d/'ForwardAction.java').read_text();assert 'LocalGroups.all()' in f and 'LocalGroups.sendFile' in f and 'LocalGroups.sendText' in f and 'Store.peerById(target.optString("id"))' in f and 'setPositiveButton(R.string.send_now' in f
assert 'ForwardAction.show(a,messages' in (d/'FileSelection.java').read_text()
assert 'codes.add(7)'in(d/'GroupChatActivity.java').read_text()
assert '()->fileMenu(m)'in(d/'ChatActivity.java').read_text()
assert 'item.remove("uri")'in(d/'FileDelete.java').read_text()
print('PASS source wiring: joined/nondeleted group incoming targets, revalidation, file+text routing, shared forward picker in private/groups, confirmation, preserved group record after local bytes delete; EN/AR resource uniqueness')
