from pathlib import Path
r=Path.home()/'bplus-drop/app/src/com/bplus/share'
u=(r/'Ui.java').read_text();helper=u.split('public static TextView icon(')[1].split('// settings-style row')[0]
assert 'Color.TRANSPARENT' in helper and 'setPadding(0,0,0,0)' in helper and 'dp(c,44)' in helper and 'Typeface.DEFAULT' in helper and 'Gravity.CENTER' in helper
m=(r/'MainActivity.java').read_text();assert 'Ui.icon(this,"⋮"' in m and 'Ui.ghost(this,"⋮"' not in m and 'tail,Ui.lp(Ui.dp(this,38),-2)' in m and 'menuLp.setMarginStart(Ui.dp(this,8))' in m
c=(r/'ChatActivity.java').read_text();assert all('Ui.icon(this,"'+s+'"' in c for s in ['ⓘ','⚙','⋮']) and 'Ui.ghost(this,' not in c
assert 'peerTitle.setSingleLine(true)' in c and 'username.setSingleLine(true)' in c
assert 'Ui.icon(this,"⚙"' in (r/'DevicesActivity.java').read_text()
# The longest normal HH:mm string fits the reserved 38dp at 10sp unless extreme accessibility scaling.
# Actual font/density/RTL pixels are a device verification item, not proven by this source check.
print('PASS transparent icon helper: 44dp centered targets/no inner padding; separate timestamp column+8dp gap; header equal icons and title/username ellipsis; Devices gear uses same helper. Source checks only, not Android pixels.')
