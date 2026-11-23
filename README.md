# B+ Share

Native Android app for direct phone-to-phone file sharing on a trusted local Wi-Fi network. Android 8 or later. English and Arabic, light and dark themes.

## Version 1.1.2, unvalidated device-test pre-release

- Restores external file sharing that became unreachable in private/group chats when 1.1.1 added selection. Selection header now has a distinct Share icon, separate from internal Send. Android's chooser supports one or multiple readable content URIs with per-item ClipData and temporary read grants. Mixed MIME batches are handled. WhatsApp/Messenger appear only if installed and compatible. The user chooses the recipient and confirms there; opening the chooser does not mean a file was sent. An unavailable item stops the whole batch with a named error rather than being silently skipped.
- Compact in-content selection header replaces the normal header instead of stacking a system action bar above it. Count, back-to-exit, selected checkmarks and unselected circles. Tap a file or its mark to add/remove it. Select all remains. Works in private/group chats and the visible library category. Library offers Share/Send/Select all, not deletion of chat entries. Chat deletion behavior remains explicitly confirmed, card-only vs local bytes.
- Visible per-file options menu: Open / Share to other apps / Select, across private chats, group chats and library. Filename limited to two lines with ellipsis. Long names remain available in the options title.
- Find nearby moves from the top + to a clear right-side floating magnifier. Accessibility label: Find nearby. Top overflow keeps QR, library, transfers, group creation and legacy lists available.

Validation: clean SDK34 build, versionCode 7 and original signing certificate, 246 matching EN/AR keys. Seven JVM assertions exercise the actual share-intent builder against Android value-object doubles and MIME rules: single/multiple streams, all-item ClipData, read grant, empty/file-URI rejection and MIME aggregation. Eighteen group, seventeen nearby and five message-network assertions pass; updater fault injection and exact released 1.1.1 downloader passed against this candidate with matching SHA. These do not establish Android OS chooser grants, selection gestures, missing-item dialogs or WhatsApp/Messenger receipt on real phones.

1.1.2 UI has not been rendered at Android runtime. The screenshot of 1.1.1 exposed the missing sharing/checkmark behavior; it is not visual validation of this build. This is an unvalidated test handoff, risk unmeasured. Prefer a second phone first. Install manually over the existing app, do not uninstall. Normal latest remains 1.0.3 and the updater excludes pre-releases. Downgrade can require uninstall and lose local chats/settings. Existing group, storage, notification and network limitations from 1.1.1 remain. No internet relay, Windows or encryption. Trusted private Wi-Fi only.

## Version 1.1.1, unvalidated device-test pre-release

- Compact flat bottom navigation: Chats / Groups / Profile / Settings, drawn icons, rounded search, smaller filters and tighter chat rows. Transfers/library/QR remain under +. Colored first-letter default avatars replace the stock Android graphic.
- Photo editor supports square crop, circular preview, drag and pinch zoom before saving a resized avatar. Original image is unchanged. Photo rotation from EXIF is not handled in this trial.
- Removed text quick-reply chips and text phrases from the emoji picker. Emoji insertion and icon-only attach/send controls remain. Message text, file names, errors and confirmation wording are retained. This interprets the request as removing quick replies, not hiding all labels.
- Groups tab lists real local-host groups and invitations. Creator hosts over the same trusted Wi-Fi. Invite paired devices from group information, open Groups on each phone and explicitly join. Text and files are relayed through the host. Members do not receive each other's pairing secrets. Unique message IDs suppress duplicate stored deliveries. No automatic retries, offline queue/history backfill, member removal, leave flow or host migration yet. Fixed IPs must remain reachable; re-pair/re-create after network changes may be needed. Legacy file recipient lists remain separate, not silently converted.
- Long-press a chat file for multi-select and top Send / Delete / Select all icons. Applies to one-to-one and live group chats, not the received-library grid. Send selects a paired device and confirms. Delete asks card-only vs local file deletion and confirms. Card-only keeps bytes; disk deletion keeps records, can include original sent files, never deletes another phone's copy, and reports refused deletions. Protected/legacy-provider files may not be deletable by this app.
- Old attachment errors distinguish absent reference, access denied, unavailable/missing target and provider error. A visible Locate file action lets the user select and confirm a readable original copy without moving/deleting it. A versioned local metadata snapshot precedes changes. No filename-only auto-linking and no claim of deleted-file recovery. Versions 1.0/1.0.1 did not retain received URIs; a surviving card therefore does not prove bytes were deleted. The specific reported device failure remains unconfirmed. No real upgrade-preservation test has been performed.
- Incoming text/files use a new audible notification channel, separate from the silent service channel. Requires Android notification permission and enabled channel/sound; silent/DND and battery restrictions apply. Only works when the app/service receives over the local network. No internet push when closed/unreachable.

