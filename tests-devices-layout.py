from pathlib import Path
s=(Path.home()/'bplus-drop/app/src/com/bplus/share/DevicesActivity.java').read_text();row=s.split('private View peerRow')[1].split('private View groupRow')[0]
assert 'LinearLayout r = Ui.col(this)' in row
assert 'r.addView(identity,Ui.lp(-1,-2))' in row and 'r.addView(actions,Ui.lp(-1,-2))' in row
assert 'title.setSingleLine(true)' in row and 'st.setSingleLine(true)' in row and row.count('TruncateAt.END')>=3
assert 'identity.addView(meta,Ui.lpw(1))' in row and 'actions.addView(send,new LinearLayout.LayoutParams(0,Ui.dp(this,44),1))' in row
assert 'actions.addView(settings,settingsLp)' in row and 'actions.addView(menu)' in row
assert 'Ui.ghost(this,getString(R.string.remove_device))' not in row and 'DeviceRemoval.menu' in row
assert 'send.setEnabled(!unpaired)' in row and '0xffff6565' in row
print('PASS Devices source layout: independent identity/actions rows, one-line ellipsis, weighted Send/44dp settings-menu, Remove in confirmed menu, unpaired status and send block. Actual Android measurement/RTL/font-scale not proven.')
