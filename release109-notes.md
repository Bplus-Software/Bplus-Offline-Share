# Android B+ Offline Share 1.0.9, code 30, staged only

- Separate Unpair and Remove device and chat actions in chat-list menu/long press, Devices menu and chat options. Already unpaired devices only offer removal. Both actions confirm first.
- Unpair keeps the token-free identity row, Not paired badge, chat and saved files, and stops pending text/automatic sends.
- Remove device and chat clears both paired/archive row and the local thread, read marker, old clear-chat snapshot and per-device folder configuration. It revokes automatic sends, changes the operation epoch and rejects stale chat writes so re-pair does not restore old local history.
- Saved files, local library and receipt/dedup/file-ownership security ledgers are retained. The other device is untouched. This does not send a remote deletion request.
- File sends recheck active pairing and removal epoch before/during upload and before saving a completion card. Bytes already delivered before cancellation cannot be recalled by this action.

Validation: SDK34 clean signed APK, same package com.bplus.share, original signer; higher code30. Actual Store JVM tests with SharedPreferences doubles cover active/archive removal, retained unpair history, clear read/snapshot/folder, no unrelated thread/file-ledger changes, repeated removal, stale writes before and after re-pair, new history after re-pair, and unpair after re-pair. Devices layout, icon layout, exact-asset updater, profile privacy and auto-send regressions pass.

Visual evidence: approved illustrative Arabic menu/confirmation renders. No emulator/device attached. Actual Android menu placement, font scale, lifecycle, transfer cancellation latency and physical installation remain unverified. Existing Devices 1.0.8 layout is retained.

STAGED ONLY. Do not publish until Basem finishes this testing batch and the parent gives the release instruction. Shared Windows+Android tag v1.0.9. Install over existing app; never uninstall or clear app data.