Validation: clean SDK34 build, versionCode 6, original signer, 240 EN/AR keys. Eighteen JVM group assertions (membership, source binding, deduplication, duplicate file-body drain), seventeen nearby assertions, five message-network cases, updater fault injection, exact released 1.1.0 downloader fetching the candidate through redirects with matching SHA. Group file storage/relay, crop gestures, selection/deletion, notification sound and Android upgrade preservation are NOT device-tested.

UI in 1.1.1 has never been rendered at Android runtime. The supplied real screenshot was of 1.1.0 only, and exposed the design defects addressed here. This is an unvalidated test handoff, not a finished/visually checked release. Risk is unmeasured. Prefer a second phone first. All group phones need 1.1.1. Install manually over the existing app without uninstalling. The normal latest release remains 1.0.3. Downgrade to lower versionCode can require uninstall and lose local chats/settings. No internet relay, encryption or Windows. Trusted private networks only. Ordinary transfers have no checksum/resume; files >=2 GB unsupported.

## Version 1.1.0, unverified Android test build

- Chat-list home, All/Unread/Groups filters and four navigation tabs: Chats, Transfers, Profile and Settings.
- Editable display name and photo, with a stable generated local @username. These are labels, not verified identities or globally reserved accounts. Pair again to refresh a paired device profile.
- Add nearby uses NSD/mDNS on the same reachable Wi-Fi with both Nearby screens open. Requests require Accept/Decline/Block; no token is returned while pending. Requests expire in 60 seconds, one pending per IP, 30-second cooldown. Persistent block uses the claimed device ID, so it is not protection against a malicious identity-changing client. QR remains available, with scanner confirmation.
- Photo/name are advertised only while Nearby is open, and exchanged with paired devices. An unpaired ping returns reachability only. Closing Nearby clears requests and stops advertisement/discovery. No automatic chat-history copy.
- Video attachments with a local reference open inside the app using Android VideoView controls, seeking and fullscreen rotation. Unsupported formats offer another app. Codec support depends on the phone.
- Received library: Videos / Photos / Files, with matching NEW save subfolders under Downloads/B+ Share. Existing flat files and references remain untouched. Browse old files opens Android's document picker; it does not move files or recover missing copies.

Validation: clean SDK34 build, versionCode 5, original signing certificate. Seventeen JVM state/endpoint assertions verify pending requests never return credentials, IP/secret binding, expiry, cooldown, blocking, close and confirmation. Message network regressions and updater fault-injection tests pass. Exact released 1.0.3 downloader recognizes and downloads candidate 1.1.0 through redirects with matching SHA-256.

Important: the redesigned Android UI has NEVER been rendered at runtime or visually verified. Only mockups were visually inspected. Software emulator attempts failed to boot in the available environment. Physical-phone discovery, request dialogs, storage permissions, video playback, installation and two-phone transfer are NOT tested. This is an unverified trial build for the first phone test, not a claim that these flows work on a device.

Both phones need 1.1.0 for nearby requests. Use QR if the router blocks discovery. Update over the old app, do not uninstall. Version 1.0.3 stays downloadable, but Android normally blocks installing an older versionCode over a newer one. Returning to 1.0.3 may require uninstalling and lose app-local chats/settings; it is not a guaranteed data-preserving rollback. Keep an existing 1.0.3 device unchanged until testing the new build if continuity matters.

No internet relay, online directory or Windows client. Local transport remains unencrypted, trusted private networks only. Ordinary file transfers have no resume/checksum and files 2 GB or larger are unsupported.

## Version 1.0.3

- Failed text messages retain a concrete HTTP/network failure reason. Tap the failed message to review the reason, inspect connectivity or retry that exact message. Retry uses a stable local message ID rather than updating the last pending message.
- Header information button tests the saved peer address, shows local/target IP and result, and offers Wi-Fi settings and token-free Copy details. A failed ping does not establish the cause.
- Retrying after an uncertain delivery may duplicate a message, since wire protocol v1 has no acknowledgement IDs/deduplication. The app warns before retry. No automatic resend was added.
- File menu adds Share via another app using the Android system chooser and temporary read access. Choose WhatsApp/Messenger or another compatible installed app, then select and confirm the recipient there. Unavailable old files still require a local copy.
- File failures retain safe reasons and offer a confirmed Send file again action.
- JVM tests of the actual message network methods cover HTTP success, HTTP 403, refused connection, ping and a bounded response stall. The exact 1.0.2 updater downloaded the candidate 1.0.3 APK through a redirect with matching SHA-256. Android UI/installer/share-sheet operation remains untested.

## Version 1.0.2

