Android 1.0.19, build 40 (beta)

Changes
- Android incoming share sheet lists joined live groups beside paired devices, with named target confirmation and revalidation before Send.
- Forward files or selected text/messages from private or group chats to any paired device or joined group. Groups listed first; each forward is a new message and ID, no forwarded flag.
- Visible file-options menu in direct chats and groups. Open, share, local delete, resend, and containing-folder actions. Direct private recall remains separate.
- Group creator is admin: delete the group or any shared file. Members can delete only their own uploads. Host authenticates member key + IP and stored sender, never a claimed role or actor.
- Shared file deletion leaves an inert named tombstone, blocks later resend/arrival, and does not delete original or downloaded bytes. Local file deletion is a separate confirmation and may require Android consent.
- Host-only group deletion removes group and conversation from the list; downloaded files remain.
- File retry keeps the message ID. Host rebroadcasts completed duplicate uploads without making duplicate rows. Per-member file/control delivery queue persists with backoff, startup and later-contact retry. Pending older/offline devices are shown as pending, not deleted successfully.
- Open containing folder uses document-provider parent where possible; otherwise opens Downloads with an explicit notice that the exact folder is unavailable.

BPLUS_VERSION_CODE: 40
BplusShare-1.0.19.apk | 689115 bytes | SHA-256 88347c69e2d8945ad662df5a1006cbd210e8f9d6fb895009c8e5b39e04f574b1

Upgrade: same package and signing certificate as 1.0.18, so no uninstall/re-pair needed from 1.0.18. Devices still on 1.0.17 or older require uninstall/reinstall because 1.0.18 changed the signer; uninstall loses local app data/chats/pairings. Back up any wanted data first.
Signer SHA-256: 3f212fa85d56b444fb6ea7df09d0646c795b04bb833b7389e1388a025bb3cc52

Verification: full resources/Java/D8/zipalign build, APK v2/v3 signature, manifest package/version, same signer as 1.0.18. Actual LocalGroups implementation with isolated Android/storage/network shims: 23 permission/tombstone tests + 14 retry/queue tests. Live physical Android installation, Android-PC end-to-end and platform folder/delete consent dialogs were not tested. UI preview is a source-based illustration, not a device screenshot. Existing 1.0.18 peers can exchange existing content but need 1.0.19 for shared deletion; unsupported/offline members remain pending. Deletion is not a security erase and cannot revoke copies saved outside the app.
