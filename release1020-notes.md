Android 1.0.20 beta, build 41

BPLUS_VERSION_CODE: 41
BplusShare-1.0.20.apk | 697307 bytes | SHA-256 c965209a35de313b9548b546d37bbb2a4ab570568013f377fde2fb68eb7edb75

Local-delete fixes:
- Exact URI deletion is followed by a conservative absence check. A provider returning success is not enough. Permission errors are not treated as absence.
- Already-gone files are counted separately; repeat delete cannot falsely count them as newly deleted.
- System recoverable/media consent retained. Nonmedia/document failure offers an Android document-picker write grant for the EXACT file. Wrong file rejected using URI/media identity or same device+inode, never filename matching.
- Group row rebuilds immediately after local deletion; Local file deleted replaces usable card, Open/Retry disabled. Shared sender/ID stays for authorized remote tombstone. Private local deletion removes its card. Library entry also removed.
- Clear deleted/already gone/not deleted summary and Copy details with provider/API/error. Failed files stay unchanged.

Admin roles:
- Creator remains admin and can delete ANY shared file regardless of uploader/platform.
- Creator can promote/demote joined members under Group info > Manage group > Manage admins.
- Delegated admins can delete any shared file and delete the group, through authenticated host requests. Only creator manages roles.
- This is NOT host migration or transfer of server ownership. Creator must stay online. All devices need1.0.20 for delegated roles. Offline/older devices remain pending.
- Roles delivery revision prevents old promotion overriding newer demotion; synced on join.

Earlier-list review: file actions reachable; local/shared deletion separate; same-ID Retry, durable queues and duplicate rebroadcast retained; exact-folder action retains truthful Downloads fallback; Android incoming shares include live groups; Forward files/selected text to devices/groups as new message/ID retained. Android text Forward uses long-press selection toolbar, not an ellipsis. Android Share is not a dedicated Save a copy button.

Verification:
- Full resources/Java/D8/zipalign compile and APK v2/v3 signature verified. Same signer as1.0.18/1.0.19:3f212fa85d56b444fb6ea7df09d0646c795b04bb833b7389e1388a025bb3cc52. com.bplus.share,versionCode41,versionName1.0.20.
- Actual FileDelete controller15 assertions under isolated API shims. Actual LocalGroups23 permission/tombstone +14 retry +12 delegated-admin tests under isolated shims. Source UI wiring/EN+AR resources checked.
- NOT device-verified: real MediaStore/SAF deletion, actual consent picker, folder opening, PC-Android end-to-end or real UI/RTL. Official emulator attempt could not start due memory minimum/no hardware acceleration. User's exact CSV exception remains unknown because1.0.19 suppressed it. New diagnostics make recurrence observable. Do not call this tested on a real phone.
- Preview is source-based illustration, not actual app screenshot.

No uninstall/re-pair from1.0.18/1.0.19.1.0.17 or older uses old signer and requires uninstall/reinstall, losing local app data/chats/pairings. Shared deletion does not erase saved copies on other devices.