- A Chats button on the main screen opens the paired-device conversation list.
- New sent/received file messages retain local content URIs. Image cards load sampled thumbnails; document cards show file type, name, size and state. Tap to open using Android; long-press for Open, Forward, Remove from chat or Locate file.
- Forward selects a paired device or group and asks for confirmation. Remove from chat removes only the local message, never the underlying file or another device's copy.
- Old/migrated file records do not contain the original files. Locate file lets the user explicitly associate a local copy; nothing is recovered automatically.
- Emoji insertion and quick replies fill the composer for editing before Send. No message reactions, launcher shortcuts or internet relay were added.
- Received files record their content URI and MIME type. Incomplete receives are discarded. Device-local file URIs are stripped from copied chat history.
- The exact 1.0.1 downloader passed a redirected download of the candidate 1.0.2 APK with matching SHA-256 in a JVM test. Android installer, permission prompts and file viewing remain untested on-device.

## Version 1.0.1

- Wi-Fi/hotspot setup guidance before QR pairing and file offers; cellular addresses are no longer used in QR codes.
- Pairing errors show the target address, this phone's address and the failure reason, with retry, Wi-Fi settings and token-free Copy details.
- GitHub updater: visible check/download/error states, percentage progress, bounded requests, up to three download attempts and resumable partial downloads. Redirects are supported. Final size, available GitHub digest, package identity, newer version and matching signing certificate are checked before installation.
- Android's installer remains in control. Unknown-app installation permission is requested when needed, with a return path.
- Pairing now checks the QR token. History is no longer copied automatically when pairing. An explicit confirmation on the paired screen is required. History copies include text and file records, not original file bytes.

## Install and test

Download the APK from the latest GitHub release and install it over the previous version. Version 1.0 only checks for releases, so the first upgrade to 1.0.1 must be installed manually. The new downloader applies to later updates.

Connect both phones to the same trusted Wi-Fi network, or try one phone's hotspot with the other joined. Keep both apps open. First try pairing and a small file. Guest Wi-Fi may prevent phones from communicating.

This is trial software. Android runtime, installer permission screens, hotspot support and real two-phone transfers have not been verified for 1.1.0. JVM fault-injection tests cover the updater network implementation, not the complete Android app. See `tests/results.txt` and `tests-updater.py`.

## Features and limits

QR pairing, multiple paired devices, per-device chat, file offers and groups are implemented. Watched-folder sending requires a chosen folder and target and is off by default. Background behavior depends on Android's restrictions and has not been tested. Internet relay mode is not implemented.

Local transfers use unencrypted HTTP with bearer tokens. Use trusted private networks only. Do not share pairing QR codes. Ordinary file transfer has no resume/checksum support and files of 2 GB or larger are not supported safely. See `PROTOCOL-v1.md` for the current wire format and limitations.

## Build

`build.sh` uses Android SDK platform/build-tools 34, JDK 17, aapt2, ecj and d8. Set up the paths in the script for your environment. Signing requires your own keystore and a signing password provided through `BPLUS_KEYSTORE_PASSWORD`. Do not commit signing keys, credentials or generated APKs.

Developed for Basem Helmy.


## 1.1.6 device-test candidate

Empty-row-corner selection marks, immediate selected-card removal, chat bulk record/copy actions and shared app-dialog styling. See release116-notes.txt for exact scope and unverified device behavior. This is a pre-release; 1.1.5 remains normal latest. No private conversations/files are included.


## 1.1.7 device-test candidate

Fix selection touch ownership and add plain text multi-selection. 1.1.6 was not promoted after a second phone recording exposed a re-entry regression. See release117-notes.txt for actions, test coverage and remaining verification. 1.1.5 remains latest.


## 1.1.8 device-test candidate

Includes 1.1.7 text selection and gesture ownership fix. Prepares dialog content/style/width before showing and replaces movement animations with alpha-only fade. Actual phone animation remains unverified. See release118-notes.txt. 1.1.5 remains latest.


## 1.1.9 device-test candidate

Plain Delete flow, exact document/provider deletion routes with supported Android consent and named-file failure dialog, expanded categorized emoji grid and composer spacing. Actual Android provider deletion and emoji pixels still need phone validation. See release119-notes.txt. 1.1.5 stays latest.


## 1.1.10 device-test candidate

New private-chat receipt-bound Delete for everyone, local permanent-delete success removes selected cards, persistent emoji panel, and in-app test-release discovery enabled by default. Both phones need this version for new receipt-bound items; old/QR batch/group recall is unsupported. See release1110-notes.txt. GitHub normal latest stays 1.1.5. Voice notes planned separately.


## 1.1.11 voice-message device-test candidate

Private/group hold-to-record AAC notes, release to send, slide to cancel; audio cards with play/pause/seek/duration. Foreground mic permission explanation; no background recording. Actual Android audio/gesture/layout test remains pending. Includes 1.1.10 test updater/recall/emoji changes. See release1111-notes.txt. No promotion.


## 1.1.12 voice UI device-test candidate

Combined mic/send composer action, clean B+ voice cards, pre-play duration metadata, live recording amplitude/timer overlay. Actual Android layout/gesture/audio validation still pending. See release1112-notes.txt. No promotion.
